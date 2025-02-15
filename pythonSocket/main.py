import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from firebase_admin import db

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


ref = db.reference("/Books/Best_Sellers/")
best_sellers: dict | object = ref.get()
print(type(best_sellers))
for key, value in best_sellers.items():
    if value["Author"] == "J.R.R. Tolkien":
        value["Price"] = 90
        ref.child(key).update({"Price":80})
