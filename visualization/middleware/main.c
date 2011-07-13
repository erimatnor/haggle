/*
** main.c -- Site Manager  for wisenet testbed
*  Written By Christofer Ferm
*/
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/wait.h>
#include <signal.h>
#include <netdb.h>

#define MONPORT 5051 // the port monitors will be connecting to
#define VCLPORT 4444 // the port vclients will be connecting to  
#define MCTRLPORT 5000 // the port monitors send control msg on
#define UDPPING "4445" //the port for  PING msgs from vclients
#define BACKLOG 20  // how many pending connections queue will hold
#define MAX_MONITORS 500 //maximum number of monitors
#define MAX_CHILD 500  
int g=1; 

int numoffd =0; 

int mon[MAX_MONITORS]; 

pthread_t thread[MAX_CHILD];

struct thread_data 
{
  int thread_id; 
  int sockfd; 
  int new_fd; 
  socklen_t sin_size; 
  struct sockaddr_in their_addr; 
  

};

struct thread_data2
{
  int thread_id; 
  int sockfd; 
  struct sockaddr_storage their_addr; 

};


struct thread_data2 threadArgs2; 
struct thread_data threadArgs[5]; 


/***FUNCTIONS**********/

//init the monitor array 
void init_mon()
{
  int i=0; 
  while(i <MAX_MONITORS)
  {
    mon[i]=0; 
    i++;
  }


}

//check for free thread space, if we get -1 then we have the max number of threads running
int find_free_thread()
{
  int i=0; 

  while(i<MAX_CHILD)
  {
    if(thread[i]==0)
      {
	return i; 

      }
   
    i++; 

  }

  return -1; 

}


//send a message to all connected monitors 
void print_monitor_list()
{
  // printf("send_to_all_monitors:got called\n"); 
  struct sockaddr_in in_addr; 
  socklen_t addrlen = sizeof(in_addr); 

  int i=0; 

  while(i<MAX_MONITORS)
  {
    if(mon[i]!=0)
    {
      getpeername(mon[i], (struct sockaddr *)&in_addr, &addrlen);
      
   

      char *address = inet_ntoa(in_addr.sin_addr); 

      printf("ID %d monitor address: %s\n",mon[i],address); 
     

    }

    i++;

  }
  
  



}


//send a message to all connected monitors 
void send_to_all_monitors(char *tosend, int length)
{
  //printf("send_to_all_monitors:got called\n"); 
  struct sockaddr_in in_addr; 
  socklen_t addrlen = sizeof(in_addr); 

  int i=0; 

  while(i<MAX_MONITORS)
  {
    if(mon[i]!=0)
    {
      getpeername(mon[i], (struct sockaddr *)&in_addr, &addrlen);
      
   

      char *address = inet_ntoa(in_addr.sin_addr); 
    struct timeval tv;
    fd_set readfds;
    fd_set except_fds;

    tv.tv_sec = 0;
    tv.tv_usec = 0;

    FD_ZERO(&readfds);
    FD_SET(mon[i], &readfds);

    // don't care about writefds and exceptfds:
    select(mon[i]+1, &readfds, NULL, &except_fds, &tv);

    if (FD_ISSET(mon[i], &except_fds))
	{
		close(mon[i]);
		mon[i] = 0;
	}
    if (FD_ISSET(mon[i], &readfds))
	{
      // printf("sending:  %s\n",tosend); 

int l= send(mon[i],tosend,length, 0); 

 if(l == length)
   {
     // printf("The Whole thing got sent\n");
     
   }
	}


    }

    i++;

  }
  
  



}
//add a monitor  to the list
void add_mon(int new_fd)
{

  int i = 0;

  while(i<MAX_MONITORS)
  {
    if(mon[i] == 0)
      {
	mon[i]=new_fd;
	numoffd++; 
	i=MAX_MONITORS; 
      }
     else 
     {
       i++; 
     }

 
  }

}

//remove a monitor 
void rm_mon(int re_fd)
{
  int i=0; 
  while(i < MAX_MONITORS)
  {
    if(mon[i]==re_fd)
    {
      mon[i]=0; 
      numoffd--; 
      i=MAX_MONITORS;
    }
    else 
    {
      i++; 
    }



  }




}

