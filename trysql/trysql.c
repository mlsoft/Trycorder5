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

static char databasefile[]="/home/bin/tryserver.sqlite";

static sqlite3 *db;
static sqlite3_stmt *selectstmt;
static sqlite3_stmt *updatestmt;

extern int looprebuild();
extern int looprecity();
extern int looploadcity();
extern int loopsavecity();
extern int looploadlist();
extern int looptrycity();

int main(int argc , char *argv[])
{
int res;
    
    // check arguments
    if(argc<=1) {
	printf("Usage: trysql rebuild|recity|loadcity\n");
	return(0);
    }
    
    // select all trycorder entrys and update the from-and-to addr
    if(strcmp(argv[1],"rebuild")==0) {
      looprebuild();
    }
    
    // select all dbipcity entrys and update the from-and-to addr
    if(strcmp(argv[1],"recity")==0) {
      looprecity();
    }
    
    // select all dbipcity entrys and update the from-and-to addr
    if(strcmp(argv[1],"loadcity")==0) {
      looploadcity();
    }
    
    // select all dbipcity entrys and update the from-and-to addr
    if(strcmp(argv[1],"savecity")==0) {
      loopsavecity();
    }
    
    // select all dbipcity entrys and update the from-and-to addr
    if(strcmp(argv[1],"loadlist")==0) {
      looploadlist();
    }
    
    // select update the trycorder-table with city
    if(strcmp(argv[1],"trycity")==0) {
      looptrycity();
    }
    
    return(0);
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

int update_callback(void *notused,int nbf, char **fields, char **names) {
int i;

    for(i=0;i<nbf;++i) {
	printf("%s ",fields[i]);
    }
    printf("\n");
  
    return(0);
}

static char selectusers[]="select * from trycorder";

static char updateusers[2048];

int looprebuild() {
int res;
char newaddr[32];
char *errmsg = 0;

    // try to open the database
    res=sqlite3_open(databasefile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_prepare(db,selectusers,-1,&selectstmt,NULL);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_step(selectstmt);
    
    while(res==SQLITE_ROW) {
	int i;
	int nbf;
	
	// transform original address to newaddr
	transform_addr(sqlite3_column_text(selectstmt,0),newaddr);
	
	// print the original record and transformation
	printf("row=%s,%s,%s\n",sqlite3_column_text(selectstmt,0),sqlite3_column_text(selectstmt,1),newaddr);
	
	// if the address has been modified
	if(strcmp(sqlite3_column_text(selectstmt,0),newaddr)!=0) {
	  // update the database
	  sprintf(updateusers,"UPDATE trycorder SET ipaddr='%s' WHERE ipaddr='%s';",newaddr,sqlite3_column_text(selectstmt,0));

	  // the exec way
	  res=sqlite3_exec(db,updateusers,update_callback,0,&errmsg);
	  
	  if( res!=SQLITE_OK ){
	    fprintf(stdout, "SQL error: %s\n", errmsg);
	    sqlite3_free(errmsg);
	  }

	  // the prepare way
	  //res=sqlite3_prepare(db,updateusers,-1,&updatestmt,NULL);
	  //if(res!=SQLITE_OK) printf("cant prepare %s\n",updateusers);
	  //res=sqlite3_step(updatestmt);
	  //if(res!=SQLITE_ROW) printf("cant step %s\n",updateusers);
	  //sqlite3_finalize(updatestmt);
	}
	
	// loop to next record
	res=sqlite3_step(selectstmt);
    }

    sqlite3_close(db);
    
    return(0);
  
}

static char selectcity[]="select * from dbipcity";

static char updatecity[2048];

int looprecity() {
int res;
char newaddr[32];
char newaddr2[32];
char *errmsg = 0;

    // try to open the database
    res=sqlite3_open(databasefile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_prepare(db,selectcity,-1,&selectstmt,NULL);
    if(res!=SQLITE_OK) {
      printf("cant prepare database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_step(selectstmt);
    
    while(res==SQLITE_ROW) {
	int i;
	int nbf;
	
	char *p=strchr(sqlite3_column_text(selectstmt,0),'.');
	if(p==NULL) {
	  res=sqlite3_step(selectstmt);
	  continue;
	}
	
	// transform original address to newaddr
	transform_addr(sqlite3_column_text(selectstmt,0),newaddr);
	transform_addr(sqlite3_column_text(selectstmt,1),newaddr2);
	
	// print the original record and transformation
	printf("row=%s,%s,%s,%s\n",sqlite3_column_text(selectstmt,0),sqlite3_column_text(selectstmt,1),newaddr,newaddr2);
	//printf("row=%s\n",newaddr);

	// if the address has been modified
	if(strcmp(sqlite3_column_text(selectstmt,0),newaddr)!=0) {
	  // update the database
	  sprintf(updatecity,"UPDATE dbipcity SET fromip='%s',toip='%s' WHERE fromaddr='%s';",newaddr,newaddr2,sqlite3_column_text(selectstmt,0));

	  // the exec way
	  res=sqlite3_exec(db,updatecity,update_callback,0,&errmsg);
	  
	  if( res!=SQLITE_OK ){
	    fprintf(stdout, "SQL error: %s\n", errmsg);
	    sqlite3_free(errmsg);
	  }

	  // the prepare way
	  //res=sqlite3_prepare(db,updateusers,-1,&updatestmt,NULL);
	  //if(res!=SQLITE_OK) printf("cant prepare %s\n",updateusers);
	  //res=sqlite3_step(updatestmt);
	  //if(res!=SQLITE_ROW) printf("cant step %s\n",updateusers);
	  //sqlite3_finalize(updatestmt);
	}
	
	// loop to next record
	res=sqlite3_step(selectstmt);
    }

    sqlite3_close(db);
    
    return(0);
  
}


void strclean(char *to, const char *from) {
const char *s;
char *p;
    s=from;
    if(*s=='"') s++;
    strcpy(to,s);
    p=strchr(to,'"');
    if(p!=NULL) *p=0;
    p=strchr(to,'\n');
    if(p!=NULL) *p=0;
    int l=strlen(to);
    for(int i=0;i<l;++i) {
      if(to[i]=='\'') to[i]=' ';
      if(to[i]=='\\') to[i]=' ';
    }
    return;
}

static char insertcity[2048];
static char cityline[512];

static char filecity[]="dbip-city.csv";
static char databasecity[]="dbip-city.sqlite";

static char schemacity[]="create table dbipcity(fromip,toip,country,state,city,fromaddr,toaddr);";

static char fromip[64];
static char toip[64];
static char country[32];
static char state[64];
static char city[64];
static char fromaddr[64];
static char toaddr[64];

int looploadcity() {
int res;
char newaddr[32];
char newaddr2[32];
char *errmsg = 0;
char *s;
char *p;

    // try to open the database
    res=sqlite3_open(databasecity,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }

    // open the data file from dbip
    FILE *fp=fopen(filecity,"r");
    if(fp==NULL) return(-1);
    
    s=fgets(cityline,500,fp);
    
    while(s!=NULL) {
	int i;
	int nbf;

	s=cityline;
	// first field fromaddr
	p=strchr(s,',');
	*p=0;
	strclean(fromaddr,s);
	s=p+1;
	// second field toaddr
	p=strchr(s,',');
	*p=0;
	strclean(toaddr,s);
	s=p+1;
	// third field country
	p=strchr(s,',');
	*p=0;
	strclean(country,s);
	s=p+1;
	// fourth field state
	p=strchr(s,',');
	*p=0;
	strclean(state,s);
	s=p+1;
	// fifth field city
	strclean(city,s);
	
	// transform original address to newaddr
	transform_addr(fromaddr,fromip);
	transform_addr(toaddr,toip);
	
	// print the original record and transformation
	printf("row=%s,%s,%s,%s,%s\n",fromip,toip,country,state,city);
	
	// insert in database
	sprintf(insertcity,"INSERT INTO dbipcity VALUES ('%s','%s','%s','%s','%s','%s','%s');",
		fromip,toip,country,state,city,fromaddr,toaddr);

	// the exec way
	res=sqlite3_exec(db,insertcity,update_callback,0,&errmsg);
	if( res!=SQLITE_OK ){
	  fprintf(stdout, "SQL error: %s\n", errmsg);
	  sqlite3_free(errmsg);
	}

	// loop to next record
	s=fgets(cityline,500,fp);
    }

    fclose(fp);
    
    sqlite3_close(db);
    
    return(0);
  
}


int loopsavecity() {
int res;
char newaddr[32];
char newaddr2[32];
char *errmsg = 0;
char *s;
char *p;

    FILE *fpo=fopen("dbipcity.csv","w");

    // open the data file from dbip
    FILE *fp=fopen(filecity,"r");
    if(fp==NULL) return(-1);
    
    s=fgets(cityline,500,fp);
    
    while(s!=NULL) {
	int i;
	int nbf;

	s=cityline;
	// first field fromaddr
	p=strchr(s,',');
	*p=0;
	strclean(fromaddr,s);
	s=p+1;
	// second field toaddr
	p=strchr(s,',');
	*p=0;
	strclean(toaddr,s);
	s=p+1;
	// third field country
	p=strchr(s,',');
	*p=0;
	strclean(country,s);
	s=p+1;
	// fourth field state
	p=strchr(s,',');
	*p=0;
	strclean(state,s);
	s=p+1;
	// fifth field city
	strclean(city,s);
	
	// transform original address to newaddr
	transform_addr(fromaddr,fromip);
	transform_addr(toaddr,toip);
	
	// print the original record and transformation
	printf("row=%s,%s,%s,%s,%s\n",fromip,toip,country,state,city);

	fprintf(fpo,"%s,%s,%s,\"%s\",%s,%s,%s\n",fromip,toip,country,state,city,fromaddr,toaddr);
	
	// loop to next record
	s=fgets(cityline,500,fp);
    }

    fclose(fp);
    
    fclose(fpo);
    
    return(0);
  
}


static char insertlist[2048];
static char listline[512];

static char ipaddr[64];
static char name[128];
static char android[32];
static char tryversion[32];

int looploadlist() {
int res;
char newaddr[64];
char *errmsg = 0;
char *s;
char *p;

    // try to open the database
    res=sqlite3_open("/home/bin/tryserver.sqlite",&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }

    // open the data file from dbip
    FILE *fp=fopen("trycorder.list","r");
    if(fp==NULL) return(-1);
    
    s=fgets(listline,500,fp);
    
    while(s!=NULL) {
	if(listline[0]<'0' || listline[0]>'9') {
	  s=fgets(listline,500,fp);
	  continue;
	}

	int i;
	int nbf;
	ipaddr[0]=0;
	name[0]=0;
	android[0]=0;
	tryversion[0]=0;
	
	s=listline;
	p=strchr(s,'\n');
	if(p!=NULL) *p=0;
	// first field ipaddr
	p=strchr(s,':');
	if(p!=NULL) *p=0;
	strcpy(ipaddr,s);
	s=p+1;
	// second field name
	p=strchr(s,'/');
	if(p!=NULL) *p=0;
	strclean(name,s);
	if(p!=NULL) {
	  s=p+1;
	  // third field android
	  p=strchr(s,'/');
	  if(p!=NULL) *p=0;
	  strclean(android,s);
	  if(p!=NULL) {
	    s=p+1;
	    // fourth field tryversion
	    strclean(tryversion,s);
	  }
	}
	
	// transform original address to newaddr
	transform_addr(ipaddr,newaddr);
	
	// print the original record and transformation
	printf("row=%s,%s,%s,%s,%s\n",newaddr,name,android,tryversion,ipaddr);
	
	// insert in database
	sprintf(insertlist,"INSERT INTO trycorder VALUES ('%s','%s','%s','%s','%s','1','','');",
		newaddr,name,android,tryversion,ipaddr);

	// the exec way
	res=sqlite3_exec(db,insertlist,update_callback,0,&errmsg);
	if( res!=SQLITE_OK ){
	  fprintf(stdout, "SQL error: %s\n", errmsg);
	  sqlite3_free(errmsg);
	}

	// loop to next record
	s=fgets(listline,500,fp);
    }

    fclose(fp);
    
    sqlite3_close(db);
    
    return(0);
  
}

int looptrycity() {
int res;
char newaddr[32];
char newaddr2[32];
char *errmsg = 0;

    // try to open the database
    res=sqlite3_open(databasefile,&db);
    if(res!=SQLITE_OK) {
      printf("cant open database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_prepare(db,"SELECT ipaddr,localaddr,name,dbipcity.country,dbipcity.state,dbipcity.city,trycorder.country from trycorder,dbipcity where localaddr>=fromaddr and localaddr<=toaddr",-1,&selectstmt,NULL);
    if(res!=SQLITE_OK) {
      printf("cant prepare database: %s\n",sqlite3_errmsg(db));
      sqlite3_close(db);
      return(-1);
    }
    
    res=sqlite3_step(selectstmt);
    
    while(res==SQLITE_ROW) {
	int i;
	int nbf;

	//const char *p=sqlite3_column_text(selectstmt,6);	// trycorder.country
	//if(*p!=0) {
	//  res=sqlite3_step(selectstmt);
	//  continue;
	//}

	char city[128];
	strclean(city,sqlite3_column_text(selectstmt,5));
	
	char state[128];
	strclean(state,sqlite3_column_text(selectstmt,4));
	
	// print the original record and transformation
	printf("row=%s,%s,%s,%s,%s\n",sqlite3_column_text(selectstmt,0),sqlite3_column_text(selectstmt,2),sqlite3_column_text(selectstmt,3),city,state);
	
	// update the database
	sprintf(updatecity,"UPDATE trycorder SET country='%s',city='%s',state='%s' WHERE localaddr='%s' and name='%s';",sqlite3_column_text(selectstmt,3),city,state,sqlite3_column_text(selectstmt,1),sqlite3_column_text(selectstmt,2));

	// the exec way
	res=sqlite3_exec(db,updatecity,update_callback,0,&errmsg);
	  
	if( res!=SQLITE_OK ){
	  fprintf(stdout, "SQL error: %s\n", errmsg);
	  sqlite3_free(errmsg);
	}

	
	// loop to next record
	res=sqlite3_step(selectstmt);
    }

    sqlite3_close(db);
    
    return(0);
  
}
