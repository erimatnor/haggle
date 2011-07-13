/* Copyright (c) 2008 Uppsala Universitet.
 * All rights reserved.
 * 
 * This file is part of Vendetta.
 *
 * Vendetta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Vendetta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vendetta.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/time.h>
#include <errno.h>
#include <unistd.h>
#include <assert.h>

#include "globals.h"
#include "log.h"
#include "logevent.h"
#include "logparser.h"
#include "network.h"
#include "readln.h"


/* Ripped from gettimeofday(1)
 */
#ifndef timercmp
#define timercmp(tvp, uvp, cmp)\
                ((tvp)->tv_sec cmp (uvp)->tv_sec ||\
                (tvp)->tv_sec == (uvp)->tv_sec &&\
                (tvp)->tv_usec cmp (uvp)->tv_usec)
#endif /* timercmp */

/** The first element of the log event rule list.
 */
struct rule_list* first = 0;

/** The last element of the log event rule list.
 */
struct rule_list* last = 0;

/** The head of the log event list.
 */
struct log_event *le_list_head = NULL;

/** The tail of the log event list.
 */
struct log_event *le_list_tail = NULL;

/** Add a new rule to the log event rule list.
 *
 * \param rule The new rule.
 * \return 0 on success, non-zero on error.
 */
int add_log_event_rule(struct log_event_rule *rule){
  struct rule_list *lel = (struct rule_list*) malloc(sizeof(struct rule_list));
  struct rule_list *curr;
  
  if(!lel){
    perror("add_logevent: Error getting memory ");
    return 1;
  }

  lel->nxt = 0;
  lel->rule = *rule;
  
  if(first == 0){
    first = lel;
    last = lel;

    return 0;
  }
  
  curr = first;
  
  while (curr->nxt != 0){
    curr = curr->nxt;
  }
  
  curr->nxt = lel;
  last = lel;

  return 0;
}

/**
 * Clear the log event rule list.
 * 
 * \todo Implement.
 */
int clear_log_event_rules(void){
  // FIXME
  return 0;
}

/** Print all loaded log event rules.
 */
void dump_event_rules(void){
  struct rule_list *curr = first;
  
  while (curr != 0){
    print_log_event_rule(&curr->rule);
    curr = curr->nxt;
  }
}

/** Print a log event rule in a human-readable form.
 *
 * \param rule The rule to print.
 * \return 0.
 */
int print_log_event_rule(struct log_event_rule *rule){
  LOG(LOG_DEBUG, "log_event: %s, p %p, net %d\n", rule->type, rule->method, rule->net);

  return 0;
}

/** Removes the first log event from the list and removes it.
 *
 * \return The first log event on the list.
 */
struct log_event *log_event_pop() {
  struct log_event *ret;

  ret = le_list_head;
  le_list_head = le_list_head->nxt;

  return ret;
}

/** Returns the first log event from the list.
 *
 * \return The first log event on the list.
 */
struct log_event *log_event_peek() {
  return le_list_head;
}

/** Create a log event and add it to the log event queue with a backoff time.
 *
 * \param vcc  This vclient's configuration.
 * \param msg  The message that constitutes the log event.
 * \param net  The network type of the log event (NET_TCP or NET_UDP)
 */
void log_event_add(struct vclient_config *vcc, char *msg, char net) {
  struct log_event *le;
  unsigned long long backoff;

  le = malloc(sizeof(struct log_event));
  if (le == NULL) {
    LOG(LOG_ERROR, "Failed to allocate memory for log event.\n");
    return;
  }

  le->nxt = NULL;
  le->msg = msg;
  le->net = net;

  if (vcc->max_delay == 0) {
    backoff = 0;
  } else {
    /* Backoff time in milliseconds. */
    backoff = ((unsigned long long) random()) % vcc->max_delay;
  }

  /* This random backoff time will be added to the time of
   * the last log event in the queue, or to the current time
   * if the log event queue is currently empty. */
  if (le_list_tail != NULL) {
    le->at = le_list_tail->at;
  } else {
    gettimeofday(&le->at, NULL);
  }

  /* Add backoff time. */
  le->at.tv_sec += backoff / 1000ll;
  le->at.tv_usec += (backoff % 1000ll) * 1000ll;
  if (le->at.tv_usec >= 1000ll * 1000ll) {
    /* usec overflow? */
    le->at.tv_usec -= 1000ll * 1000ll;
    le->at.tv_sec += 1;
  }

  if (le_list_head == NULL) {
    le_list_head = le;
    le_list_tail = le;
  } else {
    assert(le_list_tail != NULL);
    le_list_tail->nxt = le;
    le_list_tail = le;
  }
}

