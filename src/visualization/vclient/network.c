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
#include <fcntl.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <string.h>
#include <assert.h>
#include <time.h>

#include "globals.h"
#include "log.h"
#include "util.h"

#define MSG_NOSIGNAL 0

struct timeval last_udp_pkt;

// Returns 0 on error, otehrwise sockfd

int udp_open(char* host, int port){
  
  int sockfd;
  struct sockaddr_in their_addr; // connector's address information
  struct hostent *he;
  
  
  if ((he=gethostbyname(host)) == NULL) {  // get the host info
    perror("gethostbyname");
    remove_pid(PID_FILE);
    exit(1);
  }
  
  if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) == -1) {
    perror("socket");
    return 0;
  }

  fcntl(sockfd, F_SETFD, FD_CLOEXEC);
  
  their_addr.sin_family = AF_INET;     // host byte order
  their_addr.sin_port = htons(port); // short, network byte order
  their_addr.sin_addr = *((struct in_addr *)he->h_addr);
  memset(&(their_addr.sin_zero), '\0', 8);  // zero the rest of the struct
  
  if (connect(sockfd, (struct sockaddr *)&their_addr,
	      sizeof(struct sockaddr)) == -1) {
    close(sockfd);
    perror("connect");
    return 0;
  }

  if(gettimeofday(&last_udp_pkt, 0)){
    perror("open_udp: gettimeofday\n");
    return 0;
  }
   
  
  return sockfd;
}

/** Create the TCP connection to the monitor.
 *
 * \param he   The monitor's address.
 * \param port The port to connect to.
 * \todo Timeout on connect?
 * \param Socket handle on success, < 0 otherwise.
 */
int tcp_connect(struct hostent *he, int port){
  int sockfd;
  struct sockaddr_in their_addr; // connector's address information 

  if ((sockfd = socket(PF_INET, SOCK_STREAM, 0)) == -1) {
    LOG(LOG_DEBUG, "Failed to create monitor socket: %s.\n", strerror(errno));
    return -1;
  }

  fcntl(sockfd, F_SETFD, FD_CLOEXEC);

  their_addr.sin_family = AF_INET;    // host byte order 
  their_addr.sin_port = htons(port);  // short, network byte order 
  their_addr.sin_addr = *((struct in_addr *)he->h_addr);
  memset(&(their_addr.sin_zero), '\0', 8);  // zero the rest of the struct 
 
  if (connect(sockfd, (struct sockaddr *)&their_addr,
	      sizeof(struct sockaddr)) == -1) {
      
    close(sockfd);
    LOG(LOG_DEBUG, "Failed to connect to monitor: %s.\n", strerror(errno));
    return -1;
  }
  vcr.tcp_up = sockfd;

  LOG(LOG_INFO, "Connected to monitor.\n");

  return sockfd;
}

void tcp_up_disconnect() {
  if (vcr.tcp_up >= 0) {
    LOG(LOG_INFO, "Disconnected from monitor.\n");
    close(vcr.tcp_up);
    vcr.tcp_up = -1;
  }
}

int tcp_listen(char* host, int port){
  
  int sockfd;
  struct sockaddr_in my_addr;    // my address information
  struct hostent *he;
  int yes=1;

  if ((he=gethostbyname(host)) == NULL) {  // get the host info 
    herror("gethostbyname");
    return 0;
  }
 
  if ((sockfd = socket(PF_INET, SOCK_STREAM, 0)) == -1) {
    perror("socket");
    remove_pid(PID_FILE);
    exit(1);
  }
  
  if (setsockopt(sockfd,SOL_SOCKET,SO_REUSEADDR,&yes,sizeof(int)) == -1) {
    perror("setsockopt");
    return 0;
  }
    
  my_addr.sin_family = AF_INET;         // host byte order
  my_addr.sin_port = htons(port);     // short, network byte order
  my_addr.sin_addr = *((struct in_addr *)he->h_addr);
  memset(&(my_addr.sin_zero), '\0', 8); // zero the rest of the struct
  
  if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr))
      == -1) {
    perror("bind");
    return 0;
  }
  
  if (listen(sockfd, 5) == -1) {
    perror("listen");
    return 0;
  }

  fcntl(sockfd, F_SETFD, FD_CLOEXEC);
  
  return sockfd;
}

