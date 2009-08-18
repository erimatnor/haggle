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

/*
	Forward declarations of all data types declared in this file. This is to
	avoid circular dependencies. If/when a data type is added to this file,
	remember to add it here.
*/
class VendettaClient;

#include "VendettaAsynchronous.h"

using namespace haggle;

class VendettaClient : public VendettaAsynchronous {
private:
        string our_name;
        string our_ip_address;
        string our_port;
#define SITE_MANAGER_ADDRESS "192.168.1.50"
#define SITE_MANAGER_TCP_PORT "4444"
#define SITE_MANAGER_UDP_PORT "4445"
        SOCKET tcp_socket;
        SOCKET udp_socket;
	void determine_name(void);
	void determine_ip(void);
	SOCKET open_tcp_socket(void);
	SOCKET open_udp_socket(void);
	void addToBlacklist(const char *type, const char *mac);
    protected:
	bool sendEvent(string event, string params);
        
        virtual void _handleEvent(void);
	virtual void _handleSendEvent(string &event, string &params);
        virtual void _sendPING(void);
    public:
	string getOurName(void) { return our_name; }
        VendettaClient(
                        VendettaManager *m = NULL,
                        const string name = "Vendetta client module");
        ~VendettaClient();
};

#endif
