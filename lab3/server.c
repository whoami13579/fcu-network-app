#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>

void* connection_handler(void*);
int sockList[100] = {0};
int working = 0;

int main() {
    struct sockaddr_in server, client;
    unsigned int sock, csock, addressSize;
    char buf[256];
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
        if(working < 2) {
            if(!(csock = accept(sock, (struct sockaddr*)&server, &addressSize))) {
                break;
            }
            working++;
            printf("Online connect: %d\n", working);
            sprintf(buf, "連線順序: %d", working);
            write(csock, &buf, sizeof(buf));

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

void* connection_handler(void* sock) {
    int csock = *(int*)sock;
    int readSize;
    int number = working - 1;
    char buf[256];

    while((readSize = read(csock, buf, sizeof(buf))) != 0) {
        if(0 == sockList[number]) {
            sockList[number] = csock;
        }

        printf("CSock ID: %d\n", csock);
        printf("Read Message: %s\n", buf);

        for(int i = 0; i < 100; i++) {
            if(sockList[i] != 0) {
                write(sockList[i], buf, sizeof(buf));
            }
        }

        printf("\n");
    }

    if(0 == readSize) {
        printf("Client Disconnect!\n");
        printf("CSock ID: %d\n", csock);

        int delete;
        working--;

        for(int i = 0; i < 100; i++) {
            if(sockList[i] == csock) {
                delete = i;
            }
        }

        for(int i = delete; i < 100; i++) {
            sockList[i] = sockList[i + 1];
            sockList[99] = 0;
        }
    }

    close(csock);
    printf("Client Close\n\n");
    pthread_exit(0);
}