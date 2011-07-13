#include "net.h"
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>

int main(int argc, char *argv[])
{
	int retval = -2;
	net_socket sock;
	
	signal(SIGPIPE, SIG_IGN);
	
	if(argc > 2)
	{
		int i;
		char str[32];
		
		sock = net_open_tcp_connection(argv[1], strtol(argv[2], NULL, 10));
		if(sock != INVALID_SOCKET)
		{
			if(argc > 3)
			{
				for(i = 3; i < argc; i++)
				{
					write(sock, argv[i], strlen(argv[i]));
					if(i+1 < argc)
						write(sock, " ", 1);
				}
				write(sock, "\n", 1);
			}
		}
		while(!feof(stdin))
		{
			str[0] = fgetc(stdin);
			if(!feof(stdin))
				write(sock, str, 1);
		}
		shutdown(sock, SHUT_WR);
		i = -1;
		do{
			i++;
			struct timeval tv;
			
			do{
				tv.tv_sec = 60;
				tv.tv_usec = 0;
			}while(
				net_wait_on_sockets(
					&tv, 
					mask_network_read_except, 
					sock) == 0);
			switch(read(sock, &(str[i]), 1))
			{
				case 0:
					retval = 2;
				break;
				
				case 1:
				break;
				
				case -1:
					retval = 2;
				break;
			}
		}while(retval == -2 && str[i] != '\n');
		str[i] = '\0';
		printf("Got return value: %s\n", str);
		retval = strtol(str, NULL, 10);
	}else{
		retval = -1;
	}
	return retval;
}
