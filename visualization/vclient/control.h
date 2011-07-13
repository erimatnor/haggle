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

#include "globals.h"

// Control event function CEF 

typedef int(*control_event_function)(int, void*);

//  control_event_function* cef; function ptr to call
//  unsigned long at;            when to call it
//  void* args;                  ptr to struct with args specific to cef
//  int resched;                 scheduling flag, 0 = once, other values cef specific 
//  struct ce_list* nxt;         lst ptr

struct ce_list{
  control_event_function cef;
  struct timeval at; 
  void* args;
  int resched;
  struct ce_list* nxt;  
};

int read_control_msg(struct vclient_config* vcc);
struct timeval* next_control_event(void);
int fire_control_events(struct vclient_config* vcc);
int start_pings(struct vclient_config* vcc);
int get_control_event_timeout(struct vclient_config* vcc,struct timeval* to);
int is_before(struct timeval* a, struct timeval* b);

/* vim:set sw=2 ts=8 expandtab: */
