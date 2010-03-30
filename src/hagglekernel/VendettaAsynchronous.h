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
	Asynchronous forwarding module. A forwarding module should inherit from this
	module if it is doing too much processing to be executing in the kernel 
	thread.
 */
class VendettaAsynchronous : public ManagerModule<VendettaManager> {
	/**
	 This enum is used in the actions to tell the run loop what to do.
	 */
	typedef enum {
		TASK_TYPE_EVENT,
		TASK_TYPE_SEND,
		// Terminate the run loop
		TASK_TYPE_QUIT
	} TaskType_t;
	
	/**
	 These action elements are used to send data to the run loop, in order to
	 make processing asynchronous.
	 */
	class Task {
	public:
		TaskType_t type;
		string event;
		string params;
		Task(TaskType_t _type,
			  string _event = "",
			  string _params = "") : type(_type), event(_event), params(_params) {}
		~Task() {}
	};
	
	GenericQueue<Task *> taskQ;
	
	Timeval next_ping_time;
	Timeval ping_time_delta;
	Timeval call_handle_event_at;
	bool should_send_events, should_call_handle_event;
	bool run(void);
protected:
	virtual void _handleEvent(void) = 0;
	virtual void _handleSendEvent(string &event, string &params) = 0;
	virtual void _sendPING(void) {};
	
public:
	VendettaAsynchronous(VendettaManager *m = NULL, const string name = "Asynchronous vendetta module");
	~VendettaAsynchronous();
	
	/** */
	void handleEvent(void);
	void handleSendEvent(string &event, string &params);
	void handleQuit(void);
	void startSendingPings(void) { should_send_events = true; }
};

#endif
