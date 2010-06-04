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
#ifndef _VENDETTACLIENT_H
#define _VENDETTACLIENT_H

#include "VendettaAsynchronous.h"
#include <sys/socket.h>
#include <arpa/inet.h>

using namespace haggle;

class VendettaClient : public VendettaAsynchronous {
private:
        string our_name;
        unsigned short our_port;
	struct in_addr our_ip_addr;
	struct in_addr sitemgr_ip_addr;
	unsigned short sitemgr_udp_port, sitemgr_tcp_port;
        SOCKET tcp_socket;
        SOCKET udp_socket;
	bool inShutdown;
	void determine_name(struct sockaddr& sa);
	void determine_ip(SOCKET sock);
	SOCKET open_tcp_socket(void);
	SOCKET open_udp_socket(void);
	void addToBlacklist(const char *type, const char *mac);
    protected:
	bool sendEvent(string event, string params);
        
        void _handleEvent(void);
	void _handleSendEvent(string &event, string &params);
        void _sendPING(void);
	void _handleQuit(void);
    public:
	void setSiteManager(struct in_addr ip, unsigned short udp_port, unsigned short tcp_port);
	string getOurName(void) { return our_name; }
        VendettaClient(VendettaManager *m, struct in_addr ip, unsigned short udp_port, unsigned short tcp_port,
                       const string name = "Vendetta client module");
        ~VendettaClient();
};

#endif