//setup the listening sockets  
int setup_socket(int *sock)
{

  int yes=1; 
 if ((*sock = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        return -1;
    }
    if (setsockopt(*sock, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1) {
        perror("setsockopt");
        return -1; 
    }

    return 0; 

}



int bind_socket(int *sock,int port)
{
    
    struct sockaddr_in my_addr; // my address information

    my_addr.sin_family = AF_INET;        // host byte order
    my_addr.sin_port = htons(port);    // short, network byte order
    my_addr.sin_addr.s_addr = INADDR_ANY; // automatically fill with my IP
    

    memset(my_addr.sin_zero, '\0', sizeof my_addr.sin_zero);
    if (bind(*sock, (struct sockaddr *)&my_addr, sizeof my_addr) == -1) {
        perror("bind");
        return -1;
      }

    return 0; 
}


void ExtractIP(char buf[],char *address)
{
  int i=0;
  int addcnt=0; 
  int spacecnt =0; 

  for(i=0;i<512;i++)
  {
    
    if(spacecnt==1)
    {
      
      if(buf[i]!=' ')
      {
	address[addcnt] = buf[i];
	addcnt++;
      }
      else 
      {
	address[i]='\0';
	i=512;
	spacecnt++; 
      }

    }
    else if(buf[i]==' ')
    {
      spacecnt++; 
      

    }
    

    //  printf("%d\n",i);
   

  }
  
 

}


int ExtractPort(char buf[])
{
  int i=0;
  char port[4];
  int portcnt=0; 
  int spacecnt =0; 

  for(i=0;i<512;i++)
  {
    
    if(spacecnt==2)
    {
      
      if(portcnt<4)
      {
	port[portcnt] = buf[i];
	portcnt++;
      }
      else 
      {
	port[4]='\0';
	i=512; 
      }

    }
    else if(buf[i]==' ')
    {
      spacecnt++; 
      

    }
    

    //  printf("%d\n",i);
   

  }
  
  
  
return atoi(port);


}


/****THREADS******************/

void *MDown(void*  Args)
{

  struct thread_data *my_data;
  
  int taskid,monitorfd,mon_fd;
  struct sockaddr_in their_addr; 
  socklen_t sin_size; 
  
signal(SIGPIPE, SIG_IGN);
  my_data = (struct thread_data *) Args; 
  taskid = my_data->thread_id; 
  monitorfd = my_data->sockfd; 
  mon_fd = my_data->new_fd; 
  their_addr = my_data->their_addr; 
  sin_size = my_data->sin_size; 

 
	while(1)
	{
	  sin_size = sizeof their_addr; 
	   if ((mon_fd = accept(monitorfd, (struct sockaddr *)&their_addr, \
                &sin_size)) == -1) {
            perror("accept");
            continue;
        }
	else
	{

        printf("server: got connection from new Monitor at %s\n", \
	       inet_ntoa(their_addr.sin_addr));
       
	//  if (send(mon_fd, "Hello, world!\n", 14, 0) == -1)
	//      perror("send");
	    // int i = 0; 

	    //char *temp; 
		//	sprintf(temp, "test:%d\n",i);
		add_mon(mon_fd); 
		//send(new_fd, temp, strlen(temp), 0);
		//i++; 
		//	send_to_all_monitors("New Monitor Connected\n",strlen("New Monitor Connected\n"));
	  }


	}

	close(monitorfd); 
	pthread_exit(NULL); 


} 

void *VChild(void * Args)
{

       int len;

       struct sockaddr_in their_addr; 
       int vcl_fd;

       struct thread_data *my_data; 
       
 signal(SIGPIPE, SIG_IGN);
      my_data = (struct thread_data *)Args; 
       
       vcl_fd = my_data->new_fd; 
       their_addr = my_data->their_addr; 
       

      
	 len=1; 
            while(len!=0)
	    {
	      char buf[512]; 
	       len= recv(vcl_fd,buf,512,0);  
	      if(len == -1)
		{
		   close(vcl_fd); 
		  
		 printf("(VCHILD) %s disconnected\n", 
			inet_ntoa(their_addr.sin_addr));
		  break; 
		}
	      else 
		{
		  
		  send_to_all_monitors(buf,len); 
		  
		  
		}
	      
	    
	    

       }
	    
     close(vcl_fd);
     pthread_exit(NULL); 



}


void *VMCtrl(void* Args)
{
 int len;

       struct sockaddr_in their_addr; 
       int ctrl_fd;

       struct thread_data *my_data; 
       
signal(SIGPIPE, SIG_IGN);
       my_data = (struct thread_data *)Args; 
       
       ctrl_fd = my_data->new_fd; 
       their_addr = my_data->their_addr; 
       

      
	 len=1; 
            while(len!=0)
	    {
	      char buf[512]; 
	       len= recv(ctrl_fd,buf,512,0);  
	      if(len == -1)
		{
		   close(ctrl_fd); 
		  
		 printf("(VCTRL) %s disconnected\n", 
			inet_ntoa(their_addr.sin_addr));
		  break; 
		}
	      else 
		{
		  
		  int port = ExtractPort(buf);  
		  char *address; 
		  
		  address = (char *)malloc(512);

		  ExtractIP(buf,address);
		  

		  printf("Destination Address:%s PORT:%d\n", address,port); 
		  len=0;
		  
		}
	      
	    
	    

       }
	    
     close(ctrl_fd);
     pthread_exit(NULL); 



}


void *MCtrl(void* Args)
{
  struct thread_data *my_data1;
  
  int taskid,mctrlfd;
  struct sockaddr_in their_addr; 
  socklen_t sin_size; 
  
signal(SIGPIPE, SIG_IGN);
  my_data1 = (struct thread_data *) Args; 
  taskid = my_data1->thread_id; 
  mctrlfd = my_data1->sockfd; 
  their_addr = my_data1->their_addr; 
  sin_size = my_data1->sin_size; 


  while(1) { // main accept() loop
        sin_size = sizeof their_addr;
	int ctrl_fd; 
	
	if ((ctrl_fd = accept(mctrlfd, (struct sockaddr *)&their_addr, \
                &sin_size)) == -1) {
            perror("accept");
            break;
        }
	else
	{

        printf(" Got command from monitor at %s\n", \
	       inet_ntoa(their_addr.sin_addr));
       
	
	   
        //NEW THREAD HERE
	
	
    int th; 

    threadArgs[5].new_fd = ctrl_fd; 
    threadArgs[5].their_addr = their_addr; 
    threadArgs[5].sin_size = sin_size; 
    
    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating VMCtrl Thread 
       pthread_create(&thread[th],NULL, VMCtrl,(void *)&threadArgs[2]);
    }
    
	

	

	}

  }
  close(mctrlfd);
  pthread_exit(NULL);

}







void *VDown(void *Args)
{

  struct thread_data *my_data1;
  
  int taskid,vclientfd;
  struct sockaddr_in their_addr; 
  socklen_t sin_size; 
  
signal(SIGPIPE, SIG_IGN);
  my_data1 = (struct thread_data *) Args; 
  taskid = my_data1->thread_id; 
  vclientfd = my_data1->sockfd; 
  their_addr = my_data1->their_addr; 
  sin_size = my_data1->sin_size; 


  while(1) { // main accept() loop
        sin_size = sizeof their_addr;
	int vcl_fd; 
	
	if ((vcl_fd = accept(vclientfd, (struct sockaddr *)&their_addr, \
                &sin_size)) == -1) {
            perror("accept");
            break;
        }
	else
	{

        printf("server: got connection from new vclient at %s\n", \
	       inet_ntoa(their_addr.sin_addr));
       
	
	   
        //NEW THREAD HERE
	printf("forking off a new VDOWN process\n");
	
    int th; 

    threadArgs[4].new_fd = vcl_fd; 
    threadArgs[4].their_addr = their_addr; 
    threadArgs[4].sin_size = sin_size; 
    
    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating VChild Thread 
       pthread_create(&thread[th],NULL, VChild,(void *)&threadArgs[3]);
    }
    
	

	

	}

  }
  close(vclientfd);
  pthread_exit(NULL);

}

