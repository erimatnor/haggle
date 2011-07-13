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

#include <sys/types.h>
#include <regex.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include "configparser.h"
#include "logevent.h"
#include "logparser.h"
#include "globals.h"
#include "log.h"

int parse_ping_interval(FILE* f,struct vclient_config* vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  
  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", token) == 1){
    
      if(strcmp(token, "</PING_INTERVALL>")==0){
	break;
      }
      
      if(read == 0){
	read = sscanf(token, "%d", &vcc->ping_interval);
      }
    }
    ret = fgets(buf,BUFSIZE,f);
  }
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING ping interval, got %d\n", vcc->ping_interval);
#endif  
  
  // got value, all ok
  if(read == 1)
    return 0;

  return 1;

}

int parse_udp_timeout(FILE* f,struct vclient_config* vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  
  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", token) == 1){
      
      if(token[0] != '#'){

	if(strcmp(token, "</UDP_TIMEOUT>")==0){
	  break;
	}
	
	if(read == 0){
	read = sscanf(token, "%d", &vcc->udp_timeout);
	}
	
      }
    }
    ret = fgets(buf,BUFSIZE,f);
  }
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING udp timeout, got %d\n", vcc->udp_timeout);
#endif  
  
  // got value, all ok
  if(read == 1)
    return 0;

  return 1;

}

int parse_logparser(FILE* f,struct vclient_config* vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  
  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", token) == 1){

      if(token[0] != '#'){
	
	if(strcmp(token, "</LOGPARSER>")==0){
	  break;
	}
	
	if(read == 0){
	  read = sscanf(buf, "%s", vcc->logparser);
	}
      }
    }
    
    ret = fgets(buf,BUFSIZE,f);
  }
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING logparser, got %s\n", vcc->logparser);
#endif  
  
  // got value, all ok
  if(read == 1)
    return 0;

  return 1;

}

int parse_eventfilename(FILE* f, struct vclient_config* vcc){
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  
  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", token) == 1){

      if(token[0] != '#'){
	
	if(strcmp(token, "</EVENTDEFINES>")==0){
	  break;
	}
	
	if(read == 0){
	  read = sscanf(buf, "%s", vcc->event_filename);
	}
      }
    }
    
    ret = fgets(buf,BUFSIZE,f);
  }
  
  // got value, all ok
  if(read == 1)
    return 0;

  return 1;

}


int parse_logfilename(FILE* f, struct vclient_config* vcc){
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  
  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", token) == 1){

      if(token[0] != '#'){
	
	if(strcmp(token, "</LOGFILENAME>")==0){
	  break;
	}
	
	if(read == 0){
	  read = sscanf(buf, "%s", vcc->log_filename);
	}
      }
    }
    
    ret = fgets(buf,BUFSIZE,f);
  }
  
  // got value, all ok
  if(read == 1)
    return 0;

  return 1;

}


/* method get_method_from_string(char* string){ */
  
  
  
/*   return 0; */
/* } */

int parse_logevent(FILE* f,struct log_event_rule *le){
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  char value[MAXTOKENSIZE];
  char trimmed[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;
  int ec;

  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", trimmed) == 1){
      if(trimmed[0] != '#'){
	
	if(strcmp(trimmed, "</LOGEVENT>")==0){
	  break;
	}
	
	//	printf("buf %s", buf);
	if(sscanf(trimmed, "%[^=]=%[^\n]", token, value) == 2){
	  
	  // printf("value %s\n", value);
	  if(strcmp(token, "type")==0){
	    strcpy(le->type,value); // strncpy
	    read++;
	  }
	  
	  if(strcmp(token, "method")==0){
	    le->method = get_method_from_string(value);
	    read++;
	  }
	  
	  if(strcmp(token, "regexp")==0){
	    // need all of the line, the regexp contains spaces
	    if(sscanf(buf, "%[^=]=%[^\n]", token, value) == 2){
	      
	      // ec=regcomp(&le->regexp, value, REG_EXTENDED);
	      ec=regcomp(&le->regexp, value,0);
	      // strncpy(&le->regexp_s, value, 50);
	      // printf("regexp %s\n", value);
	      if(ec != 0){
		
		char str[256];
		regerror(ec, &le->regexp, str, sizeof str);
		LOG(LOG_ERROR, "%s: %s\n", value, str);
		LOG(LOG_ERROR, "Error parsing regexp %s.\n", value);
		return -1;
	      }
	      
	      read++;
	    } else 
	      return 1;
	  }
			
      	
	  if(strcmp(token, "net")==0){
	    if(strcmp(value, "none")==0){
	      le->net = NET_NO;
	    } else if(strcmp(value, "tcp")==0){
	      le->net = NET_TCP;
	    } else if(strcmp(value, "udp")==0){
	      le->net = NET_UDP;
	    } else {
	      LOG(LOG_ERROR, "Parse error, unknowned net type %s\n", value);
	      return 1;
	    }
	    
	    read++;
	  }
	}
      
      }
      
    }
    
    ret = fgets(buf,BUFSIZE,f);
  }


