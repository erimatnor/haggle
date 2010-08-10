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

#include "VendettaAsynchronous.h"
VendettaAsynchronous::VendettaAsynchronous(VendettaManager *m, const string name) :
	ManagerModule<VendettaManager>(m, name), should_send_events(false), should_call_handle_event(false)
{
	ping_time_delta = Timeval(5,0);
	next_ping_time = Timeval::now() + ping_time_delta;
}

VendettaAsynchronous::~VendettaAsynchronous()
{
	while (!taskQ.empty()) {
		Task *task = NULL;
		taskQ.retrieve(&task, NULL);
		
		if (task)
			delete task;
	}
}

void VendettaAsynchronous::handleEvent(void)
{
	should_call_handle_event = true;
	call_handle_event_at = Timeval::now() + Timeval(2,0);
	taskQ.insert(new Task(TASK_TYPE_EVENT));
}

void VendettaAsynchronous::handleSendEvent(string &event, string &params)
{
	taskQ.insert(new Task(TASK_TYPE_SEND, event, params));
}

void VendettaAsynchronous::handleQuit(void)
{
	//cancel();
	//_handleQuit();
	taskQ.insert(new Task(TASK_TYPE_QUIT));
}

bool VendettaAsynchronous::run(void)
{
	HAGGLE_DBG("Running task queue\n");
	
	while (!shouldExit()) {
		Timeval now = Timeval::now();
		Task *task = NULL;
		Timeval	time_left, time_left_ping, time_left_handle_event;
		bool call_handle_event;
		
		time_left_ping = next_ping_time - now;
		
		if (should_call_handle_event) {
			time_left_handle_event = call_handle_event_at - now;
			if (time_left_handle_event < time_left_ping) {
				call_handle_event = true;
				time_left = time_left_handle_event;
			} else {
				call_handle_event = false;
				time_left = time_left_ping;
			}
		} else {
			time_left = time_left_ping;
			call_handle_event = false;
		}
		
		//HAGGLE_DBG("Handling task\n");
		
		QueueEvent_t qe = taskQ.retrieve(&task, &time_left);
		
		switch (qe) {
			case QUEUE_WATCH_ABANDONED:
				HAGGLE_DBG("Queue wait was abandoned... cancelling.\n");
				cancel();
				break;
			case QUEUE_TIMEOUT:
				if (call_handle_event) {
					//HAGGLE_DBG("handle event\n");
					_handleEvent();
					should_call_handle_event = false;
				} else {
					//HAGGLE_DBG("send ping\n");
					_sendPING();
				}
				// Update the time until the next PING:
				
				while (next_ping_time < now) {
					next_ping_time += ping_time_delta;
				}
				break;
				
			case QUEUE_ELEMENT:
				switch (task->type) {
					case TASK_TYPE_EVENT:
						//_handleEvent();
						//HAGGLE_DBG("handle event (element)\n");
						break;
					case TASK_TYPE_SEND:
						//HAGGLE_DBG("handle send event (element)\n");
						_handleSendEvent(task->event, task->params);
						break;
					case TASK_TYPE_QUIT:
						HAGGLE_DBG("Quit!\n");
						cancel();
						_handleQuit();
						break;
				}
				break;
			default:
				HAGGLE_DBG("taskQ returned unexpected event %d. Ignoring!\n", qe);
				break;
		}
		
		if (task)
			delete task;
	}
	return false;
}
