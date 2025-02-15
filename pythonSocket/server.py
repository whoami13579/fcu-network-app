import socket
import threading
import translators
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db


HOST = "127.0.0.1"  # Standard loopback interface address (localhost)
PORT = 65432  # Port to listen on (non-privileged ports are > 1023)

cred = credentials.Certificate('confidential/firebase_key.json')
app = firebase_admin.initialize_app(cred,{"databaseURL" : "https://androidapp-ebde1-default-rtdb.firebaseio.com/"})

clients = []

def new_user(email, password)->str:
    ref = db.reference("/users")
    if bool(ref.order_by_child("email").equal_to(email).get()):
        return "email is already in use"

    users: dict | object = ref.order_by_child("id").limit_to_last(1).get()
    if users is None:
        ref.push().set({"id": 0, "email": email, "password": password, "language" : "en"})
        return "successful"

    users = list(users.items())
    ref.push().set({"id": users[0][1]["id"] + 1, "email": email, "password": password, "language" : "en"})
    return "successful"


def login(email, password)->str:
    ref = db.reference("/users")
    user: dict | object  = ref.order_by_child("email").equal_to(email).get()
    user = list(user.items())
    if len(user) == 0:
        return "user not exists"

    if user[0][1]["password"] != password:
        return "password is wrong"

    return "successful"


def change_language(email, language) -> str:
    ref = db.reference("/users")
    user: dict | object = ref.order_by_child("email").equal_to(email).get()
    user = list(user.items())
    print(user)
    ref.child(user[0][0]).update({"language" : language})
    return f"change language to {language}"


def send_message(from_email, to_email, message) -> str:
    ref = db.reference("/users")
    user: dict | object = ref.order_by_child("email").equal_to(to_email).get()
    user = list(user.items())
    language = user[0][1]["language"]

    ref = db.reference("/messages")
    message = " ".join(message)

    translation = translators.translate_text(message, to_language=language)
    message = f"from_{from_email} " + message + f" {language} " + translation

    ref.push().set({"from" : from_email, "to" : to_email, "message" : message})
    return "successful"


def load_message(email1, email2) -> str:
    ref = db.reference("/messages")
    messages:dict | object = ref.get()
    result = ""

    for key, message in messages.items():
        if (message["from"] == email1 and message["to"] == email2) or (message["from"] == email2 and message["to"] == email1):
            result += message["message"]
            result += "\n"
    return result


def add_friend(email1, email2) -> str:
    ref = db.reference("/friends")
    friends: dict | object = ref.get()
    found = False

    if not bool(friends):
        ref.push().set({"user1" : email1, "user2" : email2})
    else:
        for key, value in friends.items():
            if (value["user1"] == email1 and value["user2"] == email2) or (value["user1"] == email2 and value["user2"] == email1):
                found = True

        if not found:
            ref.push().set({"user1" : email1, "user2" : email2})

    return "successful"


def get_friends(email) -> str:
    result = []
    ref = db.reference("/friends")
    friends: dict | object = ref.order_by_child("user1").equal_to(email).get()
    if bool(friends):
        for key, value in friends.items():
            result.append(value["user2"])

    friends: dict | object = ref.order_by_child("user2").equal_to(email).get()
    if bool(friends):
        for key, value in friends.items():
            result.append(value["user1"])

    return " ".join(result)


def connect(conn, addr):
    with conn:
        print(f"Connected by {addr}")
        while True:
            data = conn.recv(1024)
            text:str = data.decode('utf-8')

            arr = text.split()

            if arr[0] == "new_user":
                conn.send(new_user(arr[1], arr[2]).encode('utf-8'))
                continue

            if arr[0] == "login":
                conn.send(login(arr[1], arr[2]).encode('utf-8'))
                if [conn, None] in clients:
                    position = clients.index([conn, None])
                    clients[position][1] = arr[1]
                continue

            if arr[0] == "language":
                conn.send(change_language(arr[1], arr[2]).encode('utf-8'))
                continue

            if arr[0] == "send_message":
                conn.send(send_message(arr[1], arr[2], arr[3:]).encode('utf-8'))
                continue

            if arr[0] == "load_message":
                conn.send(load_message(arr[1], arr[2]).encode('utf-8'))
                continue

            if arr[0] == "add_friend":
                conn.send(add_friend(arr[1], arr[2]).encode('utf-8'))
                continue

            if arr[0] == "get_friends":
                conn.send(get_friends(arr[1]).encode('utf-8'))
                continue

            if text == "exit":
                position = 0
                for client, name in clients:
                    if client == conn:
                        break
                    position += 1
                # position = clients.index(conn)
                del clients[position]
                conn.send(data)
                print(f"{addr} disconnected")
                conn.close()
                return

            text = translators.translate_text(text, to_language='zh-Hant')
            # text = translators.translate_text(text, to_language='en')
            # text = translators.translate_text(text, to_language='ja')
            # text = text.encode('ascii')
            # print(f"Receive {data}")
            for client, name in clients:
                if conn != client:
                    client.send(data)
                    client.send(text.encode('utf-8'))


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    while True:
        last = len(clients)
        s.listen()
        conn, addr = s.accept()
        clients.append([conn, None])
        t = threading.Thread(target=connect, args=(conn, addr, ))
        t.start()


"""

new_user chu@gmail.com 111111
new_user user@gmail.com 111111
new_user user2@gmail.com 111111

login chu@gmail.com 111111
login user@gmail.com 111111
login user2@gmail.com 111111
login chu@gmail.com 222222
login user@gmail.com 222222

language chu@gmail.com ja
language user@gmail.com ja
language user2@gmail.com ja
language chu@gmail.com en
language user@gmail.com en
language user2@gmail.com en
language chu@gmail.com zh-Hant
language user@gmail.com zh-Hant
language user2@gmail.com zh-Hant

send_message chu@gmail.com user@gmail.com hello
send_message chu@gmail.com user@gmail.com my name is chu
send_message chu@gmail.com user@gmail.com how are you
send_message user@gmail.com chu@gmail.com こんにちは
send_message user@gmail.com chu@gmail.com 私の名前はチューです
send_message user@gmail.com chu@gmail.com お元気ですか

load_message chu@gmail.com user@gmail.com
load_message user@gmail.com chu@gmail.com
load_message chu@gmail.com user2@gmail.com

add_friend chu@gmail.com user@gmail.com
add_friend user@gmail.com chu@gmail.com
add_friend chu@gmail.com user2@gmail.com

get_friends chu@gmail.com
get_friends user@gmail.com
get_friends user2@gmail.com

"""