void *UChild(void* Args)
{

  char buf[512]; 
  
  int udpfd;  
  int numbytes; 
  socklen_t addr_len;
  struct sockaddr_storage their_addr;  
  struct thread_data2 * my_data; 

signal(SIGPIPE, SIG_IGN);
   my_data = (struct thread_data2 *)Args;

  udpfd = my_data->sockfd; 
  their_addr = my_data->their_addr; 
  

  

  

  //printf("waiting to receive\n");



addr_len = sizeof their_addr; 


if((numbytes = recvfrom(udpfd, buf, 512, 0,(struct sockaddr *)&their_addr, &addr_len))==-1){
  perror("UDPDOWN:recvfrom"); 

 }
 else
 {
 send_to_all_monitors(buf,numbytes+1);
 }
close(udpfd); 
pthread_exit(NULL);


}

void *UDown(void* Args)
{
signal(SIGPIPE, SIG_IGN);

  while(1)
  {
 
  int udpfd; 
  
  struct addrinfo hints, *servinfo, *p; 
  int rv; 
  int numbytes; 
  struct sockaddr_storage their_addr; 
  char buf[512]; 
  
 
  socklen_t addr_len; 
  char s[INET6_ADDRSTRLEN]; 
  memset(&hints, 0, sizeof hints); 
  hints.ai_family = AF_UNSPEC; 
  hints.ai_socktype = SOCK_DGRAM; 
  hints.ai_flags = AI_PASSIVE; //use my IP; 

  

  if((rv = getaddrinfo(NULL,UDPPING, &hints, &servinfo))==-1)
  {
    fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
    pthread_exit(NULL); 
  }
 

  //loop and bind 
p = servinfo; 


  if(p != NULL)
    {
   
  if((udpfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol))==-1)
    {
      close(udpfd);
      //  perror("UDPlistener:socket");
      continue; 
    
    }
  
  if(bind(udpfd,p->ai_addr,p->ai_addrlen)==-1)
    {
      close(udpfd);
      // perror("UDPLISTENER: BIND"); 
      continue; 

    }
  else
    {
    


      
      // threadArgs2.sockfd = udpfd; 
      // threadArgs2.their_addr = their_addr;


      int th;  

      /* if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating UChild Thread 
       pthread_create(&thread[th],NULL, UChild,(void *)&threadArgs2);
       }*/

      addr_len = sizeof their_addr; 
      
      int i=0; 
      while(i<513)
	{
	  buf[i]='\n';
	  i++;
	}


      if((numbytes = recvfrom(udpfd, buf, 512, 0,(struct sockaddr *)&their_addr, &addr_len))==-1){
	perror("UDPDOWN:recvfrom"); 

      }
      else
	{

	  buf[numbytes+1]='\0';
	  send_to_all_monitors(buf,numbytes+1);
	}
      

      close(udpfd);
      }
    }
  
    
 
  



freeaddrinfo(servinfo);  

}

