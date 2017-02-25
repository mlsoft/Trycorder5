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

#include <sqlite3.h>

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
extern int sqlwrite(char *, char *);


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
  char mach[128];
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

extern int getstattrycorders();
extern int getstatcountrys();
extern int getstatcitys();
extern int getstatstates();

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
		fflush(fpmach);
		sqlwrite(conn->ipaddr,conn->tryname);
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
		} else if(strncmp(client_message,"stats",5)==0) {
		  // send statistics to same client
		  int a=getstattrycorders();
		  int b=getstatcountrys();
		  int c=getstatcitys();
		  int d=getstatstates();
		  sprintf(buf,"statistics:%d,%d,%d,%d\n",a,b,c,d);
		  res=write(sock,buf,strlen(buf));
		  sprintf(buf,"Sent Statistics: %d,%d,%d,%d\n",a,b,c,d);
		  say(buf);
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

// ========================================================================================================


void transform_name(const char *fromname, char *toname, char *toaver, char *totver) {
char *s;
char *p;
int i;
int addr[4];
char buf[128];
    strcpy(toname,fromname);
    toaver[0]=0;
    totver[0]=0;
    strcpy(buf,fromname);
    s=buf;
    // name part
    p=strchr(s,'/');
    if(p==NULL) return;
    *p=0;
    strcpy(toname,s);
    s=p+1;
    // android version part
    p=strchr(s,'/');
    if(p==NULL) return;
    *p=0;
    strcpy(toaver,s);
    s=p+1;
    // trycorder version part
    strcpy(totver,s);
    return;
}

void transform_addr(const char *fromaddr, char *toaddr) {
char *s;
char *p;
int i;
int addr[4];
char buf[64];
    strcpy(toaddr,fromaddr);
    strcpy(buf,fromaddr);
    s=buf;
    // first number
    p=strchr(s,'.');
    if(p==NULL) return;
    *p=0;
    addr[0]=atoi(s);
    s=p+1;
    // second number
    p=strchr(s,'.');
    if(p==NULL) return;
    *p=0;
    addr[1]=atoi(s);
    s=p+1;
    // third number
    p=strchr(s,'.');
    if(p==NULL) return;
    *p=0;
    addr[2]=atoi(s);
    s=p+1;
    // fourth number
    addr[3]=atoi(s);
    // convert to 12 digit addr
    sprintf(toaddr,"%03d%03d%03d%03d",addr[0],addr[1],addr[2],addr[3]);
    return;
}

static char tryserverfile[]="/home/bin/tryserver.sqlite";
static char updatebuf[2048];
static char insertbuf[2048];
static char selectbuf[2048];
static sqlite3 *db;

static char country[32];
static char state[128];
static char city[128];
static char selcountry[32];

int find_callback(void *notused,int nbf, char **fields, char **names) {
    if(nbf<1) return(-1);
    strcpy(selcountry,fields[0]);
    return(0);
}

int select_callback(void *notused,int nbf, char **fields, char **names) {
    if(nbf<3) return(-1);
    strcpy(country,fields[0]);
    strcpy(state,fields[1]);
    strcpy(city,fields[2]);
    return(0);
}


int findcity(char *ipaddr, char *name) {
char *errmsg=0;
int res;

    country[0]=0;
    state[0]=0;
    city[0]=0;

    selcountry[0]=0;
    
    // check if country already setup for this IP/Name
    sprintf(selectbuf,"SELECT country from trycorder where localaddr='%s' and name='%s';",ipaddr,name);
    
    res=sqlite3_exec(db,selectbuf,find_callback,0,&errmsg);

    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    }
    // if the country is already setup then dont reread from database
    if(selcountry[0]!=0) {
      return(-1);
    }
    
    // find the country and rest from dbipcity database
    sprintf(selectbuf,"SELECT country,state,city FROM dbipcity WHERE '%s'>=fromaddr and '%s'<=toaddr;",ipaddr,ipaddr);
  
    res=sqlite3_exec(db,selectbuf,select_callback,0,&errmsg);

    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    }
    
    return(0);
}