#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING logevent, got ");
  print_log_event_rule(le);
#endif  
    
    // got value, all ok
    if(read == 4)
      return 0;
    
    return 1;
    
    
}

int parse_monitor(FILE* f,struct vclient_config *vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  char value[MAXTOKENSIZE];
  char trimmed[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;

  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    // printf("buf %s\n", buf);
    if(sscanf(buf, "%s", trimmed) == 1){
      
      if(trimmed[0] != '#'){
	
	if(strcmp(trimmed, "</MONITOR>")==0){
	  break;
	}
      
	if(sscanf(trimmed, "%[^=]=%s", token, value) == 2){
	  
	  if(strcmp(token, "ip")==0){
	    strcpy(vcc->monitor_addr,value); // strncpy
	    read++;
	  }
	  
	  if(strcmp(token, "port")==0){
	    read += sscanf(value, "%d", &vcc->monitor_port);
	  }
	  
	}
	
	
      }
  
    }
    ret = fgets(buf,BUFSIZE,f);
  }
  
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING monitor, %s:%d \n", vcc->monitor_addr, vcc->monitor_port);
#endif  
  
  // got value, all ok
  if(read == 2)
    return 0;
  
  return 1;
    
    
}

int parse_vclient(FILE* f,struct vclient_config *vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  char value[MAXTOKENSIZE];
  char trimmed[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;

  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    //printf("%s",buf);

    if(sscanf(buf, "%s", trimmed) == 1){
     
      // printf("trimmed <%s>\n",trimmed);

      if(trimmed[0] != '#'){
	
	if(strcmp(trimmed, "</VCLIENT>")==0){
	  break;
	}
	
	if(sscanf(trimmed, "%[^=]=%s", token, value) == 2){
	  
	  // printf("token <%s>\n",token);
	  // printf("value <%s>\n",value);
	  
	  if(strcmp(token, "ip")==0){
	    strcpy(vcc->my_addr,value); // strncpy
	    read++;
	  }
	  
	  if(strcmp(token, "port")==0){
	    read += sscanf(value, "%d", &vcc->my_port);
	  }

          if (strcmp(token, "max_delay") == 0) {
            read += sscanf(value, "%u", &vcc->max_delay);
          }
	}
	
      }  
    }
    ret = fgets(buf,BUFSIZE,f);
  } 
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING vclient, %s:%d \n", vcc->monitor_addr, vcc->monitor_port);
#endif  
  
  // got value, all ok
  if(read == 3)
    return 0;
  
  return 1;
    
    
}


int parse_application(FILE* f,struct vclient_config *vcc){
  
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  char value[MAXTOKENSIZE];
  char trimmed[MAXTOKENSIZE];

  int read = 0; // not done
  char* ret;

  ret = fgets(buf,BUFSIZE,f);
  
  while (ret != NULL){
    
    if(sscanf(buf, "%s", trimmed) == 1){
     
      if(trimmed[0] != '#'){

	if(strcmp(trimmed, "</APPLICATION>")==0){
	  break;
	}
      
	if(sscanf(trimmed, "%[^=]=%s", token, value) == 2){
		
	  if(strcmp(token, "appname")==0){
	    strcpy(vcc->app_name,value); // strncpy
	    read++;
	  }
	  		
/*	  if(strcmp(token, "appargs")==0){
	    strcpy(vcc->app_args,value); // strncpy
	    read++;
	  }*/

	  
	}
	
      }  
    }
    ret = fgets(buf,BUFSIZE,f);
  } 
  
#ifdef TEST
  LOG(LOG_DEBUG, "CONFIG PARSING vclient, %s:%d \n", vcc->monitor_addr, vcc->monitor_port);
#endif  
  
  // got value, all ok
  if(read == 1)
    return 0;
  
  return 1;
    
    
}


