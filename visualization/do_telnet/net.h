/* Copyright 2009 Uppsala University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *     
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

#ifndef net_h
#define net_h

#include "platform.h"

#if defined(OS_WINDOWS)
//#define INVALID_SOCKET -1
typedef SOCKET net_socket;
#elif defined(OS_UNIX)
#include <sys/time.h>
#include <sys/socket.h>
#define INVALID_SOCKET -1
typedef int net_socket;
#endif

net_socket net_open_listening_socket(short port);
net_socket net_open_tcp_connection(char *address, short port);

void net_close_socket(net_socket sock);

net_socket net_accept_on_socket(net_socket sock);

#define mask_network_all	\
	(mask_network_read|mask_network_write|mask_network_except)
#define mask_network_read_except	\
	(mask_network_read|mask_network_except)
#define mask_network_read	(1|8)
#define mask_network_write	(2|16)
#define mask_network_except	(4|32)
/*
	mask is which events you wish to wait on. (see return values.)
	Returns:
		0 - timeout expired.
		1 - socket 1 is readable,
		2 - socket 1 is writeable,
		4 - socket 1 has an exceptional state,
		8 - socket 2 is readable,
		16 - socket 2 is writeable,
		32 - socket 2 has an exceptional state,
		3,5,6,7,9...63 - combinations of the above
*/
int net_wait_on_sockets(
		struct timeval *timeout, 
		int mask, 
		net_socket sock1, 
		net_socket sock2 = INVALID_SOCKET);

/*
	For reading from and writing to sockets, just pass the net_socket to 
	recv/send.
*/

#endif
