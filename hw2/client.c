#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/select.h>


int main() {
    struct sockaddr_in server;
    int sock, readSize, addressSize;

    int retval;
    char str1[20], str2[20], result[50];

    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_DGRAM, 0);

    addressSize = sizeof(server);

    while(1) {
        struct timeval tv;
        fd_set rfds;

        FD_ZERO(&rfds);
        FD_SET(0, &rfds);

        tv.tv_sec = 5;
        tv.tv_usec = 0;

        if(EOF == scanf("%s", str1)) {
            str1[0] = -1;
            str1[1] = '\0';
            sendto(sock, str1, sizeof(str1), 0, (struct sockaddr*)&server, sizeof(server));
            break;
        }

        sendto(sock, str1, sizeof(str1), 0, (struct sockaddr*)&server, sizeof(server));
        

        retval = select(1, &rfds, NULL, NULL, &tv);

        if(retval == -1) {
            perror("select error!");
        }
        else if(retval) {
            scanf("%s", str2);
            sendto(sock, str2, sizeof(str2), 0, (struct sockaddr*)&server, sizeof(server));
        }
        else {
            str2[0] = NULL;
            str2[1] = '\0';
            sendto(sock, str2, sizeof(str2), 0, (struct sockaddr*)&server, sizeof(server));
        }

        readSize = recvfrom(sock, result, sizeof(result), 0, (struct sockaddr*)&server, &addressSize);
        printf("Read Message: %s\n", result);
    }

    close(sock);
    return 0;
}