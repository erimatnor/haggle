/*
 * Copyright 2009 Uppsala University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

#include "VendettaClient.h"

/*
  Unfortunately, the windows (mobile) versions of the printf functions don't
  handle 64-bit integers using %lld, as they do on normal platforms, so we
  need to use this macro below:
*/
#if defined(OS_WINDOWS)
#define INT64_FORMAT "%I64d"
#else
#define INT64_FORMAT "%lld"
#endif

#define NUMBER_OF_MAC_TO_NAME_MAPPINGS	9
static struct {
	const string mac;
	const string name;
}  const node_name[NUMBER_OF_MAC_TO_NAME_MAPPINGS] =

{
	{
		"00:1C:B3:B2:C0:67", "Macken"
	},
	{
		"00:18:41:97:CF:8C", "Adam"
	},
	{
		"00:18:41:AD:B5:14", "Bertil"
	},
	{
		"00:18:41:8A:30:D3", "Ceasar"
	},
	{
		"00:13:72:E7:CF:05", "Tulip"
	},
	{
		"00:02:78:9E:BC:F8", "LG-8"
	},
	{
		"00:02:78:9E:D1:42", "LG-4"
	},
	{
		"00:02:78:24:16:12", "LG-3"
	},
	{
		"00:02:78:25:1E:50", "LG-5"
	}
};

enum {
	macken = 0,
	adam = 1,
	bertil = 2,
	ceasar = 3,
	tulip = 4,
	lg8 = 5,
	lg4 = 6,
	lg3 = 7,
	lg5 = 8
};

static string testbed_mac = "00:34:34:34:34:";
static string testbed_prefix = "node-";

static const string do_name_lookup(string mac, bool & success)
{
	long i;

	if (strncmp(mac.c_str(), testbed_mac.c_str(), testbed_mac.length()) == 0) {
		char str[10];

		i = strtol(&(mac.c_str()[testbed_mac.length()]), NULL, 10);
		sprintf(str, "%ld", i);
		return testbed_prefix + str;
	}
	for (i = 0; i < NUMBER_OF_MAC_TO_NAME_MAPPINGS; i++)
		if (node_name[i].mac == mac) {
			success = true;
			return node_name[i].name;
		}
	success = false;
	return "node-" + mac;
}

void VendettaClient::determine_name(void)
{
	InterfaceRefList ifr;
	bool has_set = false;

	our_name = "node-unknown";

	getLocalInterfaceList(ifr, false);

        for (InterfaceRefList::const_iterator it = ifr.begin(); it != ifr.end(); it++) {
		const Address *addr;
                
		addr = (*it)->getAddressByType(AddressType_EthMAC);

		if (addr) {
			bool success = false;
			string new_name;

			new_name = do_name_lookup(addr->getAddrStr(), success);

			if (!has_set || success)
				our_name = new_name;
		}
	}
}

void VendettaClient::determine_ip(void)
{
	InterfaceRefList ifr;
	int current_address_level = 0;

	our_port = "5001";
	our_ip_address = "127.0.0.1";

	getLocalInterfaceList(ifr, false);
        for (InterfaceRefList::const_iterator it = ifr.begin(); it != ifr.end(); it++) {
		const Address *addr;
                
		addr = (*it)->getAddressByType(AddressType_IPv4);
		if (addr) {
			if (strcmp(addr->getAddrStr(), "127.0.0.1") == 0) {
				//Ignore 127.0 .0 .1 address - it 's meaningless!
			} else if (strncmp(addr->getAddrStr(), "169.", 4) == 0) {
				if (current_address_level < 1) {
					our_ip_address = addr->getAddrStr();
					current_address_level = 1;
				}
			} else if (strncmp(addr->getAddrStr(), "130.238.", 8) == 0) {
				if (current_address_level < 3) {
					our_ip_address = addr->getAddrStr();
					current_address_level = 3;
				}
			} else if (strncmp(addr->getAddrStr(), "192.168.", 8) == 0) {
				if (current_address_level < 2) {
					our_ip_address = addr->getAddrStr();
					current_address_level = 2;
				}
			} else {
				if (current_address_level < 1) {
					our_ip_address = addr->getAddrStr();
					current_address_level = 1;
				}
			}
		}
	}
	HAGGLE_DBG("Vendetta info: ip: %s port: %s name: %s\n",
		   our_ip_address.c_str(),
		   our_port.c_str(),
		   our_name.c_str());
}

SOCKET VendettaClient::open_tcp_socket(void)
{
	SOCKET sock;
	struct sockaddr_in saddr;
	socklen_t addrlen = sizeof(struct sockaddr_in);
	
	saddr.sin_family = AF_INET;
	saddr.sin_addr.s_addr = sitemgr_addr.s_addr;
	saddr.sin_port = htons(sitemgr_tcp_port);
	
	//Open socket:
	sock = socket(AF_INET, SOCK_STREAM, 0);
	
	if (sock == INVALID_SOCKET) {
		HAGGLE_ERR("could not open socket : %s\n", STRERROR(ERRNO));
		return INVALID_SOCKET;
	}

	//Connect:
	if (connect(sock, (struct sockaddr *)&saddr, addrlen) == SOCKET_ERROR) {
		HAGGLE_ERR("could not connect to %s:%u : %s\n", ip_to_str(sitemgr_addr), sitemgr_tcp_port, STRERROR(ERRNO));
		CLOSE_SOCKET(sock);
		return INVALID_SOCKET;
	}

	determine_ip();
	
	return sock;
}

