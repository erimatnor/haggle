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

VendettaManager::VendettaManager(HaggleKernel *_kernel) : 
    Manager("Vendetta manager", _kernel) 
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

        theClient = new VendettaClient(this);

        if (!theClient)
                return false;
	
	onEventQueueRunningCallback = newEventCallback(onEventQueueRunning);
	
	if (onEventQueueRunningCallback)
		// Wait 5 seconds before starting to send pings... that should make it 
		// wait until after the first batch of events.
		kernel->addEvent(new Event(onEventQueueRunningCallback, NULL, 0.0));

        return true;
}

VendettaManager::~VendettaManager()
{
    if (theClient)
            delete theClient;
    if (onEventQueueRunningCallback)
            delete onEventQueueRunningCallback;
}

static void sendEvent(
		VendettaAsynchronous *client,
		const char *event, 
		const char *param1 = NULL, 
		const char *param2 = NULL, 
		const char *param3 = NULL)
{
	// 512 is the buffer size in the site manager...
	char str[512];
	
	if(!client)
		return;
	
	// Create the event string:
	if(param3 != NULL)
	{
		sprintf(
			str,
			"%s %s %s", 
			param1,
			param2,
			param3);
	}else if(param2 != NULL)
	{
		sprintf(
			str,
			"%s %s", 
			param1,
			param2);
	}else if(param1 != NULL)
	{
		sprintf(
			str,
			"%s", 
			param1);
	}else{
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
			NodeRef dNode = Node::create(NODE_TYPE_PEER, dObj);
			
                        if (dNode) {
                                if (getKernel()->getThisNode()->getIdStr() == dNode->getIdStr())
                                        dObjIdStr = "A";
                        }
		}
	}
	return dObjIdStr;
}

static string getNodeIDStr(DataObjectRef dObj)
{
	string dObjIdStr = "";
	if(dObj)
	{
		if(dObj->isNodeDescription())
		{
			NodeRef dNode = Node::create(NODE_TYPE_PEER, dObj);
			
                        if (dNode) {
                                dObjIdStr = dNode->getIdStr();
                                if (dObjIdStr == "[Not yet set]")
                                        dObjIdStr = "[Notyetset]";
                        }
		}
	}
	return dObjIdStr;
}

static string getNodeIDStr(NodeRef node)
{
	string NodeIdStr = "[Unknown]";
	if(node)
	{
		NodeIdStr = node->getIdStr();
		if(NodeIdStr == "[Not yet set]")
			NodeIdStr = "[Notyetset]";
	}
	return NodeIdStr;
}

void VendettaManager::onDataObjectEvent(Event *e)
{
	if(theClient)
		theClient->handleEvent();
	DataObjectRef &dObj = e->getDataObject();
	bool should_send_event = true;
    if(dObj)
    {
		const Attribute *attr;
		// Should be: HAGGLE_ATTR_CONTROL_NAME
		attr = dObj->getAttribute("HaggleIPC");
		if(attr)
			should_send_event = false;
		attr = dObj->getAttribute("Forward");
		if(attr)
			if(attr->getValue() != "*")
				should_send_event = false;
		attr = dObj->getAttribute("Hide");
		if(attr)
			if(attr->getValue() == "this")
				should_send_event = false;
		if(!dObj->isPersistent())
			should_send_event = false;
		if(should_send_event)
		{
			NodeRef node = 
				kernel->getNodeStore()->retrieve(
					dObj->getRemoteInterface());
			string nodeIDStr = getNodeIDStr(dObj);
			sendEvent(
				theClient, 
				e->getName(), 
				getDOIDStr(e->getDataObject()).c_str(),
				nodeIDStr != ""?nodeIDStr.c_str():(node?"-":NULL),
				node?getNodeIDStr(node).c_str():NULL);
		}
    }
}

void VendettaManager::onNodeEvent(Event *e)
{
	NodeRef &neighbor = e->getNode();
	if(neighbor)
		sendEvent(theClient, e->getName(), getNodeIDStr(neighbor).c_str());
}

void VendettaManager::onNodeNodeListEvent(Event *e)
{
    if(theClient)
    {
        NodeRefList &nodes = e->getNodeList();
        NodeRef &node = e->getNode();
        
        for(NodeRefList::iterator it = nodes.begin(); it != nodes.end(); it++)
        {
			sendEvent(
				theClient, 
				e->getName(), 
				getNodeIDStr(node).c_str(), 
				getNodeIDStr((*it)).c_str());
        }
    }
}

void VendettaManager::onNodeNodeListDataObjectEvent(Event *e)
{
    if(theClient)
    {
        NodeRef &target = e->getNode();
        NodeRefList &nodes = e->getNodeList();
        DataObjectRef &dObj = e->getDataObject();
        
        for(NodeRefList::iterator it = nodes.begin(); it != nodes.end(); it++)
        {
			sendEvent(theClient, 
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
	if(node && dObj)
		sendEvent(
			theClient, 
			e->getName(), 
			getDOIDStr(dObj).c_str(), 
			getNodeIDStr(node).c_str());
}

void VendettaManager::hookShutdown()
{
	sendEvent(
		theClient, 
		Event::getTypeName(EVENT_TYPE_SHUTDOWN));
	theClient->handleQuit();
}

void VendettaManager::onEventQueueRunning(Event *e)
{
	if(theClient)
		theClient->startSendingPings();
}
