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

#ifndef GLOBALSH
#define GLOBALSH
 
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <netdb.h>

#include "readln.h"

#ifndef VERSION
#define VERSION "$Id: globals.h 1540 2008-07-11 09:59:59Z frhe4063 $"
#endif
#ifndef COMPILE_INFO
#define COMPILE_INFO "Unknown"
#endif

#define MAXAPPSTRLEN 50
#define MAXDATASIZE 100
//#define PORT 5678
#define MAXCTRLMSGSIZE 500
#define MAXCEFARGLEN 50
#define BUFSIZE 200
#define MAXTOKENSIZE 100
#define MAXLOGLINELEN 150
#define LE_HEADER 25 // Space for timestamp and GUIs in pkts

#define MAXMSGSIZE 256 
#define UDPBUFSIZE 250

#define MAX_ARGC 16     // Maximal number of arguments that can be passed
                        // to a fork()'ed application.

#define NET_NO 0
#define NET_TCP 1
#define NET_UDP 2

#define DAEMON_STDOUT_DEFAULT	"log/vclient.log"
#define DAEMON_STDERR_DEFAULT	"log/vclient.log"

#define NODEID_FILE "info/nodeid"

#define POSITION_FIFO "run/position"
#define POSITION_FILE "info/position"

// This is relative the the vclient's working directory,
// which should be a node directory.
#define PID_FILE "run/vclient.pid"

/** Runtime information about this vclient instance.
 */
struct vclient_runtime {
  /** The name of the monitored sensor node.
   */
  char nodeid[MAXAPPSTRLEN]; 

  /** The position of the monitored sensor node.
   */
  struct {
    float x, y, z;
  } position;

  /** File descriptor of the FIFO that holds updated position information
   */
  int position_fifo_fd;

  /** The monitored application.
   */
  struct {
      /** The PID of the monitored application. 0 if not running.
       */
      pid_t pid;

      /** The applications stdout.
       */
      struct readln_state out;
  } app;

  /** Is this vclient instance running in daemon mode?
   */
  int as_daemon;

  int tcp_up;
};

struct vclient_config{

  char config_filename[MAXAPPSTRLEN];

  char app_name[MAXAPPSTRLEN];
  char app_argv[MAX_ARGC][MAXAPPSTRLEN];
  char app_argc;

  char log_filename[MAXAPPSTRLEN];
  char event_filename[MAXAPPSTRLEN];

  char my_addr[MAXAPPSTRLEN];
  char my_hostname[MAXAPPSTRLEN];
  int my_port;

  char monitor_addr[MAXAPPSTRLEN]; 
  int monitor_port;
  
  struct hostent he;
  //  struct in_addr monitor_saddr;
  
  int udp_up; // data to monitor
  int tcp_up; // data to monitor
  int tcp_down; // control messages from monitor
  int tcp_listen; // socket for monitor to connect to

  int ping_interval;
  int udp_timeout;
  char logparser[MAXAPPSTRLEN];

  /** The maximum time in milliseconds that a log event will be delayed.
   */
  unsigned int max_delay;
};

/** The single vclient runtime information.
 */
struct vclient_runtime vcr;

#endif

/* vim:set sw=2 ts=8 expandtab: */
