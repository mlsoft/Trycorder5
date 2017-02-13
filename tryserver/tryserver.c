/*
    C socket server example, handles multiple clients using threads
*/
 
#include <stdio.h>
#include <fcntl.h>
#include <string.h>    //strlen
#include <stdlib.h>    //strlen
#include <sys/socket.h>  // sockets
#include <sys/un.h>	// unix sockets
#include <arpa/inet.h> //inet_addr
#include <unistd.h>    //write
#include <pthread.h> //for threading , link with lpthread
#include <time.h>

#define SOCK_PATH "/var/run/tryserver.sock"

// ========= for the server of the tryclients ==========
// the thread function to handle input listener loop
void *input_handler(void *);

// the thread function to handle a received connection
void *coninput_handler(void *);

// ========= for the server of the trycorders ==========
// the thread function to handle trycorder listener loop
void *listener_handler(void *);

// the thread function to handle a received connection
void *connection_handler(void *);

// === functions to interact with list of connected trycorders ===
void sendall(char *);
void listconnclient();
void publishlist();

// function to write stats to database
int sqlwrite(char *, char *);


// the speak mode flag
static int speakmode=0;
// the mirror mode flag
static int mirrormode=1;

// the log-file pointer
static FILE *fplog=NULL;
// the machine log file pointer
static FILE *fpmach=NULL;

// the local unix socket to talk with the user
static int fdsock=0;

// function to talk to the tryclient user and log it all
void say(const char *s) {
  int res;
  printf("%s",s);
  if(fplog!=NULL) fprintf(fplog,"%s",s);
  if(fdsock!=0) res=write(fdsock,s,strlen(s));
  fflush(stdout);
  fflush(fplog);
}

void demomode() {
    sendall("sensors\n");
    sleep(2);
    sendall("computer magnetic\n");
    sleep(3);
    sendall("computer orientation\n");
    sleep(3);
    sendall("computer gravity\n");
    sleep(3);
    sendall("computer temperature\n");
    sleep(3);
    sendall("computer sensor off\n");
    sleep(2);
    sendall("communications\n");
    sleep(2);
    sendall("computer hailing\n");
    sleep(3);
    sendall("computer hailing close\n");
    sleep(2);
    sendall("computer intercomm\n");
    sleep(4);
    sendall("computer chatcomm\n");
    sleep(4);
    sendall("shields\n");
    sleep(2);
    sendall("computer raise shield\n");
    sleep(2);
    sendall("computer lower shield\n");
    sleep(2);
    sendall("fire capabilities\n");
    sleep(2);
    sendall("computer phaser\n");
    sleep(4);
    sendall("computer fire\n");
    sleep(3);
    sendall("yellow alert\n");
    sleep(2);
    sendall("transporter\n");
    sleep(2);
    sendall("computer beam me down\n");
    sleep(4);
    sendall("computer beam me up\n");
    sleep(4);
    sendall("tractor beam\n");
    sleep(2);
    sendall("computer tractor push\n");
    sleep(3);
    sendall("computer tractor pull\n");
    sleep(3);
    sendall("computer tractor off\n");
    sleep(2);
    sendall("propulsors\n");
    sleep(2);
    sendall("computer impulse power\n");
    sleep(3);
    sendall("computer warp drive\n");
    sleep(4);
    sendall("computer stay here\n");
    sleep(2);
    sendall("camera\n");
    sleep(2);
    sendall("computer local viewer\n");
    sleep(4);
    sendall("computer snap photo\n");
    sleep(4);
    sendall("computer logs console\n");
    sendall("computer intercomm\n");
    sendall("finishing demo\n");
}

void * demo_handler( void *dum) {
    demomode();
}



// ====================== MAIN ==============================

