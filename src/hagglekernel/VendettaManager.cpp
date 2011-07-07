/* Copyright 2008-2009 Uppsala University
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

#include "VendettaManager.h"
#include "VendettaClient.h"

#define SITE_MANAGER_ADDRESS "192.168.2.50"
#define SITE_MANAGER_TCP_PORT 4444
#define SITE_MANAGER_UDP_PORT 4445

VendettaManager::VendettaManager(HaggleKernel *_kernel) : 
	Manager("VendettaManager", _kernel)
{	
}

bool VendettaManager::init_derived()
{
#define __CLASS__ VendettaManager
	setEventHandler(EVENT_TYPE_DATAOBJECT_NEW, onDataObjectEvent);
	setEventHandler(EVENT_TYPE_DATAOBJECT_DELETED, onDataObjectEvent);
	setEventHandler(EVENT_TYPE_NODE_DESCRIPTION_SEND, onDataObjectEvent);
	setEventHandler(EVENT_TYPE_NODE_CONTACT_NEW, onNodeEvent);
	setEventHandler(EVENT_TYPE_NODE_CONTACT_END, onNodeEvent);
	setEventHandler(EVENT_TYPE_TARGET_NODES, onNodeNodeListEvent);
	setEventHandler(EVENT_TYPE_DELEGATE_NODES, onNodeNodeListDataObjectEvent);
	setEventHandler(EVENT_TYPE_DATAOBJECT_SEND_SUCCESSFUL, onNodeDataObjectEvent);
	setEventHandler(EVENT_TYPE_DATAOBJECT_SEND_FAILURE, onNodeDataObjectEvent);
	setEventHandler(EVENT_TYPE_LOCAL_INTERFACE_UP, onLocalInterfaceUp);
	
	struct in_addr sitemgr_addr;
	
	sitemgr_addr.s_addr = inet_addr(SITE_MANAGER_ADDRESS);
	
        client = new VendettaClient(this, sitemgr_addr, SITE_MANAGER_UDP_PORT, SITE_MANAGER_TCP_PORT);
	
        if (!client)
                return false;
	
        return true;
}

VendettaManager::~VendettaManager()
{
	if (client)
		delete client;
	
	if (onEventQueueRunningCallback)
		delete onEventQueueRunningCallback;
}


#define EVENT_STRING_LEN 512

static void sendEvent(VendettaAsynchronous *client,
		      const char *event, 
		      const char *param1 = NULL, 
		      const char *param2 = NULL, 
		      const char *param3 = NULL)
{
	char str[EVENT_STRING_LEN];
	
	if (!client)
		return;
	
	// Create the event string:
	if (param3 != NULL) {
		snprintf(str,
			 EVENT_STRING_LEN,
			"%s %s %s", 
			param1,
			param2,
			param3);
	} else if(param2 != NULL) {
		snprintf(str,
			EVENT_STRING_LEN,
			"%s %s", 
			param1,
			param2);
	} else if(param1 != NULL) {
		snprintf(str,
			EVENT_STRING_LEN,
			"%s", 
			param1);
	} else {
		str[0] = '\0';
	}
	string params = str;
	string _event = event;
	
	client->handleSendEvent(_event, params);
}

string VendettaManager::getDOIDStr(DataObjectRef dObj)
{
	string dObjIdStr = "null";
	if(dObj) {
		dObjIdStr = dObj->getIdStr();
		if (dObj->isNodeDescription()) {
			NodeRef dNode = Node::create(Node::TYPE_PEER, dObj);
			
                        if (dNode) {
                                if (getKernel()->getThisNode()->getIdStr() == dNode->getIdStr())
                                        dObjIdStr = "A";
                        }
		}
	}
	
	HAGGLE_DBG("Data obj id is %s (from data object)\n", dObjIdStr.c_str());
	return dObjIdStr;
}

static string getNodeIDStr(DataObjectRef dObj)
{
	string dObjIdStr = "";
	if (dObj) {
		if (dObj->isNodeDescription()) {
			NodeRef dNode = Node::create(Node::TYPE_PEER, dObj);
			
                        if (dNode) {
                                dObjIdStr = dNode->getIdStr();
                                if (dObjIdStr == "[Not yet set]")
                                        dObjIdStr = "[Notyetset]";
                        }
		}
	}
	
	HAGGLE_DBG("Node id is %s (from data object)\n", dObjIdStr.c_str());
	return dObjIdStr;
}

static string getNodeIDStr(NodeRef node)
{
	string NodeIdStr = "[Unknown]";
	
	if (node) {
		NodeIdStr = node->getIdStr();
		if (NodeIdStr.length() == 0)
			NodeIdStr = "[Notyetset]";
	}
	HAGGLE_DBG("Node id is %s (from node)\n", NodeIdStr.c_str());
	return NodeIdStr;
}

void VendettaManager::onDataObjectEvent(Event *e)
{
	const Attribute *attr;
	
	if (client)
		client->handleEvent();
	
	DataObjectRef &dObj = e->getDataObject();
	
	if (!dObj)
		return;
	
	// Should be: HAGGLE_ATTR_CONTROL_NAME
	if (dObj->getAttribute("HaggleIPC"))
		return;
	
	attr = dObj->getAttribute("Forward");
	
	if (attr && attr->getValue() != "*")
		return;
	
	attr = dObj->getAttribute("Hide");
	
	if (attr && attr->getValue() == "this")
		return;
	
	if (!dObj->isPersistent())
		return;
		
	InterfaceRef remoteIface = dObj->getRemoteInterface();
	
	NodeRef node = kernel->getNodeStore()->retrieve(remoteIface);
	
	if (!node || strlen(node->getIdStr()) == 0)
		return;
	
	string nodeIDStr = getNodeIDStr(dObj);
	
	sendEvent(client, 
		  e->getName(), 
		  getDOIDStr(e->getDataObject()).c_str(),
		  nodeIDStr != "" ? nodeIDStr.c_str() : (node ? "-" : NULL),
		  node ? getNodeIDStr(node).c_str() : NULL);
}

void VendettaManager::onNodeEvent(Event *e)
{
	NodeRef &neighbor = e->getNode();
	
	if (neighbor)
		sendEvent(client, e->getName(), getNodeIDStr(neighbor).c_str());
}

void VendettaManager::onNodeNodeListEvent(Event *e)
{
	if (client) {
		NodeRefList &nodes = e->getNodeList();
		NodeRef &node = e->getNode();
		
		for (NodeRefList::iterator it = nodes.begin(); it != nodes.end(); it++) {
			sendEvent(client, 
				  e->getName(), 
				  getNodeIDStr(node).c_str(), 
				  getNodeIDStr((*it)).c_str());
		}
	}
}

void VendettaManager::onNodeNodeListDataObjectEvent(Event *e)
{
	if (client) {
		NodeRef &target = e->getNode();
		NodeRefList &nodes = e->getNodeList();
		DataObjectRef &dObj = e->getDataObject();
		
		for (NodeRefList::iterator it = nodes.begin(); it != nodes.end(); it++) {
			sendEvent(client, 
				  e->getName(), 
				  getDOIDStr(dObj).c_str(), 
				  getNodeIDStr((*it)).c_str(),
				  getNodeIDStr(target).c_str());
		}
	}
}

void VendettaManager::onNodeDataObjectEvent(Event *e)
{
	NodeRef &node = e->getNode();
	DataObjectRef &dObj = e->getDataObject();
	if (node && dObj)
		sendEvent(client, 
			  e->getName(), 
			  getDOIDStr(dObj).c_str(), 
			  getNodeIDStr(node).c_str());
}

void VendettaManager::onShutdown()
{
	sendEvent(client, Event::getTypeName(EVENT_TYPE_SHUTDOWN));
	client->handleQuit();
	HAGGLE_DBG("Joining with client thread\n");
	client->join();
	HAGGLE_DBG("Joined with client\n");
	
	unregisterWithKernel();
}

void VendettaManager::onLocalInterfaceUp(Event *e)
{
	// A local inteface was brought up. This is our trigger to start the 
	// Vendetta client in hope that we can connect to the Vendetta monitor
	// over this interface
	if (client && !client->isRunning())
		client->start();
}


void VendettaManager::onConfig(Metadata *m)
{
	Metadata *sm = m->getMetadata("SiteManager");
	
	if (sm) {
		struct in_addr ip;
		unsigned short udp_port = SITE_MANAGER_UDP_PORT, tcp_port = SITE_MANAGER_UDP_PORT;
		
		HAGGLE_DBG("SiteManager configuration...\n");
		
		ip.s_addr = inet_addr(SITE_MANAGER_ADDRESS);
		
		const char *param = sm->getParameter("ip");
		
		if (param) {
			ip.s_addr = inet_addr(param);
				
			HAGGLE_DBG("config site manager address is %s\n", ip_to_str(ip));
		}
		
		param = sm->getParameter("udp_port");
		
		if (param) {
			char *endptr = NULL;
			udp_port = strtoul(param, &endptr, 10) & 0xFFFF;
			
			if (endptr && endptr != param) {
				HAGGLE_DBG("config site manager udp port is %u\n", tcp_port);
			}
		}
		
		param = sm->getParameter("tcp_port");
		
		if (param) {
			char *endptr = NULL;
			tcp_port = strtoul(param, &endptr, 10) & 0xFFFF;
			
			if (endptr && endptr != param) {
				HAGGLE_DBG("config site manager tcp port is %u\n", tcp_port);
			}
		}
		
		if (client)
			client->setSiteManager(ip, udp_port, tcp_port);
	}
}
