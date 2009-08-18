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
VendettaAsynchronous::VendettaAsynchronous(
		VendettaManager *m, 
		const string name) :
    ManagerModule<VendettaManager>(m,name)
{
    ping_time_delta = Timeval(5,0);
    next_ping_time = Timeval::now() + ping_time_delta;
    should_call_handle_event = false;
	should_send_events = false;
    start();
}

VendettaAsynchronous::~VendettaAsynchronous()
{
	// Tell the thread to quit:
	actionQueue.insert(new VC_Action(VC_quit));
	// Make sure noone else adds stuff to the queue:
	actionQueue.close();
	// Wait for the thread to terminate:
        stop();
}

void VendettaAsynchronous::handleEvent(void)
{
    should_call_handle_event = true;
    call_handle_event_at = Timeval::now() + Timeval(2,0);
    actionQueue.insert(new VC_Action(VC_event));
}

void VendettaAsynchronous::handleSendEvent(string &event, string &params)
{
    actionQueue.insert(new VC_Action(VC_send_event, event, params));
}

void VendettaAsynchronous::handleQuit(void)
{
    actionQueue.insert(new VC_Action(VC_quit));
}

bool VendettaAsynchronous::run(void)
{

	while(!shouldExit() && !should_send_events)
	{
		cancelableSleep(10);
	}

	while(!shouldExit())
	{
		VC_Action *action;
		Timeval	time_left, time_left_ping, time_left_handle_event;
		bool call_handle_event;

		action = NULL;
		time_left_ping = next_ping_time - Timeval::now();
		if(should_call_handle_event)
		{
			time_left_handle_event = call_handle_event_at - Timeval::now();
			if(time_left_handle_event < time_left_ping)
			{
				call_handle_event = true;
				time_left = time_left_handle_event;
			}else{
				call_handle_event = false;
				time_left = time_left_ping;
			}
		}else{
			time_left = time_left_ping;
			call_handle_event = false;
		}

		switch(actionQueue.retrieve(&action, &time_left))
		{
		default:
			break;

		case QUEUE_TIMEOUT:
			if(call_handle_event)
			{
				_handleEvent();
				should_call_handle_event = false;
			}else{
				_sendPING();
			}
			// Update the time until the next PING:
			{
				Timeval Now = Timeval::now();

				while(next_ping_time < Now)
				{
					next_ping_time += ping_time_delta;
				}
			}
			break;

		case QUEUE_ELEMENT:
			switch(action->action)
			{
			case VC_event:
				//_handleEvent();
				break;

			case VC_send_event:
				_handleSendEvent(action->event, action->params);
				break;

			case VC_quit:
				cancel();
				break;
			}
			break;
		}
		if(action)
			delete action;
	}
	return false;
}
