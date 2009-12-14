/* Copyright 2009 Uppsala University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *	 
 *	 http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

#include "net.h"

#if defined(OS_WINDOWS)
#elif defined(OS_UNIX)
#include <stdio.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <fcntl.h>
#endif

net_socket net_open_listening_socket(short port)
{
	struct sockaddr_in my_addr;
	net_socket sock;
	int optval;
	
	// Set up local port:
	my_addr.sin_family = AF_INET;
	my_addr.sin_port = htons(port);
	my_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	memset(my_addr.sin_zero, '\0', sizeof(my_addr.sin_zero));
	
	// Open a TCP socket:
	sock = socket(PF_INET, SOCK_STREAM, 0);
	if(sock == -1)
		goto fail_socket;
	
	// Reuse address (less problems when shutting down/restarting server):
	optval = 1;
	if(	setsockopt(
			sock, 
			SOL_SOCKET, 
			SO_REUSEADDR, 
			(char *)&optval, 
			sizeof(optval)) == -1)
		goto fail_sockopt;
	
	// Bind the socket to the address:
	if(bind(sock, (struct sockaddr *)&my_addr, sizeof(my_addr)) == -1)
		goto fail_bind;
	
	// Listen on the socket:
	if(listen(sock, 20) == -1)
		goto fail_listen;
	
	return sock;
	
fail_listen:
fail_bind:
fail_sockopt:
	net_close_socket(sock);
fail_socket:
	return INVALID_SOCKET;
}

net_socket net_open_tcp_connection(char *address, short port)
{
	int sockfd;  
	struct addrinfo hints, *servinfo, *p;
	char Port[32];
	int rv;
	
	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	
	sprintf(Port, "%u", port);
	if ((rv = getaddrinfo(address, Port, &hints, &servinfo)) != 0)
		goto fail_addrinfo;

	// loop through all the results and connect to the first we can
	for(p = servinfo; p != NULL; p = p->ai_next)
	{
		sockfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
		if(sockfd == -1)
			continue;

		if(connect(sockfd, p->ai_addr, p->ai_addrlen) == -1)
		{
			close(sockfd);
			continue;
		}
		// Set to non-blocking:
		if(fcntl(sockfd, F_SETFL, O_NONBLOCK) == -1)
		{
			close(sockfd);
			continue;
		}
		break;
	}

	if (p == NULL)
		goto fail_connect;
	
	freeaddrinfo(servinfo); // all done with this structure
	
	return sockfd;
	
fail_connect:
	freeaddrinfo(servinfo);
fail_addrinfo:
	return INVALID_SOCKET;
}

void net_close_socket(net_socket sock)
{
	if(sock != INVALID_SOCKET)
#if defined(OS_WINDOWS)
		closesocket(sock);
#elif defined(OS_UNIX)
		close(sock);
#else
#error Platform not supported!
#endif
}

net_socket net_accept_on_socket(net_socket sock)
{
	struct sockaddr	addr;
	socklen_t		addr_len;
	net_socket		client_socket;
	
	addr_len = sizeof(addr);
	do{
		client_socket = accept(sock, &addr, &addr_len);
	}while(client_socket == -512);
	printf("New client socket: %d\n", client_socket);
	return client_socket;
}

int net_wait_on_sockets(struct timeval *timeout, int mask, net_socket sock1, net_socket sock2)
{
	int retval;
	net_socket max_socket;
    fd_set readfds;
    fd_set writefds;
    fd_set exceptfds;
	
	max_socket = 0;
	if(sock1 != INVALID_SOCKET && max_socket <= sock1)
		max_socket = sock1+1;
	if(sock2 != INVALID_SOCKET && max_socket <= sock2)
		max_socket = sock2+1;

    FD_ZERO(&readfds);
    FD_ZERO(&writefds);
    FD_ZERO(&exceptfds);
	if(sock1 != INVALID_SOCKET)
	{
		if(mask & 1)
			FD_SET(sock1, &readfds);
		if(mask & 2)
			FD_SET(sock1, &writefds);
		if(mask & 4)
			FD_SET(sock1, &exceptfds);
	}
	if(sock2 != INVALID_SOCKET)
	{
		if(mask & 8)
			FD_SET(sock2, &readfds);
		if(mask & 16)
			FD_SET(sock2, &writefds);
		if(mask & 32)
			FD_SET(sock2, &exceptfds);
	}
	
    select(max_socket, &readfds, &writefds, &exceptfds, timeout);

	retval = 0;
	if(sock1 != INVALID_SOCKET)
	{
		if(FD_ISSET(sock1, &readfds))
			retval |= 1;
		if(FD_ISSET(sock1, &writefds))
			retval |= 2;
		if(FD_ISSET(sock1, &exceptfds))
			retval |= 4;
	}
	if(sock2 != INVALID_SOCKET)
	{
		if(FD_ISSET(sock2, &readfds))
			retval |= 8;
		if(FD_ISSET(sock2, &writefds))
			retval |= 16;
		if(FD_ISSET(sock2, &exceptfds))
			retval |= 32;
	}
	return retval;
}
