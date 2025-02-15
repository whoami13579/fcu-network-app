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
    int num1, num2, ans;

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

        tv.tv_sec = 3;
        tv.tv_usec = 0;

        scanf("%d", &num1);
        sendto(sock, &num1, sizeof(num1), 0, (struct sockaddr*)&server, sizeof(server));

        retval = select(1, &rfds, NULL, NULL, &tv);

        if(retval == -1) {
            perror("select error!");
        }
        else if(retval) {
            scanf("%d", &num2);
            sendto(sock, &num2, sizeof(num2), 0, (struct sockaddr*)&server, sizeof(server));
        }
        else {
            num2 = 100;
            sendto(sock, &num2, sizeof(num2), 0, (struct sockaddr*)&server, sizeof(server));
        }

        readSize = recvfrom(sock, &ans, sizeof(ans), 0, (struct sockaddr*)&server, &addressSize);
        printf("Read Message: %d\n", ans);
    }
}