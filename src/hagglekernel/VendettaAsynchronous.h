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
#ifndef _VENDETTAASYNCHRONOUS_H
#define _VENDETTAASYNCHRONOUS_H

/*
	Forward declarations of all data types declared in this file. This is to
	avoid circular dependencies. If/when a data type is added to this file,
	remember to add it here.
*/
class VendettaAsynchronous;

#include "ManagerModule.h"
#include "VendettaManager.h"
#include "DataObject.h"
#include "Node.h"

using namespace haggle;

#include <libcpphaggle/String.h>
#include <libcpphaggle/Mutex.h>
#include <libcpphaggle/GenericQueue.h>
#include <haggleutils.h>

/**
	This enum is used in the actions to tell the run loop what to do.
*/
typedef enum {
    VC_event,
    VC_send_event,
	// Terminate the run loop
	VC_quit
} VC_action_type;

/**
	These action elements are used to send data to the run loop, in order to
	make processing asynchronous.
*/
class VC_Action {
public:
	VC_action_type	action;
    string event;
    string params;
	VC_Action(
		VC_action_type _action,
        string _event = "",
        string _params = "") :
        action(_action),
        event(_event),
        params(_params)
		{}
	~VC_Action() {}
};

/**
	Asynchronous forwarding module. A forwarding module should inherit from this
	module if it is doing too much processing to be executing in the kernel 
	thread.
*/
class VendettaAsynchronous : public ManagerModule<VendettaManager> {
	GenericQueue<VC_Action *> actionQueue;
	
    Timeval next_ping_time;
    Timeval ping_time_delta;
    
    bool should_call_handle_event;
    Timeval call_handle_event_at;
    
	/**
		Main run loop for the prophet forwarder.
	*/
	bool run(void);
protected:
	bool should_send_events;
	
	virtual void _handleEvent(void) = 0;
	virtual void _handleSendEvent(string &event, string &params) = 0;
	virtual void _sendPING(void) {};
	
public:
	VendettaAsynchronous(
		VendettaManager *m = NULL, 
		const string name = "Asynchronous vendetta module");
	~VendettaAsynchronous();
	
    /** */
	void handleEvent(void);
    void handleSendEvent(string &event, string &params);
    void handleQuit(void);
	void startSendingPings(void) { should_send_events = true; }
};

#endif
