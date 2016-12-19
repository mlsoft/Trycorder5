#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>    //write
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>

#include <pthread.h> //for threading , link with lpthread

#define SOCK_PATH "/var/run/tryserver.sock"

void *receive_handler(void *);

int main(int argc,char *argv[])
{
    int sock, len;
    struct sockaddr_un remote;
    char str[1000];

    if ((sock = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        exit(1);
    }

    printf("Trying to connect...\n");
    remote.sun_family = AF_UNIX;
    strcpy(remote.sun_path, SOCK_PATH);
    len = strlen(remote.sun_path) + sizeof(remote.sun_family);
    if (connect(sock, (struct sockaddr *)&remote, len) == -1) {
        perror("connect");
        exit(1);
    }
    printf("Connected.\n");

    // start a receive thread
    pthread_t receive_thread;
    if( pthread_create( &receive_thread , NULL ,  receive_handler , (void*) &sock) < 0) {
            printf("could not create thread\n");
            return (1);
    }
    
    char *s;
    while(s=fgets(str, 1000, stdin), !feof(stdin)) {
        if (send(sock, str, strlen(str), 0) == -1) {
            perror("send");
            exit(1);
        }
    }

    close(sock);

    return 0;
}


void *receive_handler(void *sockptr) {
    int sock=*(int *)sockptr;
    int len;
    char str[1024];
    
    while ((len=recv(sock, str, 1000, 0)) > 0) {
            str[len] = '\0';
            printf("%s", str);
    } 
    
    if (len < 0) perror("recv");
    else printf("Server closed connection\n");
    exit(0);
    return(NULL);
}
