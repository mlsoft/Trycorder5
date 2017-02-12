/*
    Sqlite3 program to normalize entries in database
*/
 
#include <stdio.h>
#include <fcntl.h>
#include <string.h>    //strlen
#include <stdlib.h>    //strlen
#include <sys/socket.h>  // sockets
#include <sys/un.h>	// unix sockets
#include <arpa/inet.h> //inet_addr
#include <unistd.h>    //write
#include <time.h>


#include <sqlite3.h>


// ====================== MAIN ==============================

static char databasefile[]="tryserver.sqlite";

static char selectusers[]="select * from trycorder";

static char updateusers[2048];

static sqlite3 *db;
static sqlite3_stmt *selectstmt;
static sqlite3_stmt *updatestmt;

int main(int argc , char *argv[])
{
int res;
    
    // check arguments
    if(argc<=1) {
	printf("Usage: trysql rebuild\n");
	return(0);
    }
    
    // try to open the database
    res=sqlite3_open(databasefile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    // select all trycorder entrys
    
    
  
    sqlite3_close(db);
    
    return(0);
}

