/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

// Fungsi ini trigger bila ada document baru dalam collection 'notifications'
exports.sendNotificationOnCreate = onDocumentCreated("notifications/{notificationId}", async (event) => {

    // 1. Ambil data snapshot
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event");
        return;
    }

    const notiData = snapshot.data();
    const recipientId = notiData.recipientId;
    const title = notiData.title || "New Notification";
    const body = notiData.message || "You have a new update";

    console.log("Nak hantar noti kepada User ID:", recipientId);

    try {
      // 2. Cari FCM Token user tersebut dalam collection 'users'
      // FIX DI SINI: Tukar .document() jadi .doc()
      const userDoc = await admin.firestore().collection("users").doc(recipientId).get();

      if (!userDoc.exists) {
        console.log("User tak jumpa!");
        return null;
      }

      const userData = userDoc.data();
      const fcmToken = userData.fcmToken;

      if (!fcmToken) {
        console.log("User ni takde token (mungkin tak pernah login app baru)!");
        return null;
      }

      // 3. Bina Payload Mesej
      const message = {
        notification: {
          title: title,
          body: body,
        },
        token: fcmToken,
        data: {
            // --- TAMBAH LINE INI (PENTING!) ---
            nav_to: "notification_tab",
            // ----------------------------------

            click_action: "FLUTTER_NOTIFICATION_CLICK",
            productId: notiData.productId || ""
        }
      };

      // 4. Hantar ke FCM
      const response = await admin.messaging().send(message);
      console.log("Berjaya hantar noti:", response);
      return {success: true};

    } catch (error) {
      console.log("Error hantar noti:", error);
      return {error: error.message};
    }
});