SOCKET VendettaClient::open_udp_socket(void)
{
	SOCKET sock;
	struct sockaddr_in saddr;
	socklen_t addrlen = sizeof(struct sockaddr_in);
	
	saddr.sin_family = AF_INET;
	saddr.sin_addr.s_addr = sitemgr_addr.s_addr;
	saddr.sin_port = htons(sitemgr_udp_port);
	
	//Open socket:
	sock = socket(AF_INET, SOCK_DGRAM, 0);
	
	if (sock == INVALID_SOCKET) {
		HAGGLE_ERR("could not open socket : %s\n", STRERROR(ERRNO));
		return INVALID_SOCKET;
	}

	//Connect:
	if (connect(sock, (struct sockaddr *)&saddr, addrlen) == SOCKET_ERROR) {
		HAGGLE_ERR("could not connect to %s:%u : %s\n", ip_to_str(sitemgr_addr), sitemgr_udp_port, STRERROR(ERRNO));
		CLOSE_SOCKET(sock);
		return INVALID_SOCKET;
	}

	return sock;
}

void VendettaClient::addToBlacklist(const char *type, const char *mac)
{
	string str;

	str = "<Haggle persistent=\"no\">"
		"<Attr name=\"Connectivity\">Blacklist</Attr>"
		"<Connectivity>"
		"<Blacklist type=\"";
	str += type;
	str += "\">";
	str += mac;
	str +=
		"</Blacklist>"
		"</Connectivity>"
		"</Haggle>";

	getKernel()->addEvent(new Event(EVENT_TYPE_DATAOBJECT_VERIFIED, 
                                        DataObjectRef(DataObject::create((unsigned char *)str.c_str(), str.length()))));
}

VendettaClient::VendettaClient(VendettaManager *m, struct in_addr ip, unsigned short udp_port, unsigned short tcp_port, const string name):
	VendettaAsynchronous(m, name), sitemgr_udp_port(udp_port), sitemgr_tcp_port(tcp_port)
{
	tcp_socket = INVALID_SOCKET;
	udp_socket = INVALID_SOCKET;

	memcpy(&sitemgr_udp_port, &ip, sizeof(struct in_addr));
	
	handleEvent();
	determine_name();
        // Post this into the send queue:
        string evt = "LE_MACHINE_TYPE";
        string param = get_hardware_name();
        
        handleSendEvent(evt, param);
        
	if (our_name == node_name[adam].name) {
		addToBlacklist("ethernet", "00:18:41:8A:30:D3");
	}
	if (our_name == node_name[ceasar].name) {
		addToBlacklist("ethernet", "00:18:41:97:CF:8C");
	}
}

VendettaClient::~VendettaClient()
{
	CLOSE_SOCKET(udp_socket);
	CLOSE_SOCKET(tcp_socket);
}

void VendettaClient::setSiteManager(struct in_addr ip, unsigned short udp_port, unsigned short tcp_port)
{
	memcpy(&sitemgr_addr, &ip, sizeof(ip));
	
	sitemgr_tcp_port = tcp_port;
	sitemgr_udp_port = udp_port;
}

bool VendettaClient::sendEvent(string event, string params)
{
	struct timeval  tv;
	fd_set exceptfds;
	//512 is the buffer size in the site manager...
        char str[512];
	unsigned long long timestamp;

	//Is the socket dead already ?
        if (tcp_socket == INVALID_SOCKET) {
		//Yep : try to reconnect to the site manager.
                tcp_socket = open_tcp_socket();
        } else {
                //Not that we know...test it for exceptional
                tv.tv_sec = 0;
		tv.tv_usec = 0;
		FD_ZERO(&exceptfds);
		FD_SET(tcp_socket, &exceptfds);
                
		select(tcp_socket + 1, NULL, NULL, &exceptfds, &tv);
                
		if (FD_ISSET(tcp_socket, &exceptfds)) {
                        //Oops - dead socket.Close it and reconnect:
			CLOSE_SOCKET(tcp_socket);
			tcp_socket = open_tcp_socket();
		}
	}
        //Check that we have a good socket:
        if (tcp_socket == INVALID_SOCKET) {
		return false;
	}
        //Create the event string:
        timestamp = Timeval::now().getTimeAsMilliSeconds();
        sprintf(str, INT64_FORMAT " %s:%s %s %s %s\n",
		timestamp,
		our_ip_address.c_str(),
		our_port.c_str(),
		event.c_str(),
		our_name.c_str(),
		params.c_str());
	send(tcp_socket, str, strlen(str), 0);

	return true;
}

void VendettaClient::_handleSendEvent(string & event, string & params)
{
	static unsigned int num_fails = 0;

	if (!sendEvent(event, params)) {
		if (num_fails > 5) {
			HAGGLE_DBG("Max number of failed send events reached! not rescheduling this one...\n");
			return;
		} else {
			num_fails++;
			handleSendEvent(event, params);
		}
	} else {
		num_fails = 0;
	}
}

void VendettaClient::_handleEvent(void)
{
	sendEvent("LE_EVENT", "Hello world!");
}

void VendettaClient::_sendPING(void)
{
	char str[256];
	unsigned long long timestamp;

	//Is the socket connected ?
        if (udp_socket == INVALID_SOCKET) {
		//Nope : try to connect to the site manager.
                udp_socket = open_udp_socket();
                
		if (udp_socket == INVALID_SOCKET)
			return;
	}

        timestamp = Timeval::now().getTimeAsMilliSeconds();
        sprintf(str,
                INT64_FORMAT " %s:%s PING %s (0.0,0.0,0.0) 1337\n",
                timestamp,
                our_ip_address.c_str(),
                our_port.c_str(),
                our_name.c_str());
        send(udp_socket, str, strlen(str), 0);
}
