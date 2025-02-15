import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from firebase_admin import db
import bcrypt

cred = credentials.Certificate('confidential/firebase_key.json')

app = firebase_admin.initialize_app(cred,{"databaseURL" : "https://androidapp-ebde1-default-rtdb.firebaseio.com/"})

# ref = db.reference("/")
# ref.push().set({
#     "Books":
#     {
#         "Best_Sellers": -1
#     }
# })
# #
# ref = db.reference("/Books/Best_Sellers")
# import json
# with open("book_info.json", "r") as f:
#     file_contents = json.load(f)
#
# for key, value in file_contents.items():
#     ref.push().set(value)


# ref = db.reference("users/")
# best_sellers: dict | object = ref.order_by_child("email").equal_to("chu@gmail.com").get()
# print(type(best_sellers))
# print(best_sellers)
# tmp = list(best_sellers.items())
# print(tmp)
# print(tmp[0][0])
# print()
# ref.child(tmp[0][0]).update({"password" : 111111})

# user = ref.order_by_child("id").limit_to_last(1).get()
# # tmp = list(user.items())
# tmp = list(user)
# tmp = list(tmp)
# print((tmp[0][1]["id"]))
# print(type(tmp[0][1]["id"]))
# print(tmp[0][1]["id"])
# for key, value in best_sellers.items():
#     if value["Author"] == "J.R.R. Tolkien":
#         value["Price"] = 90
#         ref.child(key).update({"Price":80})


# result = []
# ref = db.reference("/friends")
# friends = ref.order_by_child("user1").equal_to("chu@gmail.com").get()
# for key, value in friends.items():
#         result.append(value["user2"])
#
# friends = ref.order_by_child("user2").equal_to("chu@gmail.com").get()
# for key, value in friends.items():
#     result.append(value["user1"])
#
# print(" ".join(result))


print(bcrypt.hashpw("123456".encode('utf-8'), bcrypt.gensalt()).decode('utf8'))
print(bcrypt.hashpw("123456".encode('utf-8'), bcrypt.gensalt()))
print(bcrypt.hashpw("123456".encode('utf-8'), bcrypt.gensalt()))

print(bcrypt.checkpw("123456".encode('utf-8'), "$2b$12$GrDtG.oT/vE60kHd3I6ry.w1b9Mt40Xs2XbhPhtXS9Nl9PJr.tsFi".encode('utf-8')))
print(bcrypt.checkpw("123456".encode('utf-8'), "$2b$12$wNYu84GvR6u3ksW9cTSr9O6m1Hnlx3N91mBd4vyxiKChnyqOvxXDS".encode('utf-8')))
print(bcrypt.checkpw("111111".encode('utf-8'), "$2b$12$GrDtG.oT/vE60kHd3I6ry.w1b9Mt40Xs2XbhPhtXS9Nl9PJr.tsFi".encode('utf-8')))
