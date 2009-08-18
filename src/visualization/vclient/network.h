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

#ifndef NETWORKH
#define NETWORKH

int udp_open(char* host, int port);
int tcp_up_connect(char* host, int port);
void tcp_up_disconnect();
int tcp_listen(char* host, int port);

int send_tcp(struct vclient_config* vcc, void* data, int len);
int send_udp(struct vclient_config* vcc, void* data, int len);

int get_udp_timeout(struct vclient_config *vcc, struct timeval *diff);

#endif

/* vim:set sw=2 ts=8 expandtab: */