int parse_event_config(struct vclient_config* vcc){
  
  FILE* f = fopen (vcc->event_filename, "r");
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  int ret; 
  //  int ping_interval;
  //  int udp_timeout;
  //  char logparser[100];
  
  struct log_event_rule ev;
  

  if (f == NULL){
    LOG(LOG_ERROR, "Error opening config file %s.\n", vcc->event_filename);
    perror("config file ");
    return 1;
  }
  
  ret = 0;
  
  while (fgets(buf,BUFSIZE,f) != NULL){
    
    if(ret != 0){
      LOG(LOG_ERROR, "Error parsing config file %s.\n", vcc->event_filename);
      fclose(f);
      return 1;
    }

    if(sscanf(buf, "%s", token) != 1)
      continue;
    
    if(strcmp(token, "<PING_INTERVALL>")==0){
      ret = parse_ping_interval(f,vcc);
      continue;
    }
    
    if(strcmp(token, "<UDP_TIMEOUT>")==0){
      ret = parse_udp_timeout(f,vcc);
      continue;
    }
    
    if(strcmp(token, "<LOGPARSER>")==0){

      // we do not care about logparser in this client
      ret = parse_logparser(f, vcc);
      continue;
    }

    if(strcmp(token, "<LOGEVENT>")==0){
      ret = parse_logevent(f,&ev);
      if(ret == 0){
	add_log_event_rule(&ev);
      }
      continue;
    }
    
    if(token[0] == '#')
      continue;

    LOG(LOG_ERROR, "Error in file %s, %s.\n",vcc->event_filename, token);
    ret =  1;
    break;
  }

  fclose (f);
  return ret;

}

int parse_config(struct vclient_config* vcc){
  
  FILE* f = fopen (vcc->config_filename, "r");
  char buf[BUFSIZE];
  char token[MAXTOKENSIZE];
  int ret; 
  
  //struct log_event ev;
  
  LOG(LOG_INFO, "Opening config file %s.\n", vcc->config_filename);
  
  if (f == NULL){
    LOG(LOG_ERROR, "error opening config file %s\n", vcc->config_filename);
    perror("config file ");
    return 1;
  }
  
  ret = 0;
  
  while (fgets(buf,BUFSIZE,f) != NULL){
    
    if(ret != 0){
      LOG(LOG_ERROR, "error parsing config file %s\n", vcc->config_filename);
      fclose(f);
      return 1;
    }

    if(sscanf(buf, "%s", token) != 1)
      continue;
    
    if(strcmp(token, "<MONITOR>")==0){
      ret = parse_monitor(f,vcc);
      continue;
    }
    
    if(strcmp(token, "<VCLIENT>")==0){
      ret = parse_vclient(f,vcc);
      continue;
    }

    if(strcmp(token, "<APPLICATION>")==0){
      ret = parse_application(f,vcc);
      continue;
    }

    if(strcmp(token, "<LOGFILE>")==0){
      ret = parse_logfilename(f,vcc);
      continue;
    }
    
    if(strcmp(token, "<EVENTDEFINES>")==0){
      ret = parse_eventfilename(f,vcc);
      continue;
    }
    
    if(strcmp(token, "<APPLICATION>")==0){

      // we do not care about logparser in this client
      // it will be linked into the app
      // futute work could be to use dlopen() on a .so 

      ret = parse_logparser(f, vcc);
      continue;
    }
    
    /*     if(strcmp(token, "<LOGEVENT>")==0){ */
    /*       ret = parse_logevent(f,&ev); */
    /*       if(ret == 0){ */
    /* 	add_log_event(&ev); */
    /*       } */
    /*       continue; */
    /*     } */
    
    if(token[0] == '#')
      continue;
    
    LOG(LOG_ERROR, "error in file %s, %s\n",vcc->config_filename, token);
    return 1;
    break;
  }
  
  fclose (f);
  return ret;
}


#ifdef TEST

int main (int argc, char ** argv){
  struct vclient_config vcc;
  
  strncpy((void*)vcc.config_filename,  argv[1],MAXAPPSTRLEN);

  
  if(argc != 2){
    printf("Usage: %s configfilename\n", argv[0]);
    return 1;
  } 
    
  parse_config(&vcc);
  dump_event_rules();
  
  return 0;
}

#endif 

/* vim:set sw=2 ts=8 expandtab: */
