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
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <signal.h>

#include "globals.h"
#include "control.h"
#include "network.h"
#include "util.h"
#include "log.h"


struct ce_list* control_event_list_hd = 0;

extern int run;
extern int reset;

/** Helper function to compare timevals, think it exists in a library too
 *
 * \return 1 even if the values are the same, will have an impact on the
 *         order of ASAP control messages
 */
int is_before(struct timeval* a, struct timeval* b){

  if(a->tv_sec > b->tv_sec)
    return 0;
  
  if(a->tv_sec < b->tv_sec)
    return 1;
  
  return (a->tv_usec <= b->tv_usec);
}

/** Add an event to the control message queue
 *
 * \param cef is a function pointer to the function doing the job, 
 * \param args is a pointer to whatever state the function needs to keep between calls
 * \param at is when the event should be fired, 0 for ASAP
 * \param resched is rescheduling information, the interpretation of the integer is up to the cef
          e.g. period time, probability, or poisson intensity 
 * \return 0 on success, non-zero on failure.
 */
int add_control_event(control_event_function cef, void* args, struct timeval* at, int resched){
  
  struct ce_list* cel = (struct ce_list*) malloc(sizeof(struct ce_list));
  struct ce_list* curr;
  struct ce_list* last;

  if(!cel){
    LOG(LOG_ERROR, "error getting memory for control event\n");
    return 1;
  }

  cel->cef = cef;
  
  if(at){
    cel->at = *at;
  } else {
    cel->at.tv_sec = 0;
    cel->at.tv_usec = 0;
  }
    
  cel->args = args;
  cel->resched = resched;
  
  if(control_event_list_hd == 0){
    cel->nxt = 0;
    control_event_list_hd = cel;
    return 0;    
  }

  last = 0;
  curr = control_event_list_hd;
  
  while (curr != 0){
    
    if(is_before(&cel->at, &curr->at)){
      
      cel->nxt = curr;

      if (last)
	last->nxt = cel;
      else
	control_event_list_hd = cel;
      
      return 0;
    }
    
    last = curr;
    curr = curr->nxt;
  }
  
  last->nxt = cel;
  cel->nxt = 0;

  return 0;
}

/** A CEF to send a PING message to the monitor
 * 
 * \param resched is the ping period
 * \param p points to the configuration struct where the monitor information
 *        can be found
 * \return resched on success, meaning that the function will be rescheduled
 *         with the same period, 0 if something went wrong
 */
int ping_event(int resched, void* p) {
  struct vclient_config* vcc =  (struct vclient_config*) p;
  struct timeval now;
  unsigned long long timestamp;
  char msg[MAXMSGSIZE];
  
  if (gettimeofday(&now, 0)) {
    perror("ping_event: gettimeofday\n");
    return 0;
  }

  timestamp = now.tv_sec * 1000ll;
  timestamp += now.tv_usec / 1000ll;

  if (vcr.app.pid > 0) {
    sprintf(msg, "%llu %s:%d PING %s (%f,%f,%f) %d",
            timestamp, vcc->my_addr, 
            vcc->my_port, vcr.nodeid,
            vcr.position.x, vcr.position.y, vcr.position.z,
            vcr.app.pid);
  } else {
    sprintf(msg, "%llu %s:%d PING %s (%f,%f,%f)",
            timestamp, vcc->my_addr, 
            vcc->my_port, vcr.nodeid,
            vcr.position.x, vcr.position.y, vcr.position.z);
  }

  // Send the ping on the side of the UDP queue
  // we do not want PINGs to be delayed
  send(vcc->udp_up, msg, strlen(msg) , 0);

  return resched;
}


/** A CEF to terminate the monitored application.
 *
 * A SIGTERM signal will be sent to the monitored application, if
 * it is running. Clean-up of resources used by the monitored
 * application is performed in the SIGCHLD handler.
 * 
 * \param resched is the period (if the app should go up and down
 *        periodically)
 * \param p Unused. 
 * \return resched if everything was ok, meaning that the
 *         function will be rescheduled with the same period.
 */
int kill_app_event(int resched, void* p) {
  if (!vcr.app.pid) {
    LOG(LOG_INFO, "Application not started.\n");
    return resched;
  }
  
  kill(vcr.app.pid, SIGTERM);
  LOG(LOG_DEBUG, "Sent SIGTERM to application.\n");
    
  return resched;
}

/** A CEF to fork the application to be monitored.
 *
 * This function will create a fork and then execute the application
 * to be monitored using execve(). The values in vcr.app will be
 * set appropriately.
 *
 * \param resched is the period (if the app should go up and down
 *        periodically)
 * \param p points to the configuration struct where the command
 *        to start the app along with it arguments can be found.
 * \return resched if everything was ok, meaning that the function
 *         will be rescheduled with the same period.
 */
