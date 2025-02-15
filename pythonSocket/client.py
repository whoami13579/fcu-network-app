import socket
import threading
from operator import truediv

# HOST = "127.0.0.1"  # The server's hostname or IP address
HOST = "192.168.9.137"  # The server's hostname or IP address
PORT = 65432  # The port used by the server

def receive(s):
    with s:
        while True:
            data = s.recv(1024).decode('utf-8')
            if data == "exit":
                break
            print("Received " + data)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    t = threading.Thread(target=receive, args=(s,))
    t.start()
    while True:
        message = input()
        if message == "exit":
            s.send(message.encode('ascii'))
            t.join()
            exit()
        s.send(message.encode('utf-8'))
