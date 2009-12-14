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
#include <sys/wait.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include <signal.h>
#include <getopt.h>
#include <fcntl.h>

#include "configparser.h"
#include "network.h"
#include "control.h"
#include "logevent.h"
#include "log.h"
#include "util.h"
#include "position.h"

int run = 1;
int reset = 0;

// In daemon mode, where should stdout and stdin go?
char *stdout_file = NULL;
char *stderr_file = NULL;

/** Initialize vclient
 *
 * Note that this function will be called once on startup and once
 * for every SIGHUP! The global variable reset will be set to 1
 * if this function is called due to a SIGHUP.
 */
int vclient_init(struct vclient_config* vcc){
  // if connections are open, close them
  // the monitor will handle the client as a restarted node
  if(reset) {
    tcp_up_disconnect();

    if(vcc->tcp_down != 0 ){
      close(vcc->tcp_down);
    }

    if(vcc->tcp_listen != 0 ){
      close(vcc->tcp_listen);
    }

    if(vcc->udp_up != 0 ){
      close(vcc->udp_up);
    }
  }

  // read configs
  // if an error occur while reading configs, crash and burn
  // we can not recover

  if(parse_config(vcc)){

    return 1;
  }

  struct hostent *host;
  if ((host = gethostbyname(vcc->monitor_addr)) == NULL){
    LOG(LOG_ERROR, "Failed to get monitor host address.\n");
    return 1;
  }
  memcpy(&vcc->he, host, sizeof(struct hostent));

  if(parse_event_config(vcc)){

    return 1;
  }

  // open connections to monitor and start listening socket
  // only return an error if we can not get an listening socket.

  if((vcc->tcp_listen = tcp_listen(vcc->my_addr,vcc->my_port)) == 0){
    LOG(LOG_ERROR, "tcp listening socket monitor host info\n");
    return 1;
  }

  if((vcc->udp_up = udp_open(vcc->monitor_addr,vcc->monitor_port + 1)) == 0){
    LOG(LOG_ERROR, "error opening udp socket\n");
  }

  // Read the node id of the node we're monitoring.
  if (read_file(NODEID_FILE, vcr.nodeid, MAXAPPSTRLEN)) {
    LOG(LOG_ERROR, "Error reading node id.\n");
    return 1;
  }

  vcr.tcp_up = -1;

  if (!reset) {
    // Read the initial position information.
    position_read_from_file(POSITION_FILE);

    // Create the position FIFO for updated positions
    position_create_fifo();
  
    // Open the position FIFO
    if (position_open_fifo() < 0) {
      return 1;
    }
 }

 reset = 0;
 
 return 0;
}


/** Clean up and release resource for this vclient instance
 *
 * This function is called once the vclient is shutdown.
 */
void vclient_cleanup(struct vclient_config* vcc, int had_error){
  if(vcr.app.pid) {
    // Notify the application we're cleaning up.
    // Wait for SIGCHLD.
    sigset_t set;
    sigfillset(&set);
    sigdelset(&set, SIGCHLD);

    kill(vcr.app.pid, SIGTERM);
    sigsuspend(&set);
  }

  tcp_up_disconnect();

  if(vcc->tcp_down != 0 ){
    close(vcc->tcp_down);
  }

  if(vcc->udp_up != 0 ){
    close(vcc->udp_up);
  }

  if (vcr.position_fifo_fd >= 0) {
    close(vcr.position_fifo_fd);
  }

  position_delete_fifo();
  if (!had_error) {
    position_write_to_file(POSITION_FILE);
  }
}

/** Reap a terminated child that was signalled using SIGCHLD
 *
 * This is called from the signal handler, so it ought to be quick.
 */
void reap_child() {
  pid_t pid;

  pid = waitpid(-1, NULL, WNOHANG);

  if (pid == vcr.app.pid) {
    // The monitored application died, so we clean up.
    LOG(LOG_DEBUG, "Application exited, cleaning up.\n");
    vcr.app.out.fd = -1;
    vcr.app.out.buf_len = 0;
    vcr.app.out.nonblocking = 0;
    vcr.app.pid = 0;
  } else {
    // Should not happen ...
    LOG(LOG_WARN, "Got SIGCHLD for unknown child process (PID: %d).\n",
                   pid);
  }

  LOG(LOG_INFO, "Reaped terminated child. (PID: %d)\n", pid);
}

/** vclient's signal handler.
 * 
 * sighup reread config
 * sig int , die gracefully
 * sigchld, reap zombie.
 * sigusr1, toggle verbosity of output
 */