int fork_app_event(int resched, void* p) {
  struct vclient_config* vcc =  (struct vclient_config*) p;
  pid_t child_pid;
  int pipefd[2];
  
  // already started ?
  if (vcr.app.pid) {
    LOG(LOG_INFO, "Application already running with PID: %d.\n", vcr.app.pid);
    return 0; // do not resched
  }

  if(pipe(pipefd)<0){
    perror("pipe");
    remove_pid(PID_FILE);
    exit(1);
  }

  if ((child_pid = fork()) < 0) {
    perror("fork");
    // Think about how to handle it
    remove_pid(PID_FILE);
    exit(1);	
  }
  
  if ( child_pid == 0 ){
    char *args[vcc->app_argc+2];
    int i;

    args[0] = vcc->app_name;
    for (i=0;i<vcc->app_argc;i++) {
      args[i+1] = vcc->app_argv[i];
  	}
    args[i+1] = NULL;

    reset_sighandler();
  
    close(1);      // close stdout
    
    // pipefd[1] is for writing to the pipe. We want the output
    // that used to go to the standard output (file descriptor 1)
    // to be written to the pipe. The following command does this,
    // creating a new file descripter 1 (the lowest available) 
    // that writes where pipefd[1] goes.
    
    dup (pipefd[1]); // points pipefd at file descriptor
    
    // the child isn't going to read from the pipe, so
    // pipefd[0] can be closed
    close (pipefd[0]);

    
    // chdir to make life easier for gprof ?
    chdir("app");
  
    execv( vcc->app_name, args );
    exit(1);

    // app crashed, use to report to monitor ??
    // Could we do a printf ? probably not
  }
  
  // Parent stuff
  close (pipefd[1]);
  vcr.app.out.fd = pipefd[0];
  vcr.app.pid = child_pid;

  LOG(LOG_INFO, "Application started with PID %d.\n", child_pid);

  return resched;
}


// Parse functions
// These functions parse arguments attached to the command from the monitor. 
// if a command msg needs state, this is a good place to do a malloc,
// initiate the state, and then scedule a CEF
// State or configuration info is passed to the CEF using a void ptr, it is 
// up to the CEF to interpret the memory pointed to. 

int fork_app(char *payload, struct vclient_config* vcc){ 
  int argc, n = strlen("args=");
  char value[MAXTOKENSIZE];

  if (vcr.app.pid) {
    LOG(LOG_INFO, "Application already running.\n");
    return -1;
  }

  argc = 0;

  // XXX Should we ensure that the payload actually begins with
  // CTRL_NET_UP_REQ? Can we trust the caller?
  payload += strlen("CTRL_NET_UP_REQ ");
  while (sscanf(payload, "args=%s", value) == 1
         && argc < MAX_ARGC) {
    strcpy(vcc->app_argv[argc], value);
    argc++;
    payload += n + strlen(value) + 1;
  }

  vcc->app_argc = argc;

  // parse extra args here
  // to get at and resched
  
  return add_control_event(fork_app_event,(void*) vcc, 0, 0);
}

int kill_app(char* args,struct vclient_config* vcc){ 
  
  // parse extra args here
  // to get at and resched
  
  return add_control_event(kill_app_event,(void*) vcc, 0, 0);
}

/** Parse a control command to update the position.
 *
 * \param args The original control command from the monitor
 * \returns 0
 */
int position_update(char *args) {
  float x, y, z;

  if (3 != sscanf(args, "CTRL_POSITION_UPDATE (%f, %f, %f)\n",
      			&x, &y, &z)) {
    LOG(LOG_WARN, "Got malformed position update request from monitor.\n");
    return 0;
  }

  vcr.position.x = x;
  vcr.position.y = y;
  vcr.position.z = z;

  LOG(LOG_DEBUG, "Position updated to %f, %f, %f.\n",
  	vcr.position.x, vcr.position.y, vcr.position.z);

  return 0;
}

/** Parses the args from the monitor and puts an cef on the queue
 * 
 * this functions has to be called for vcclient to report in to the monitor
 * it schedules periodic pings with the period vcc->ping_interval
 */
int start_pings(struct vclient_config* vcc){
  return add_control_event(ping_event,(void*) vcc, 0, vcc->ping_interval);
}

/** Reads a msg from the monitor and sends it to the right parsing
 * 
 * \param vcc is a config pointer
 * \returns number of bytes read on success, <0 on failure
 */