int main(int argc , char *argv[])
{
    // the inputline
    char inputline[256]="";

    // the log file for all tryserver actions
    fplog=fopen("/var/log/tryserver.log","a");
    // the log file for all tryserver machines
    fpmach=fopen("/var/log/trycorder.log","a");
    
    // the thread that will handle connections from tryclient
    pthread_t input_thread;
    
    if( pthread_create( &input_thread , NULL ,  input_handler , (void *)"Tryclient" ) < 0) {
            say("could not create input thread\n");
            return (1);
    }

    // the thread that will handle connections from trycorders
    pthread_t listener_thread;
    
    if( pthread_create( &listener_thread , NULL ,  listener_handler , (void *)"Tryserver" ) < 0) {
            say("could not create listener thread\n");
            return (1);
    }
    
    //Now join the thread , so that we dont terminate before the thread
    //pthread_join( listener_thread , NULL);
    //say("Handler assigned\n");

    // the main thread loop to input commands from command-line 
    // and ask to send it to all connected sockets
    while(1) {
      char *s=fgets(inputline,200,stdin);
      if(s!=NULL) {
	if(strncmp(inputline,"list",4) ==0) {
	  listconnclient();
	} else if(strncmp(inputline,"demo",4) ==0) {
	  demomode();
	} else if(strncmp(inputline,"speak",5) ==0) {
	  speakmode=1;
	} else if(strncmp(inputline,"nospeak",7) ==0) {
	  speakmode=0;
	} else if(strncmp(inputline,"mirror",6) ==0) {
	  mirrormode=1;
	} else if(strncmp(inputline,"fullmirror",10) ==0) {
	  mirrormode=2;
	} else if(strncmp(inputline,"nomirror",8) ==0) {
	  mirrormode=0;
	} else if(strncmp(inputline,"quit",4) ==0) {
	  break;
	} else {
	  // send this command to all trycorders
	  sendall(inputline);
	}
      }
    }
    
    
    fclose(fplog);
    fclose(fpmach);
 
    return(0);
}

// ===================================== connection list management =====================================

// data block passed to the connection thread
// with all data to handle a client connection
struct ConnClient {
  struct sockaddr_in client;
  int clientlen;
  int client_sock;
  char ipaddr[64];
  char tryname[64];
  char androidver[64];
};

static struct ConnClient *connclient[256];
static int nbconnclient=0;

void addconnclient(struct ConnClient *conn) {
  if(nbconnclient>255) return;
  connclient[nbconnclient]=conn;
  nbconnclient++;
  // tell all trycorders the list changed
  publishlist();
}

void delconnclient(struct ConnClient *conn) {
  if(nbconnclient<=0) return;
  int i;
  for(i=0;i<nbconnclient;++i) {
    if(conn==connclient[i]) {
      int j;
      for(j=i;j<(nbconnclient-1);++j) {
	connclient[j]=connclient[j+1];
      }
      nbconnclient--;
      // tell all trycorders the list changed
      publishlist();
      break;
    }
  }
}

void listconnclient() {
  if(nbconnclient<=0) return;
  int i;
  char buf[256];
  for (i=0;i<nbconnclient;++i) {
    sprintf(buf,"Client:%s:%s:Sock:%d\n",connclient[i]->ipaddr,connclient[i]->tryname,connclient[i]->client_sock);
    say(buf);
  }
}

// this will handle the mechanic to send this master message 
// to all trycorders connected
void sendall(char *line) {
  if(nbconnclient<=0) return;
  int i;
  char buf[256];
  for (i=0;i<nbconnclient;++i) {
    int sock=connclient[i]->client_sock;
    int res=write(sock , line , strlen(line));
    sprintf(buf,"Send:%s:%s\n",connclient[i]->ipaddr,line);
    say(buf);
  }
  return;
}

// publish the list of trycorders connected to all clients
void publishlist() {
  if(nbconnclient<=0) return;
  char buf[2048];
  char mach[64];
  int i;
  strcpy(buf,"trycorders:");
  for(i=0;i<nbconnclient;++i) {
    sprintf(mach,"%s,%s:",connclient[i]->ipaddr,connclient[i]->tryname);
    strcat(buf,mach);
  }
  strcat(buf,"\n");
  sendall(buf);
}

// ===================================== TRYCLIENT server part =====================================


/*
 * This will wait for connection from each tryclient
 * */

