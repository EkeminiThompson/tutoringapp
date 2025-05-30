const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendMessageNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
      const message = snap.data();
      const receiverId = message.receiverId;
      const userDoc = await admin.firestore()
          .collection("users")
          .doc(receiverId)
          .get();
      const token = userDoc.data().fcmToken;

      if (token) {
        await admin.messaging().send({
          token,
          notification: {
            title: "New Message",
            body: message.content,
          },
        });
      }
    });
