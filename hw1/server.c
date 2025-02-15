#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <ctype.h>

void homework(char* str) {
    char letters[256];
    int c = 0;
    int d = 0;
    int l = 0;
    for(int i = 0; i < strlen(str); i++) { // shift the letters, and count how many letters and digits are there in the string
        if(isalpha(str[i])) {
            l++;

            str[i] -= 1;

            if('A' - 1 == str[i]) {
                str[i] = 'Z';
            }

            letters[c++] = str[i];
        }
        else if(isdigit(str[i])) {
            d++;
        }
    }

    letters[c] = '\0';
    sprintf(str, "letters: %d, numbers: %d and %s", l, d, letters); // format the string
}

int main() {
    struct sockaddr_in server, client;
    int sock ,csock, readSize, addressSize;
    char buf[256];
    bzero(&server, sizeof(server));
    server.sin_family = PF_INET;
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_port = htons(5678);
    sock = socket(PF_INET, SOCK_STREAM, 0);
    bind(sock, (struct sockaddr*)&server, sizeof(server));
    listen(sock, 5);
    addressSize = sizeof(client);
    csock = accept(sock, (struct sockaddr*)&client, &addressSize);
    while(1) {
        readSize = recv(csock, buf, sizeof(buf), 0); //recive the string from client
        buf[readSize] = '\0';
        if(buf[0] == EOF) { // check eof
            puts("Client has closed the connection.");
            break;
        }

        homework(buf);
        send(csock, buf, sizeof(buf), 0); // send the result to the client
    }

    close(sock);
    return 0;
}