pthread_exit(NULL); 




}

/****MAIN FUNCTION******/

int main(void)
{   
signal(SIGPIPE, SIG_IGN);
  init_mon(); 
  
  //pointer to monitorlist; 
  int monitorfd,vclientfd,mctrlfd; // listen on monitorfd,vclientfd and mctrlfd 
    struct sockaddr_in mon_addr; // monconnector's address information
    struct sockaddr_in vcl_addr; // vclconnector's address information
    socklen_t sin_size;
   

    //steup socket paramters for monitorfd
    if(setup_socket(&monitorfd)==-1)
      {
	perror("MONSETSOCKET");
	exit(0);
      }
   
    //steup socket paramters for vclientfd
    if(setup_socket(&vclientfd)==-1)
      {
	perror("VCLSETSOCKET:"); 
	exit(0);
      }

     //steup socket paramters for mctrlfd
    if(setup_socket(&mctrlfd)==-1)
      {
	perror("VCLSETSOCKET:"); 
	exit(0);
      }

   
   
    //bind monitorfd to MONPORT 
    if(bind_socket(&monitorfd,MONPORT)==-1)
      {
	perror("MONBIND:"); 
	exit(0);
      }
   //bind vclientfd to VCLPORT 
    if(bind_socket(&vclientfd,VCLPORT)==-1)
      {
	perror("VCLBIND:");
	exit(0);
      }

    //bind mctrlfd to MCTRLPORT 
    if(bind_socket(&mctrlfd,MCTRLPORT)==-1)
      {
	perror("VCLBIND:");
	exit(0);
      }

   
  
    
    //listen on monitorfd 
    if (listen(monitorfd, BACKLOG) == -1) {
        perror("MONLISTEN:");
        exit(1);
    }
    
     //listen on vclientfd 
    if (listen(vclientfd, BACKLOG) == -1) {
        perror("VCLLISTEN:");
        exit(1);
    }
    
     //listen on mctrlfd 
    if (listen(mctrlfd, BACKLOG) == -1) {
        perror("VCLLISTEN:");
        exit(1);
    }


   

     

    threadArgs[0].sockfd = monitorfd; 
    threadArgs[0].their_addr = mon_addr; 
    threadArgs[0].sin_size = sin_size;

    threadArgs[1].sockfd = vclientfd; 
    threadArgs[1].their_addr = vcl_addr; 
    threadArgs[1].sin_size = sin_size; 

    threadArgs[2].sockfd = mctrlfd;  
    threadArgs[2].their_addr = vcl_addr; 
    threadArgs[2].sin_size = sin_size; 


    
 
    int th;  

    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating MDown Thread 
       pthread_create(&thread[th],NULL, MDown,(void *)&threadArgs[0]);
    }
    

    

    
    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating VDown Thread 
    pthread_create(&thread[th],NULL, VDown,(void *)&threadArgs[1]);
    }
    
    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
    //Creating MCtrl Thread 
      pthread_create(&thread[th],NULL, MCtrl,(void *)&threadArgs[2]);
    }

    //Create UDown Thread
    printf("Create UDown Thread\n");
    if(th = find_free_thread()==-1)
    {
      printf("cannot create more threads limit reached\n"); 

    } 
    else
    {
   
    pthread_create(&thread[th],NULL, UDown,(void *)&threadArgs[2]);
    }
    

	
    //menu and command loop
	
    int cmd=1; 
    printf("Middleware C version(press ctrl c to quit\n)");
    while(cmd != 0)
    {
      
		sleep(1);
      
      
      


    }



    pthread_exit(NULL);     

     return 0;
 }