int update_callback(void *notused,int nbf, char **fields, char **names) {
int i;

    for(i=0;i<nbf;++i) {
	printf("%s ",fields[i]);
    }
    printf("\n");
  
    return(0);
}

int sqlwrite(char *ipaddr, char *tryname) {
char newaddr[64];
char newname[64];
char newandver[32];
char newtryver[32];
char *errmsg=0;
int res;

    transform_addr(ipaddr,newaddr);
    transform_name(tryname,newname,newandver,newtryver);
    
    // try to open the database
    res=sqlite3_open(tryserverfile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    int exi=findcity(ipaddr,newname);
    
    // if the record already exist then just update it
    if(exi==(-1)) {
      // update the database
      sprintf(updatebuf,"UPDATE trycorder SET android='%s',tryversion='%s',connection=connection+1 WHERE ipaddr='%s' and name='%s';",newandver,newtryver,newaddr,newname);
      // the exec way
      res=sqlite3_exec(db,updatebuf,update_callback,0,&errmsg);
      if( res!=SQLITE_OK ){
	fprintf(stdout, "SQL UPDATE error: %s\n", errmsg);
	sqlite3_free(errmsg);
      } else {
	printf("Update OK\n");
      }
    } else {	// if not, then insert in table
      // insert in database
      sprintf(insertbuf,"INSERT INTO trycorder VALUES ('%s','%s','%s','%s','%s','1','%s','%s','%s');",newaddr,newname,newandver,newtryver,ipaddr,country,city,state);
      // the exec way
      res=sqlite3_exec(db,insertbuf,update_callback,0,&errmsg);
      if( res!=SQLITE_OK ){
	fprintf(stdout, "SQL INSERT error: %s\n", errmsg);
	sqlite3_free(errmsg);
      } else {
	printf("Insert OK\n");
      }
    }
    
    sqlite3_close(db);
  
    return(0);
}

// ===========================================================================================================

static char countvalue[32];
static int countloop=0;

static int nbtry=0;
static int nbcou=0;
static int nbcit=0;
static int nbsta=0;

int count_callback(void *notused,int nbf, char **fields, char **names) {
    countvalue[0]=0;
    if(nbf<1) return(-1);
    strcpy(countvalue,fields[0]);
    countloop++;
    return(0);
}


int getstattrycorders() {
char *errmsg=0;
int res;
    // try to open the database
    res=sqlite3_open(tryserverfile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }

    // return nb of trycorders()
    countloop=0;
    sprintf(selectbuf,"SELECT count() from trycorder;");
    res=sqlite3_exec(db,selectbuf,count_callback,0,&errmsg);
    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    } else {
      nbtry=atoi(countvalue);
    }
    
    // return nb of countrys
    countloop=0;
    sprintf(selectbuf,"SELECT count() from trycorder group by country;");
    res=sqlite3_exec(db,selectbuf,count_callback,0,&errmsg);
    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    } else {
      nbcou=countloop;
    }
    
    // return nb of citys
    countloop=0;
    sprintf(selectbuf,"SELECT count() from trycorder group by city;");
    res=sqlite3_exec(db,selectbuf,count_callback,0,&errmsg);
    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    } else {
      nbcit=countloop;
    }
    
    // return nb of states
    countloop=0;
    sprintf(selectbuf,"SELECT count() from trycorder group by state;");
    res=sqlite3_exec(db,selectbuf,count_callback,0,&errmsg);
    if( res!=SQLITE_OK ){
      fprintf(stdout, "SQL SELECT error: %s\n", errmsg);
      sqlite3_free(errmsg);
    } else {
      nbsta=countloop;
    }
    
    sqlite3_close(db);
    return(nbtry);
}

int getstatcountrys() {
    return(nbcou);
}

int getstatcitys() {
    return(nbcit);
}

int getstatstates() {
    return(nbsta);
}
