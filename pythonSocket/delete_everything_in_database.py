import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

cred = credentials.Certificate('confidential/firebase_key.json')
app = firebase_admin.initialize_app(cred,{"databaseURL" : "https://androidapp-ebde1-default-rtdb.firebaseio.com/"})

ref = db.reference("/")
ref.set({})
