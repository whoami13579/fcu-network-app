#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

int main() {
    struct sockaddr_in server;
    int sock, readSize;
    int client_id;
    char room_id;
    char sendBuf[256];
    char readBuf[256];
    char roomBuf[256];
    pid_t pid;

    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_STREAM, 0);

    connect(sock, (struct sockaddr*) &server, sizeof(server));
    printf("Choose a room to enter.(A, B or C): ");
    scanf("%27[^\n]%*c", roomBuf);
    write(sock, roomBuf, sizeof(roomBuf));

    sprintf(sendBuf, "New user joined.");
    write(sock, sendBuf, sizeof(sendBuf));

    pid = fork();

    while(1) {
        if(-1 == pid) {
            perror("Fork");
            break;
        }
        else if(0 == pid) {
            read(sock, readBuf, sizeof(readBuf));
            printf("RSock ID: %d\n", sock);
            printf("Read Message: %s\n\n", readBuf);

            if(!strcmp("Server is full!", readBuf)) {
                sprintf(sendBuf, "EXIT!");
                write(sock, sendBuf, sizeof(sendBuf));
                kill(pid, SIGKILL);
                break;
            }
        }
        else if(0 < pid) {
            if(scanf("%27[^\n]%*c", sendBuf) != EOF) {
                write(sock, sendBuf, sizeof(sendBuf));

                if(!strcmp("EXIT!", sendBuf)) {
                    kill(pid, SIGKILL);
                    break;
                }
            }
            else {
                printf("EOF!!\n");
                kill(pid, SIGKILL);
                break;
            }
        }
    }

    close(sock);
    printf("Client close");
    return 0;
}