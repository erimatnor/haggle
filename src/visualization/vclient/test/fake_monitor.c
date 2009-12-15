#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#define MAXBUFLEN 100

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
  
  return sockfd;
}

int main(int argc, char **argv)
{
  int udpfd;
  int tcpfd = 0;
  int listenfd;
  struct sockaddr_in my_addr;    // my address information
  struct sockaddr_in their_addr; // connector's address information
  socklen_t addr_len;
  int numbytes;
  char buf[MAXBUFLEN];
  int udpport;
  int tcpport;  
  fd_set master;   
  fd_set read_fds; 
  struct sockaddr_in remoteaddr; 
  int fdmax;  
  socklen_t addrlen;
    
  if (argc != 3){
    printf("usage: %s udpport tcpport\n", argv[0]);
    exit(1);
  }
  
  udpport = atoi(argv[1]);
  tcpport = atoi(argv[2]);
  
  if ((udpfd = socket(PF_INET, SOCK_DGRAM, 0)) == -1) {
    perror("socket");
    exit(1);
  }
    
  my_addr.sin_family = AF_INET;         // host byte order
  my_addr.sin_port = htons(udpport);     // short, network byte order
  my_addr.sin_addr.s_addr = INADDR_ANY; // automatically fill with my IP
  memset(&(my_addr.sin_zero), '\0', 8); // zero the rest of the struct

  if (bind(udpfd, (struct sockaddr *)&my_addr,
	   sizeof(struct sockaddr)) == -1) {
    perror("bind");
    exit(1);
  }
    
  addr_len = sizeof(struct sockaddr);
  
  listenfd = tcp_listen("localhost", tcpport);
  
  FD_ZERO(&master);  
  FD_SET(listenfd, &master);
  FD_SET(udpfd, &master);
  
  fdmax = listenfd > udpfd ? listenfd : udpfd;
  
  while(1){
    
    read_fds = master; 
    
    if (select(fdmax+1, &read_fds, NULL, NULL, NULL) == -1) {
      perror("select");
      exit(1);
    }
    
    //printf("Select returned\n");
    
    if(FD_ISSET(listenfd, &read_fds)){
      
      addrlen = sizeof(remoteaddr);
      if ((tcpfd = accept(listenfd, (struct sockaddr *)&remoteaddr,
			    &addrlen)) == -1) { 
	perror("vclient: accept");
      } else {
	
	FD_SET(tcpfd, &master); 
	if (tcpfd > fdmax) {    
	    fdmax = tcpfd;
	}
	
	printf("vclient: new connection from %s on "
	       "socket %d\n", inet_ntoa(remoteaddr.sin_addr), tcpfd);
      }       	
    }      
  
    if(FD_ISSET(udpfd, &read_fds)){
      
      if ((numbytes=recvfrom(udpfd, buf, MAXBUFLEN-1 , 0,
			     (struct sockaddr *)&their_addr, &addr_len)) == -1) {
	perror("recvfrom");
	exit(1);
      }
      
      buf[numbytes] = '\0';
      printf("udp rec: %s\n",buf);
    }
    
    if(tcpfd && FD_ISSET(tcpfd, &read_fds)){
      
      if ((numbytes=recvfrom(tcpfd, buf, MAXBUFLEN-1 , 0,
			     (struct sockaddr *)&their_addr, &addr_len)) == -1) {
	perror("recvfrom");
	exit(1);
      }
      
      if(numbytes > 0){
	
	buf[numbytes] = '\0';
	printf("tcp rec: %s\n",buf);
      } else {
	FD_CLR(tcpfd, &master);
	  close(tcpfd);
      }
    }

    

  }
  close(udpfd);

  return 0;
} 
