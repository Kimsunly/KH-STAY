
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { GoogleAuth } = require('google-auth-library');
const axios = require('axios');

admin.initializeApp();
const db = admin.firestore();

const PROJECT_ID =
  process.env.GCLOUD_PROJECT ||
  (process.env.FIREBASE_CONFIG ? JSON.parse(process.env.FIREBASE_CONFIG).projectId : '');

const auth = new GoogleAuth({
  scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
});

/** Build DATA-ONLY message payload */
function buildDataOnlyMessage(token, { title, body, type = 'generic', data = {}, targetUid }) {
  return {
    message: {
      token,
      data: {
        title,
        body,
        type,
        ...(data || {}),
        targetUid: targetUid || '',
      },
      android: { priority: 'high' },
    },
  };
}

exports.sendNotification = functions.https.onRequest(async (req, res) => {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  try {
    const { targetUserId, token, title, body, type, data } = req.body || {};
    if (!title || !body) return res.status(400).json({ error: 'Missing required fields: title, body' });

    // Resolve token by UID if token param not provided
    let fcmToken = token || null;
    let resolvedUid = targetUserId || null;

    if (!fcmToken && targetUserId) {
      const userDoc = await db.collection('users').doc(targetUserId).get();
      if (!userDoc.exists) {
        // Still create in-app notif for non-existing user doc? Usually no.
        return res.status(404).json({ error: 'Target user not found' });
      }
      fcmToken = userDoc.get('fcmToken') || null;
    }

    // Try reverse mapping for safety (tokens/{token}.uid)
    let ownerUid = null;
    if (fcmToken) {
      const tokenDoc = await db.collection('tokens').doc(fcmToken).get();
      ownerUid = tokenDoc.exists ? tokenDoc.get('uid') : null;
    }
    if (!resolvedUid && ownerUid) resolvedUid = ownerUid;

    // If both exist but mismatch, refuse (prevents misrouting)
    if (ownerUid && targetUserId && ownerUid !== targetUserId) {
      console.error('Token ownership mismatch: token belongs to', ownerUid, 'not', targetUserId);
      return res.status(409).json({ error: 'Token belongs to another user; refusing to send' });
    }

    // ✅ Always write in-app notification first (Admin SDK bypasses client rules)
    if (resolvedUid) {
      await db.collection('users').doc(resolvedUid).collection('notifications').add({
        title,
        body,
        type: type || 'generic',
        data: data || {},
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
      });
    }

    // If no token, return 200 with delivered=false (in-app only)
    if (!fcmToken) {
      return res.status(200).json({
        ok: true,
        delivered: false,
        reason: 'NO_TOKEN',
        note: 'In-app notification created; user has no FCM token',
      });
    }

    // Send FCM (data-only)
    const client = await auth.getClient();
    const accessToken = await client.getAccessToken();
    const url = `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`;

    const bodyJson = buildDataOnlyMessage(fcmToken, {
      title,
      body,
      type: type || 'generic',
      data,
      targetUid: resolvedUid || targetUserId || ownerUid || '',
    });

    console.log('[sendNotification] targetUserId=', targetUserId);
    console.log('[sendNotification] resolvedUid=', resolvedUid);
    console.log('[sendNotification] token=', fcmToken);

    let delivered = true;
    try {
      const rsp = await axios.post(url, bodyJson, {
        headers: { Authorization: `Bearer ${accessToken.token}`, 'Content-Type': 'application/json' },
      });
      return res.status(200).json({ ok: true, delivered, fcmResponse: rsp.data });
    } catch (err) {
      const details = (err && err.response && err.response.data) ? err.response.data : err.message;
      console.error('FCM v1 error:', details);
      // Even if FCM fails (UNREGISTERED/INVALID_ARGUMENT), we already wrote in-app
      delivered = false;
      return res.status(200).json({ ok: true, delivered, reason: 'FCM_ERROR', details });
    }
  } catch (err) {
    console.error('sendNotification fatal error:', err);
    return res.status(500).json({ error: 'Failed to send notification', details: err.message });
  }
});
