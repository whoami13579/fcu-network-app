#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <ctype.h>

void homework(char* str) { // capitalize and shift the letters
    for(int i = 0; i < strlen(str); i++) {
        if(isalpha(str[i])) {
            str[i] = toupper(str[i]) + 1;

            if('Z'+ 1 == str[i]) {
                str[i] = 'A';
            }
        }
    }
}

int main() {
    struct sockaddr_in server;
    int sock, readSize;
    char buf[256];
    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_STREAM, 0);
    connect(sock, (struct sockaddr*)&server, sizeof(server));
    while(scanf("%[^\n]%*c", buf) != EOF) {
        homework(buf);
        send(sock, buf, sizeof(buf), 0); // send the result to the server

        readSize = recv(sock, buf, sizeof(buf), 0); // receive and print the result
        buf[readSize] = '\0';
        printf("%s\n", buf);
    }

    sprintf(buf, "%c", EOF); // send eof to the server
    send(sock, buf, sizeof(buf), 0);

    close(sock);
    return 0;
}