/** Clears the log event list and free()s all elements.
 */
void free_log_events() {
  struct log_event *le;

  le = le_list_head;
  while (le != NULL) {
    struct log_event *nxt = le->nxt;

    free(le->msg);
    free(le);

    le = nxt;
  }
}

/** Sends all log events that have timed out.
 *
 * \param vcc
 */
void send_log_events(struct vclient_config *vcc) {
  struct log_event *le;
  struct timeval now;

  gettimeofday(&now, NULL);

  le = log_event_peek();
  while (le != NULL &&
           timercmp(&le->at, &now, <=)) {
    struct log_event *nxt;

    switch (le->net) {
    case NET_TCP:
      send_tcp(vcc, le->msg, strlen(le->msg));
      break;
    case NET_UDP:
      send_udp(vcc, le->msg, strlen(le->msg));
      break;
    default:
      LOG(LOG_WARN, "Unknown log event type: %d!\n", le->net);
      break;
    }

    log_event_pop();
    nxt = le->nxt;
    free(le->msg);
    free(le);

    le = nxt;
  }
}

/** Get the timeout of the next log event, if any.
 *
 * \param to Will be set to the next timeout, relative to 0.
 * \return 0 on success, 1 if the log event list s empty.
 */
int get_log_event_timeout(struct timeval *to) {
  struct log_event *le;
  struct timeval now;

  le = log_event_peek();
  if (le != NULL) {
    gettimeofday(&now, NULL);
    if (timercmp(&le->at, &now, <=)) {
      to->tv_sec = 0;
      to->tv_usec = 0;
    } else {
      timersub(&le->at, &now, to);
    }
    return 0;
  } else {
    return 1;
  }
}

/** Format a log event string as the monitor wants it.
 *
 * \param vcc
 * \param logline
 * \param pkt
 * \param maxlen
 *
 * \return 0 on success, 1 if the log line exceeds the packet buffer or
 *         if another error occured.
 */
int format_log_event_msg(struct vclient_config* vcc, struct log_event_rule *rule,
                        char *logline, char *pkt, int maxlen) {
  struct timeval now;
  unsigned long long timestamp;
  
  if (gettimeofday (&now, 0)){
    perror ("fire_control_events: gettimeofday\n");
    return 1;
  }

  timestamp = now.tv_sec * 1000ll;
  timestamp += now.tv_usec / 1000ll;
  
  if (snprintf(pkt, maxlen, "%llu %s:%d %s %s", timestamp, vcc->my_addr,
                            vcc->my_port, rule->type, logline) < 0) {
    return 1;
  }
  
  return 0;
}

/** Read one line from the monitored app and create a log event.
 *
 * This function will try to get a complete line from the forked
 * applications stdout using the readln() function. It will then
 * try to match the read line against the defined log events and
 * upon success create a new logevent.
 * Note that there still might be more input to process, since the
 * monitored app might have written more than one line at once.
 * In that case, vcr.app.out.buf_len will be > 0.
 */
int log_app_output(struct vclient_config* vcc){
  char buf[MAXLOGLINELEN];
  char logline[MAXLOGLINELEN];
  regmatch_t rm;

  // Try to read the line ...
  if (readln(&vcr.app.out, buf, MAXLOGLINELEN) <= 0) {
    // ... return, if we didn't get anything.
    return 0;
  }
  
  LOG(LOG_DEBUG, "Got line from app: %s", buf);

  // Try to match the read line against a log event rule.
  struct rule_list *curr = first;
  while (curr != NULL) {
    if (curr->rule.method == NULL) {
      // This should not be NULL, but the user could have screwed up.
      LOG(LOG_WARN, "Log event rule has empty method, ignoring!\n");
      curr = curr->nxt;
      continue;
    }
    
    if (!regexec(&curr->rule.regexp, buf,1, &rm, 0) &&
          !curr->rule.method(buf, logline)) {

      // This rule matched, so lets queue it up in the log event list.
      char *le_msg = malloc(sizeof(char) * (MAXLOGLINELEN + LE_HEADER));

      if (le_msg == NULL) {
        LOG(LOG_ERROR, "Failed to allocate memory for "
                       "log event mesage.\n");
        return 1;
      }

      format_log_event_msg(vcc, &curr->rule, logline, le_msg, MAXLOGLINELEN + LE_HEADER);
      log_event_add(vcc, le_msg, curr->rule.net);
    }
    
    curr = curr->nxt;
  }

  return 0;  
}

/* vim:set sw=2 ts=8 expandtab: */
