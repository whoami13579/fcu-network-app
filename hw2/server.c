#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <unistd.h>

int main() {
    struct sockaddr_in server, client;
    int sock, addressSize;
    char str1[20], str2[20], result[50];

    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_DGRAM, 0);

    bind(sock, (struct sockaddr*)&server, sizeof(server));

    addressSize = sizeof(client);

    while(1) {
        recvfrom(sock, str1, sizeof(str1), 0, (struct sockaddr*)&client, &addressSize);
        if(-1 == str1[0]) {
            break;
        }
        printf("Read Message[1]: %s\n", str1);

        recvfrom(sock, str2, sizeof(str2), 0, (struct sockaddr*)&client, &addressSize);
        if(str2[0]) {
            printf("Read Message[2]: %s\n", str2);
            sprintf(result, "%s, %s", str2, str1);
        }
        else {
            sprintf(result, "%s, string A = %ld", (strlen(str1) % 2) ? "odd" : "even", strlen(str1));
        }

        printf("Answer: %s\n", result);

        sendto(sock, result, sizeof(result), 0, (struct sockaddr*)&client, sizeof(client));
    }

    close(sock);
    return 0;
}