int read_control_msg(struct vclient_config* vcc){
  char buf[MAXCTRLMSGSIZE];
  // char token[MAXTOKENSIZE];
  // char value[MAXTOKENSIZE];
  char trimmed[MAXTOKENSIZE];
  char *payload;
  int numbytes;

  int node_id, dest_port;	// node id for future use.
  char dest_addr[MAXTOKENSIZE];

  if ((numbytes=recv(vcc->tcp_down, buf, MAXCTRLMSGSIZE-1, 0)) == -1) {
    perror("recv");
    return -1;
  }

  if (numbytes == 0) {
      // There's nothing to see here, move along!
      return 0;
  }

  buf[numbytes] = '\0';
  LOG(LOG_DEBUG, "Received: %s (%d bytes).\n", buf, numbytes);
  
  if(sscanf(buf, "%d %s %d %s", &node_id, dest_addr, &dest_port, trimmed) == 4){
    int i = 0;

    // <fugly>
    // This strips the header from the packet such that
    // the pointer payload later on only contains the actual
    // control message with its arguments.
    // Implemented by skipping until the third space.
    payload = buf;
    do {
      for (;*payload!=' ' && *payload!='\0';payload++)
	;

      if (payload == '\0') {
        LOG(LOG_WARN, "Premature end of control message!\n");
	return -1;
      }

      i++;
      payload++;
    } while (i < 3);
    // </fugly>

    
    if(strcmp(trimmed, "CTRL_NET_UP_REQ")==0){
      fork_app(payload, vcc);
    }
 
    if(strcmp(trimmed, "CTRL_NET_DOWN_REQ")==0){
      kill_app(payload, vcc);
    }

    if (strcmp(trimmed, "CTRL_POSITION_UPDATE") == 0) {
      position_update(payload);
    }

    if(strcmp(trimmed, "CTRL_SHUTDOWN")==0){
      reset = 1;
      run = 0;
    }
  } else {
    LOG(LOG_INFO, "Failed to parse incoming control message.\n");
  }
  
  return numbytes;
}

/** Pulls the next CEF from the queue.
 */
struct timeval* next_control_event(){
  if(control_event_list_hd == 0)
    return 0;
  
  return &control_event_list_hd->at;
  
}

/** Get the next control event timeout.
 *
 * \returns 1 if there is no control timeout,
 *          if 0 is returned struct timeval* to is set to the time until
 *          the next control event.  
 */
int get_control_event_timeout(struct vclient_config* vcc,struct timeval* to){
  struct timeval* nxt_ev = next_control_event();
  struct timeval now;
 
  if(!nxt_ev) {
    return 1;
  }
   
  if(gettimeofday(&now, 0)){
    perror("get_timeout: gettimeofday\n");
    return 1;
  }
  
  timersub(nxt_ev, &now , to);
  
  if(to->tv_sec < 0 || to->tv_usec < 0 ||
      (to->tv_sec == 0 && to->tv_usec == 0)) {
    // We missed this one, so timeout immediately.
    to->tv_sec = 0;
    to->tv_usec = 0;
  } 
  
  return 0;
}


/** Calls and reschedules all CEFs where the timeout has passed .
 *
 * \return 0 on success
 */
int fire_control_events(struct vclient_config* vcc){

  struct timeval now;
  struct timeval next_call;
  int ret;
  int i = 0;
  
  if(gettimeofday(&now, 0)){
    perror("fire_control_events: gettimeofday\n");
    return 1;
  }
    
  if(control_event_list_hd == 0)
    return 0;
    
  struct ce_list* curr = control_event_list_hd ;
  
  curr = control_event_list_hd ;

  while (curr != 0 && is_before(&curr->at, &now)) {
    i++;
    
    control_event_list_hd = curr->nxt;
    
    ret = curr->cef(curr->resched, curr->args);
    
    if(ret > 0){ // rescedule the CEF
      
      next_call.tv_sec = now.tv_sec +  (ret + ((long)now.tv_usec / 1000)) / 1000;
      next_call.tv_usec = ((long)now.tv_usec + ret * 1000) % 1000000;
 
      // printf("now is %ld sec and %ld usec\n",now.tv_sec,now.tv_usec );
//      printf("nextcall is %ld sec and %ld usec , later %d, bigger %d\n",next_call.tv_sec,next_call.tv_usec , is_before(&now, &next_call),now.tv_sec < next_call.tv_sec );
      add_control_event(curr->cef,curr->args, &next_call, ret);
    }    

    free(curr);
    curr = control_event_list_hd;
  }

  return 0;
  
}

/* vim:set sw=2 ts=8 expandtab: */