char udp_buf[UDPBUFSIZE];
int udp_data_len=0;

int flush_udp(struct vclient_config* vcc) {
  if(gettimeofday(&last_udp_pkt, 0)){
    perror("send_udp: gettimeofday\n");
    return 1;
  }
  
  if(udp_data_len > 0){
    LOG(LOG_DEBUG, "flush udp.\n");
    
    send(vcc->udp_up, udp_buf, udp_data_len , 0);
    udp_data_len=0;
  }
  return 0;
}
  
int send_udp(struct vclient_config* vcc, void* data, int len) {
  
  // add to queue
  
  if (len >= UDPBUFSIZE){
    LOG(LOG_WARN, "trying to send UDP pkt bigger that UDP buf\n");
    return 1;
  }
    
  if(udp_data_len + len + 1 >= UDPBUFSIZE){
     
    if(gettimeofday(&last_udp_pkt, 0)){
      perror("send_udp: gettimeofday\n");
      return 1;
    }
   
    send(vcc->udp_up, udp_buf, udp_data_len , 0);
    udp_data_len=0;
    
  }
  
  memcpy(udp_buf + udp_data_len, data, len);
  udp_data_len += len;
  udp_buf[udp_data_len] = ':';
  udp_data_len++;
  
  return 0;
}


/** Send data to the monitor.
 *
 * \param vcc The vclient configuration.
 * \param data The data to send.
 * \param len The length of the data in bytes.
 *
 * \return 0 on success, non-zero otherwise.
 */
int send_tcp(struct vclient_config* vcc, void* data, int len) {
  int total = 0;        // how many bytes we've sent
  int bytesleft = len; // how many we have left to send
  int n = 0;

  if (vcr.tcp_up == -1) {
    // We're not connected, try to (re-)connect.
    if (tcp_connect(&vcc->he, vcc->monitor_port) == -1) {
      LOG(LOG_ERROR, "Failed to reconnect to monitor!\n");
      return -1;
    }
  }

  assert(vcr.tcp_up != -1);
  while (total < len) {
    n = send(vcr.tcp_up, (unsigned char *) data+total, bytesleft, MSG_NOSIGNAL);
    if (n == -1) { break; }
    total += n;
    bytesleft -= n;
  }

  if (n == -1) {
    if (errno == EPIPE) {
      // We lose the log line here, but it's the monitor's fault ;)
      LOG(LOG_WARN, "Connection closed by monitor.\n");
    } else {
      LOG(LOG_WARN, "Error sending to monitor: %s.\n", strerror(errno));
    }
    tcp_up_disconnect();
  }

  return n==-1?-1:0; // return -1 on failure, 0 on success
}

int get_udp_timeout(struct vclient_config *vcc, struct timeval* diff){
  
  struct timeval abs_at;
  struct timeval now;
  int behind = 1;
  
  if(gettimeofday(&now, 0)){
    perror("get_udp_timeout: gettimeofday\n");
    return 1;
  }
  
  while(behind){
    abs_at.tv_sec = last_udp_pkt.tv_sec 
      + (vcc->udp_timeout + last_udp_pkt.tv_usec / 1000) / 1000 ;
    abs_at.tv_usec = (last_udp_pkt.tv_usec + vcc->udp_timeout * 1000) % 1000000;
    
    timersub(&abs_at, &now, diff);
        
    // did we miss a deadline ?
    if(diff->tv_sec < 0 || diff->tv_usec < 0)
      flush_udp(vcc);
    else
      behind = 0;
  }

//  LOG(LOG_DEBUG, "udp_timeout is %ld sec and %ld usec\n",abs_at.tv_sec,abs_at.tv_usec);
//  LOG(LOG_DEBUG, "now is %ld sec and %ld usec\n",now.tv_sec,now.tv_usec);
//  LOG(LOG_DEBUG, "diff is %ld sec and %ld usec\n",diff->tv_sec,diff->tv_usec);
  return 0;
}

/* vim:set sw=2 ts=8 expandtab: */
