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

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "globals.h"
#include "logparser.h"
#include "logevent.h"

int tests = 0;
int tcp_no = 0;

int test_method(char* buf, char* logline){
  printf("Test_method %d _ %s\n",tests++, buf);
  return 0;
}

int send_plain(char *buf, char *logline) {
  sprintf(logline, "%s", buf);
  return 0;
}


int udp(char* buf, char* logline){
  
  sprintf(logline, "%s", buf);
  printf("udp method  _ %s\n", buf);
  return 0;
}

int tcp(char* buf, char* logline){
  sprintf(logline, "%s number %d ", buf, tcp_no);
  printf("TCP %d, %s\n",tcp_no++, buf);
  return 0;
}


// for now, the user needs to provide the 
// mapping between method name in the event config 
// file and the corresponding function. 
// Simply add an 
// if(!strcmp(string, "your method name"))
//       return &your_function
//
// then implement 
// int your_function(char* string, char* logline)
// 
// string is the log line that has matched the provided 
// reg exp, causing your_function() to be called. The function 
// should fill in the logline that will be logged if the function 
// returns 0 

method get_method_from_string(char* string){
  if(!strcmp(string, "foo"))
     return  &test_method;
  if(!strcmp(string, "udp"))
    return  &udp;
  if(!strcmp(string, "tcp"))
     return  &tcp;
  if (!strcmp(string, "send_plain"))
     return  &send_plain;
  
  return 0;
}

/* vim:set sw=2 ts=8 expandtab: */
