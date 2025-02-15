#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>

#define MAX_CLIENT_NUMBER 5

void* connection_handler(void*);
int sockListA[100] = {0};
int sockListB[100] = {0};
int sockListC[100] = {0};
int workingA = 0;
int workingB = 0;
int workingC = 0;


int main() {
    struct sockaddr_in server, client;
    unsigned int sock, csock, addressSize;
    char buf[256];
    char room_id;
    pthread_t sniffer_thread;

    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_STREAM, 0);

    bind(sock, (struct sockaddr*)&server, sizeof(server));
    listen(sock, 5);
    addressSize = sizeof(client);
    while(1) {
        if((workingA + workingB + workingC) < MAX_CLIENT_NUMBER - 1) {
            if(!(csock = accept(sock, (struct sockaddr*)&server, &addressSize))) {
                break;
            }

            if(pthread_create(&sniffer_thread, 0, connection_handler, (void*)&csock) != 0) {
                perror("Thread creation");
            }
            else {
                pthread_detach(sniffer_thread);
            }

            if(csock < 0) {
                perror("Csock");
            }
        }
        else {
            csock = accept(sock, (struct sockaddr*)&server, &addressSize);
            sprintf(buf, "Server is full!");
            write(csock, &buf, sizeof(buf));
        }
    }

    close(sock);
    printf("[Socket Close]\n");
    return 0;
}

void* connection_handler(void* client) {
    int csock = *(int*)client;
    int readSize;
    int* working_ptr;
    int number;
    char buf[256];
    int* sockList_ptr;

    read(csock, buf, sizeof(buf));

    if(!strcmp("A", buf)) {
        working_ptr = &workingA;
        sockList_ptr = sockListA;
        workingA++;
    }
    else if(!strcmp("B", buf)) {
        working_ptr = &workingB;
        sockList_ptr = sockListB;
        workingB++;
    }
    else if(!strcmp("C", buf)){
        working_ptr = &workingC;
        sockList_ptr = sockListC;
        workingC++;
    }

    number = (*working_ptr) - 1;


    while((readSize = read(csock, buf, sizeof(buf))) != 0) {

        if(!strcmp("EXIT!", buf)) {
            break;
        }

        if(0 == sockList_ptr[number]) {
            sockList_ptr[number] = csock;
        }

        if(!strcmp("list", buf)) {
            sprintf(buf, "%d people in this room.\n", *working_ptr);
            write(csock, buf, sizeof(buf));
            continue;
        }

        // printf("CSock ID: %d\n", csock);
        // printf("Read Message: %s\n", buf);

        for(int i = 0; i < 100; i++) {
            if(sockList_ptr[i] != 0) {
                write(sockList_ptr[i], buf, sizeof(buf));
            }
        }

        printf("\n");
    }

    if(0 == readSize || !strcmp("EXIT!", buf)) {
        printf("Client Disconnect!\n");
        printf("CSock ID: %d\n", csock);

        int delete;
        (*working_ptr)--;

        for(int i = 0; i < 100; i++) {
            if(sockList_ptr[i] == csock) {
                delete = i;
            }
        }

        for(int i = delete; i < 100; i++) {
            sockList_ptr[i] = sockList_ptr[i + 1];
            sockList_ptr[99] = 0;
        }
    }

    close(csock);
    printf("Client Close\n\n");
    pthread_exit(0);
}