void sig_handler(int signum){
  switch(signum){
  case SIGTERM:
  case SIGINT:
    LOG(LOG_INFO, "Stopping due to signal.\n");
    run = 0;
    reset = 1;
    break;
  case SIGHUP:
    LOG(LOG_INFO, "Resetting.\n");
    reset = 1;
    break;
  case SIGCHLD:
    // Reap a terminated child to prevent zombie processes.
    reap_child();
    break;
  case SIGUSR1:
    // Toggle vclient's debug
    toggle_debug();
    break;
  default:
    LOG(LOG_WARN, "Unknown signal received.\n");
    break;
  }
}


/** Tell the user which arguments we take.
 */
void usage(char **argv) {
  fprintf(stderr, "\nUsage: %s [--daemon] [--stdout FILE] [--stderr FILE] [config]\n", argv[0]);
  fprintf(stderr, "\nDefault config file is ./vclient.config\n");
  fprintf(stderr, "In daemon mode stdout and stderr are by default redirected to /var/log/vclient.log.\n");
  fprintf(stderr, "The options --stdout and --stderr have no effect if vclient is not run in daemon mode.\n");
}


/** Parse arguments from the command line.
 */
void parse_args(int argc, char **argv, struct vclient_config *vcc) {
  int opt, opt_index;
  struct option long_opts[] =
     { { "daemon", 0, NULL, 'd' },
       { "stdout", 1, NULL, 'o' },
       { "stderr", 1, NULL, 'e' },
       { 0, 0, 0, 0 }
     };

  while ((opt = getopt_long(argc, argv, "do:e:", long_opts, &opt_index)) != -1) {
    switch (opt) {
    case 'd':
      vcr.as_daemon = 1;
      break;
    case 'o':
      stdout_file = malloc(strlen(optarg) + 1);
      strcpy(stdout_file, optarg);
      break;
    case 'e':
      stderr_file = malloc(strlen(optarg) + 1);
      strcpy(stderr_file, optarg);
      break;
    default:
      usage(argv);
      exit(1);
    }
  }

  if (optind < argc) {
    strncpy((void *) vcc->config_filename, argv[optind], MAXAPPSTRLEN);
  } else {
    strncpy((void *) vcc->config_filename, "vclient.config", MAXAPPSTRLEN);
  }
}

/** Daemonizes this vclient instance.
 */
void daemonize() {
  pid_t child;

  if (stdout_file == NULL) {
    stdout_file = DAEMON_STDOUT_DEFAULT;
  }

  if (stderr_file == NULL) {
    stderr_file = DAEMON_STDERR_DEFAULT;
  }

  child = fork();
  if (child < 0) {
    LOG(LOG_ERROR, "Cannot start in daemon mode: Failed to fork (%s).\n",
                    strerror(errno));
    exit(1);
  } else if (child == 0) {
    FILE *daemon_stdout = fopen(stdout_file, "a");
    FILE *daemon_stderr = fopen(stderr_file, "a");

    // Redirect stdout & stderr
    close(fileno(stdout));
    close(fileno(stderr));
    dup2(fileno(daemon_stdout), fileno(stdout));
    dup2(fileno(daemon_stderr), fileno(stderr));
  } else {
    // This is the parent, we just exit nicely.
    exit(0);
  }
}