void* input_handler(void* threadname)
{

    int socket_desc ;
    struct sockaddr_un server ;
    char buf[256];
    
    //Create socket
    socket_desc = socket(AF_UNIX , SOCK_STREAM , 0);
    if (socket_desc == -1)
    {
        say("Could not create socket\n");
        exit(1);
    }
    sprintf(buf,"Input Socket created: %d\n",socket_desc);
    say(buf);
     
    //Prepare the sockaddr_in structure
    server.sun_family = AF_UNIX;
    strcpy(server.sun_path,SOCK_PATH);
    unlink(server.sun_path);
    
    //Bind
    if( bind(socket_desc,(struct sockaddr *)&server , sizeof(server)) < 0)
    {
        //print the error message
        say("bind failed. Error\n");
        exit(1);
    }
    //puts("bind done\n");
     
    //Listen
    listen(socket_desc , 3);
     
    //Accept and incoming connection
    say("Waiting for incoming connections...\n");
    
    // loop to accept incoming client connections
    
    struct ConnClient *conn;
    
    while(1) {
	fdsock=0;	// make sure socket 
	conn=(struct ConnClient *) malloc(sizeof(struct ConnClient));
	conn->clientlen=sizeof(struct sockaddr_un);
	
	conn->client_sock = accept(socket_desc, (struct sockaddr *)&(conn->client), (socklen_t*)&(conn->clientlen));
	if(conn->client_sock<=0) break;
	
        sprintf(buf,"\nInput accepted: Sock-%d\n",conn->client_sock);
	say(buf);
         
	fdsock=conn->client_sock;
	
	int read_size;
	char client_message[2000];
	
	// Receive a message from client, 
	// and continue to wait for more until the client close the connection
	while( (read_size = recv(fdsock , client_message , 2000 , 0)) > 0 )
	{
	    int res;
	    client_message[read_size]=0;
	    if(strncmp(client_message,"list",4) ==0) {
	      listconnclient();
	    } else if(strncmp(client_message,"demo",4) ==0) {
	      demomode();
	    } else if(strncmp(client_message,"speak",5) ==0) {
	      speakmode=1;
	    } else if(strncmp(client_message,"nospeak",7) ==0) {
	      speakmode=0;
	    } else if(strncmp(client_message,"mirror",6) ==0) {
	      mirrormode=1;
	    } else if(strncmp(client_message,"fullmirror",10) ==0) {
	      mirrormode=2;
	    } else if(strncmp(client_message,"nomirror",8) ==0) {
	      mirrormode=0;
	    } else if(strncmp(client_message,"quit",4) ==0) {
	      close(fdsock);
	      close(socket_desc);
	      exit(0);
	    } else {
	      // send this command to all trycorders
	      sendall(client_message);
	    }
	    
	}
	close(fdsock);
	free(conn);
	fdsock=0;
	
    }
    
    fdsock=0;
    
    close(socket_desc);
    
    return (NULL);
}


// ===================================== TRYCORDER server part =====================================

/*
 * This will wait for connection from each client
 * */

void* listener_handler(void* threadname)
{

    int socket_desc ;
    struct sockaddr_in server ;
    char buf[256];
    
    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    if (socket_desc == -1)
    {
        say("Could not create socket\n");
        exit(1);
    }
    sprintf(buf,"Listener Socket created: %d\n",socket_desc);
    say(buf);
     
    //Prepare the sockaddr_in structure
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons( 1701 );
     
    //Bind
    if( bind(socket_desc,(struct sockaddr *)&server , sizeof(server)) < 0)
    {
        //print the error message
        say("bind failed. Error\n");
        exit(1);
    }
    //puts("bind done\n");
     
    //Listen
    listen(socket_desc , 3);
     
    //Accept and incoming connection
    say("Waiting for incoming connections...\n");
    
    // loop to accept incoming client connections
    
    struct ConnClient *conn;
    
    while(1) {
	conn=(struct ConnClient *) malloc(sizeof(struct ConnClient));
	conn->clientlen=sizeof(struct sockaddr_in);
	
	conn->client_sock = accept(socket_desc, (struct sockaddr *)&(conn->client), (socklen_t*)&(conn->clientlen));
	if(conn->client_sock<=0) break;
	
        sprintf(buf,"\nConnection accepted: Sock-%d\n",conn->client_sock);
	say(buf);
	// extract the ip-addr from the connection
	sprintf(conn->ipaddr,"%d.%d.%d.%d",
            (int)(conn->client.sin_addr.s_addr&0xFF),
            (int)((conn->client.sin_addr.s_addr&0xFF00)>>8),
            (int)((conn->client.sin_addr.s_addr&0xFF0000)>>16),
            (int)((conn->client.sin_addr.s_addr&0xFF000000)>>24));
         
        pthread_t connection_thread;
         
        if( pthread_create( &connection_thread , NULL ,  connection_handler , (void*) conn) < 0)
        {
            say("could not create thread\n");
            return (NULL);
        }
         
    }
     
    if (conn->client_sock < 0)
    {
        say("accept failed\n");
        return (NULL);
    }
     
    return (NULL);
}

