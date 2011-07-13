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

#ifndef LOGEVENTH
#define LOGEVENTH

#include <regex.h>
#include <sys/time.h>

/** A rule for creating log events.
 */
struct log_event_rule {
  char type[50];
  int (*method)(char *, char *);
  regex_t regexp;
  char regexp_s[50];

  char net;
};

/** A list of log event rules.
 */
struct rule_list{
  struct log_event_rule rule;
  struct rule_list* nxt;  
};

/** A log event list element.
 */
struct log_event {
  /** Pointer to the next element in the log event list.
   * NULL if this is the last element.
   */
  struct log_event *nxt;

  /** The time the log event should be sent at.
   */
  struct timeval at;
  /** The message itself. Must be free'd by the consuming function.
   */
  char *msg;
  /** Type of the message: NET_TCP || NET_UDP.
   */
  char net;
};

int log_app_output(struct vclient_config*);
    
int add_log_event_rule(struct log_event_rule*);
int clear_log_events_rules(void);
void dump_event_rules(void);

int print_log_event_rule(struct log_event_rule *ev);

int get_log_event_timeout(struct timeval *);
void send_log_events(struct vclient_config *);
void free_log_events();


#endif

/* vim:set sw=2 ts=8 expandtab: */