int main(int argc, char** argv) {
  int ret = 0;
  struct vclient_config vcc;
  fd_set master;
  fd_set read_fds;
  struct sockaddr_in remoteaddr;
  int fdmax;
  socklen_t addrlen;
  int newfd = -1;
  int had_error = 0;
  struct timeval next_to, to;

  memset(&vcc, 0, sizeof(vcc));
  parse_args(argc, argv, &vcc);

  if (vcr.as_daemon) {
    daemonize();
  }

  // register signal handler
  if (signal (SIGINT, sig_handler) == SIG_IGN)
    signal (SIGINT, SIG_IGN);
  if (signal (SIGHUP, sig_handler) == SIG_IGN)
    signal (SIGHUP, SIG_IGN);
  if (signal (SIGCHLD, sig_handler) == SIG_IGN)
    signal (SIGCHLD, SIG_IGN);
  if (signal (SIGTERM, sig_handler) == SIG_IGN)
    signal (SIGTERM, SIG_IGN);
  if (signal (SIGUSR1, sig_handler) == SIG_IGN)
    signal(SIGUSR1, sig_handler);

  LOG(LOG_INFO, "This is vclient %s (%s)\n", VERSION, COMPILE_INFO);

  while (run){
    // should this move into the while ??
    FD_ZERO(&master);

    if(vclient_init(&vcc)){
      ret = 1;
      had_error = 1;
      break;
    }

    write_pid(PID_FILE);

    FD_ZERO(&read_fds);

    if(vcc.tcp_listen){
      FD_SET(vcc.tcp_listen, &master);
    }

    fdmax = vcc.tcp_listen;

    start_pings(&vcc); // Add pings to event queue

    while (!reset){

      read_fds = master;

      if (vcr.app.pid && vcr.app.out.fd >= 0) {
        FD_SET(vcr.app.out.fd, &read_fds);
        if (vcr.app.out.fd > fdmax) {    // keep track of the maximum
          fdmax = vcr.app.out.fd;
        }
      }

      if (vcr.position_fifo_fd >= 0) {
        // Add the position information FIFO to the set of file
        // descriptors which we want to read from.
        FD_SET(vcr.position_fifo_fd, &read_fds);
        if (vcr.position_fifo_fd > fdmax) {
          fdmax = vcr.position_fifo_fd;
        }
      }


      // Ok, let's figure out which event is next.
      if (get_udp_timeout(&vcc, &to)){
        LOG(LOG_WARN, "Error with periodic ping, resetting.\n");
        reset = 1;
        break;
      }
      next_to = to;

      if (!get_control_event_timeout(&vcc, &to) &&
            is_before(&to, &next_to)) {
        // Control timeout is next.
        next_to = to;
      }

      if (!get_log_event_timeout(&to) &&
            is_before(&to, &next_to)) {
        // Log event timeout is next.
        next_to = to;
      }

      // Wait until something interesting happens or the next timeout occurs.
      ret = select(fdmax+1, &read_fds, NULL, NULL, &next_to);
      
      if (ret == -1) {
        if (errno == EINTR) {
          // We got interrupted, just do it again. This may happen when a
          // fork()'ed child terminates.
          continue;
        } else {
          // This should never happen.
          LOG(LOG_ERROR, "select() failed: %s\n", strerror(errno));
          remove_pid(PID_FILE);
          exit(1);
        }
      } else if (ret == 0) {
//        LOG(LOG_DEBUG, "select() timed out.\n");
      }

      // Send the queued log events.
      send_log_events(&vcc);
      // Handle control messages.
      fire_control_events(&vcc);

      // Control msgs from monitor ?
      if(vcc.tcp_down && FD_ISSET(vcc.tcp_down, &read_fds)){
        // used to see if app is stopped or started
        // app_state = vcc.app_started;

        if(read_control_msg(&vcc) <= 0) {
          LOG(LOG_DEBUG, "Connection closed from %s on socket %d.\n", inet_ntoa(remoteaddr.sin_addr), newfd);
          FD_CLR(vcc.tcp_down, &master);
        }
      }

      // Application output to read ?
      if (vcr.app.out.fd >= 0 &&
            (FD_ISSET(vcr.app.out.fd, &read_fds) ||
             vcr.app.out.buf_len > 0)) {
        int old_fd = vcr.app.out.fd;

        log_app_output(&vcc);

        // Check if app_out was closed while reading it. If so, remove it
        // from the FD set we select() on.
        if (vcr.app.out.fd == -1) {
          FD_CLR(old_fd, &read_fds);
        }
      } 

      // New monitor connection ?
      if(FD_ISSET(vcc.tcp_listen, &read_fds)){
        addrlen = sizeof(remoteaddr);
        if ((newfd = accept(vcc.tcp_listen, (struct sockaddr *)&remoteaddr,
                            &addrlen)) == -1) {
          LOG(LOG_WARN, "accept() failed: %s\n", strerror(errno));
        } else {
          // FIXME: Check that it is the monitor that is sending
          
          fcntl(newfd, F_SETFD, FD_CLOEXEC);

          if(vcc.tcp_down){
            close(vcc.tcp_down);
          }
        
          FD_SET(newfd, &master);
          if (newfd > fdmax) {
            fdmax = newfd;
          }
        
          vcc.tcp_down = newfd;
        
          LOG(LOG_DEBUG, "New connection from %s on socket %d.\n", inet_ntoa(remoteaddr.sin_addr), newfd);
        }
      }

      // new position from the position FIFO?
      if (vcr.position_fifo_fd >= 0 &&
            FD_ISSET(vcr.position_fifo_fd, &read_fds)) {
        position_read_fifo();
      }

    }   //  reset

    free_log_events();
  } // run


  remove_pid(PID_FILE);
  vclient_cleanup(&vcc, had_error);
  return ret;
}

/* vim:set sw=2 ts=8 expandtab: */
