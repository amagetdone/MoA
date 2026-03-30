importScripts('https://www.gstatic.com/firebasejs/12.9.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.9.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'AIzaSyB56iWJ5MGAVGUs_zuxzDOy_O1efnPrTQM',
  authDomain: 'mooa-6b3f7.firebaseapp.com',
  projectId: 'mooa-6b3f7',
  storageBucket: 'mooa-6b3f7.firebasestorage.app',
  messagingSenderId: '433365528425',
  appId: '1:433365528425:web:2c13d820812d0aabf4d7f8',
  measurementId: 'G-1LEF2JHTVC',
});

const messaging = firebase.messaging();

function resolveRoute(data) {
  const type = (data?.type || '').toUpperCase();
  const planId = data?.planId;

  if (type === 'PLAN_AI_COMPLETED' && planId) {
    const status = (data?.aiPipelineStatus || '').toUpperCase();
    return status === 'SUCCEEDED'
      ? `/goal/preview?id=${planId}`
      : `/goal/detail?id=${planId}`;
  }

  return '/menu';
}

function base64UrlEncode(value) {
  return btoa(unescape(encodeURIComponent(value)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
}

function buildLaunchUrl(route, envelope) {
  const encodedPayload = base64UrlEncode(JSON.stringify(envelope));
  return `${self.location.origin}/?fcmPayload=${encodedPayload}#${route}`;
}

messaging.onBackgroundMessage((payload) => {
  const title =
    payload?.notification?.title ||
    payload?.data?.title ||
    'MORE';
  const body =
    payload?.notification?.body ||
    payload?.data?.body ||
    '';
  const route = resolveRoute(payload?.data || {});

  const envelope = {
    messageId: payload?.messageId || '',
    title,
    body,
    sentTime: new Date().toISOString(),
    data: payload?.data || {},
  };

  self.registration.showNotification(title, {
    body,
    icon: '/icons/Icon-192.png',
    badge: '/icons/Icon-192.png',
    data: {
      route,
      envelope,
    },
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const route = event.notification?.data?.route || '/menu';
  const envelope = event.notification?.data?.envelope || {
    title: event.notification?.title || 'MORE',
    body: event.notification?.body || '',
    sentTime: new Date().toISOString(),
    data: {},
  };
  const launchUrl = buildLaunchUrl(route, envelope);

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (
          'navigate' in client &&
          typeof client.url === 'string' &&
          client.url.startsWith(self.location.origin) &&
          !client.url.includes('/admin') &&
          !client.url.includes('/pgadmin')
        ) {
          return client.navigate(launchUrl).then(() => client.focus());
        }
      }

      if (clients.openWindow) {
        return clients.openWindow(launchUrl);
      }

      return null;
    }),
  );
});