/*
 * This will handle connection for each client
 * */
void *connection_handler(void *connvoid)
{
    //Get the socket descriptor
    struct ConnClient *conn = (struct ConnClient *)connvoid;
    int sock = conn->client_sock;
    int read_size;
    char client_message[2000];
    char runcommand[1024];
    char server_response[]="server ok\n";
    char server_response2[]="You are connected\n";
    char buf[256];
    
    // log connection from
    time_t now=time(NULL);
    sprintf(buf,"%s",ctime(&now));
    say(buf);
    sprintf(buf,"From:%s\n",conn->ipaddr);
    say(buf);
    
    // Receive a message from client, 
    // and continue to wait for more until the client close the connection
    while( (read_size = recv(sock , client_message , 2000 , 0)) > 0 )
    {
	int res;

	client_message[read_size]=0;
	// remove the last '\n' from the message
	if(client_message[read_size-1]=='\n') client_message[read_size-1]=0;

	// display the received message
        sprintf(buf,"RECV:Sock-%d:%s\n",sock,client_message);
	say(buf);
	
	//Send the response back to client
	if(strncmp(client_message,"trycorder:",10)==0) {
        	// this is a permanent link with the client
		res=write(sock , "READY\n" , 6);
        	res=write(sock , server_response2 , strlen(server_response2));
        	sprintf(buf,"Responded:%s",server_response2);
        	say(buf);
		// save the client name
		strcpy(conn->tryname,client_message+10);
		// add this thread/connection to the list of permanent threads
		addconnclient(conn);
		// log the machine name in machine log
		now=time(NULL);
		fprintf(fpmach,"%s",ctime(&now));
		fprintf(fpmach,"%s:%s\n",conn->ipaddr,conn->tryname);
		sqlwrite(conn->ipaddr,conn->tryname);
		fflush(fpmach);
	} else {
		// this is a temporary, one-shot message from the client
                res=write(sock , server_response , strlen(server_response));
        	sprintf(buf,"Responded:%s",server_response);
        	say(buf);
		if(strncmp(client_message,"demo",4)==0) {
		  pthread_t demo_thread;
		  if( pthread_create( &demo_thread , NULL ,  demo_handler , (void*) NULL) < 0) {
		    say("could not create thread\n");
		  }
		} else {
		  // send command back to all clients except the sender
		  if(nbconnclient>0) {
		    strcat(client_message,"\n");
		    int i;
		    for (i=0;i<nbconnclient;++i) {
		      // do not mirror to the sender
		      if(strcmp(connclient[i]->ipaddr,conn->ipaddr)==0) continue; 
		      // when it is not a command, we always mirror
		      if(strncmp(client_message,"computer",8) ==0) {
			// do not mirror when mode == 0
			if(mirrormode==0) continue;	
			// do not mirror to others than local if mirrormode==1
			if(strncmp(connclient[i]->ipaddr,"192.168.",8)!=0 && mirrormode==1) continue;
			// mirrormode==2 or client is local then mirror is on
		      }
		      int sockm=connclient[i]->client_sock;
		      int res=write(sockm , client_message , strlen(client_message));
		      sprintf(buf,"Mirror:%s:%s",connclient[i]->ipaddr,client_message);
		      say(buf);
		    }
		  }
		}
        }
	
	// remove the last '\n' from the message
	if(client_message[read_size-1]=='\n') client_message[read_size-1]=0;
	// speak the message
	sprintf(runcommand,"espeak \"%s\" &",client_message);
	if(speakmode!=0) res=system(runcommand);

	
    }
     
    if(read_size == 0)
    {
        //puts("Client disconnected\n");
    }
    else if(read_size == -1)
    {
        say("recv failed\n");
    }
    
    // delete our structure from the list,
    // and free the structure 
    delconnclient(conn);
    
    //Free the socket pointer
    sprintf(buf,"Closed Socket: %s:Sock-%d\n",conn->ipaddr,sock);
    say(buf);

    free(conn);

    return 0;
}

static char buf[2048];

int sqlwrite(char *ipaddr, char *tryname) {
      return(0);
}
