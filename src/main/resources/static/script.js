const tabs = document.querySelectorAll('.nav-btn');
const panels = document.querySelectorAll('.tab-panel');

const aiHistory = document.getElementById('ai-history');
const aiForm = document.getElementById('ai-form');
const aiInput = document.getElementById('ai-input');
const aiLoading = document.getElementById('ai-loading');

const friendHistory = document.getElementById('friend-history');
const friendForm = document.getElementById('friend-form');
const friendInput = document.getElementById('friend-input');
const connectChatBtn = document.getElementById('connect-chat-btn');
const chatRemotePeerIdInput = document.getElementById('chat-remote-peer-id');
const shareCodeBtn = document.getElementById('share-code-btn');
const shareOutputBtn = document.getElementById('share-output-btn');
const shareCodeEditor = document.getElementById('share-code-editor');
const shareOutputConsole = document.getElementById('share-output-console');
const chatStatus = document.getElementById('chat-status');
const chatUnreadBadge = document.getElementById('chat-unread-badge');

const startCall = document.getElementById('start-call');
const joinCall = document.getElementById('join-call');
const endCall = document.getElementById('end-call');
const muteBtn = document.getElementById('mute-btn');
const videoBtn = document.getElementById('video-btn');
const screenShareBtn = document.getElementById('screen-share-btn');
const callStatus = document.getElementById('call-status');
const callTimer = document.getElementById('call-timer');
const callSync = document.getElementById('call-sync');
const localOnline = document.getElementById('local-online');
const remoteOnline = document.getElementById('remote-online');
const localVideo = document.getElementById('local-video');
const remoteVideo = document.getElementById('remote-video');
const remotePeerIdInput = document.getElementById('remote-peer-id');
const myPeerIdLabel = document.getElementById('my-peer-id');

const incomingCallPopup = document.getElementById('incoming-call-popup');
const incomingCallText = document.getElementById('incoming-call-text');
const acceptCallBtn = document.getElementById('accept-call-btn');
const rejectCallBtn = document.getElementById('reject-call-btn');

const currentDeviceLabel = document.getElementById('current-device-label');
const refreshDevicesBtn = document.getElementById('refresh-devices');
const deviceList = document.getElementById('device-list');

const deviceNameById = new Map();
let activeTabId = 'assistant';
let unreadChatCount = 0;

const connectDeviceBanner = document.getElementById('connect-device-banner');
const connectBannerUrl = document.getElementById('connect-banner-url');
const connectQrCanvas = document.getElementById('connect-qr');

function devicePeerId(deviceId) {
  return `peer-${deviceId.replace(/[^a-zA-Z0-9-]/g, '')}`;
}

let callSeconds = 0;
let callTimerId = null;
let muted = false;
let videoOff = false;
let localStream = null;
let activeCall = null;
let pendingIncomingCall = null;
let screenStream = null;
let originalCameraTrack = null;

let peer = null;
let activeDataConnection = null;

const storedDeviceId = (() => {
  try {
    return localStorage.getItem('assistant-device-id');
  } catch {
    return null;
  }
})();
const generatedDeviceId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
  ? crypto.randomUUID()
  : `device-${Math.random().toString(36).slice(2, 10)}`;
const currentDeviceId = storedDeviceId || generatedDeviceId;
if (!storedDeviceId) {
  try {
    localStorage.setItem('assistant-device-id', currentDeviceId);
  } catch {
    // no-op
  }
}
const currentDeviceName = `Browser-${currentDeviceId.slice(-4)}`;
const currentDeviceType = /Mobi|Android/i.test(navigator.userAgent) ? 'mobile' : 'desktop';

function getCurrentTimeString() {
  return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function addMessage(target, who, text, outgoing = false, delivery = '') {
  const row = document.createElement('article');
  row.className = `message ${outgoing ? 'self' : ''}`;

  const avatar = document.createElement('span');
  avatar.className = 'avatar';
  avatar.textContent = who[0] ?? '?';

  const bubble = document.createElement('div');
  bubble.className = 'bubble';

  const label = document.createElement('strong');
  label.textContent = who;
  bubble.appendChild(label);
  bubble.appendChild(document.createElement('br'));
  bubble.appendChild(document.createTextNode(text));

  const meta = document.createElement('span');
  meta.className = 'meta';
  meta.textContent = `${getCurrentTimeString()} ${delivery}`.trim();
  bubble.appendChild(meta);

  row.appendChild(avatar);
  row.appendChild(bubble);
  target.appendChild(row);
  target.scrollTop = target.scrollHeight;
}

function setActiveTab(tabId) {
  activeTabId = tabId;
  tabs.forEach((btn) => btn.classList.toggle('active', btn.dataset.tab === tabId));
  panels.forEach((panel) => panel.classList.toggle('active', panel.id === tabId));
  if (tabId === 'friends-chat') {
    unreadChatCount = 0;
    updateChatUnreadBadge();
  }
}

function updateChatUnreadBadge() {
  chatUnreadBadge.textContent = String(unreadChatCount);
  chatUnreadBadge.classList.toggle('hidden', unreadChatCount === 0);
}

function incrementUnreadChat() {
  if (activeTabId === 'friends-chat') return;
  unreadChatCount += 1;
  updateChatUnreadBadge();
}

function setCallButtons(inCall) {
  joinCall.disabled = inCall;
  endCall.disabled = !inCall;
  muteBtn.disabled = !inCall;
  videoBtn.disabled = !inCall;
  screenShareBtn.disabled = !inCall;
}

function updateCallTimer() {
  callSeconds += 1;
  const mm = String(Math.floor(callSeconds / 60)).padStart(2, '0');
  const ss = String(callSeconds % 60).padStart(2, '0');
  callTimer.textContent = `${mm}:${ss}`;
}

function stopTimer() {
  clearInterval(callTimerId);
  callTimerId = null;
  callSeconds = 0;
  callTimer.textContent = '00:00';
}

function stopStreamTracks(stream) {
  if (!stream) return;
  stream.getTracks().forEach((track) => track.stop());
}

function applyTrackStates() {
  if (!localStream) return;
  localStream.getAudioTracks().forEach((track) => {
    track.enabled = !muted;
  });
  localStream.getVideoTracks().forEach((track) => {
    track.enabled = !videoOff;
  });
}

async function ensureLocalMedia() {
  if (localStream) return localStream;
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('Camera/mic are not supported in this browser.');
  }
  localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  originalCameraTrack = localStream.getVideoTracks()[0] || null;
  localVideo.srcObject = localStream;
  localOnline.textContent = 'Online';
  applyTrackStates();
  return localStream;
}

function resetCallState() {
  callStatus.textContent = 'Idle';
  callStatus.className = 'status-pill idle';
  callSync.textContent = 'P2P ready';
  localOnline.textContent = localStream ? 'Online' : 'Offline';
  remoteOnline.textContent = 'Waiting';
  muted = false;
  videoOff = false;
  muteBtn.textContent = 'Mute';
  videoBtn.textContent = 'Video Off';
  screenShareBtn.textContent = 'Share Screen';
  setCallButtons(false);
}

function attachCallHandlers(call) {
  activeCall = call;
  call.on('stream', (remoteStream) => {
    remoteVideo.srcObject = remoteStream;
    remoteOnline.textContent = 'Connected';
  });
  call.on('close', () => {
    remoteVideo.srcObject = null;
    activeCall = null;
    stopTimer();
    resetCallState();
  });
  call.on('error', (error) => {
    callSync.textContent = `Call error: ${error?.message || 'Unknown error'}`;
    remoteVideo.srcObject = null;
    activeCall = null;
    stopTimer();
    resetCallState();
  });
}

async function placeCall(remotePeerId) {
  if (!peer?.open) {
    throw new Error('Peer connection is not ready yet.');
  }
  const stream = await ensureLocalMedia();
  const call = peer.call(remotePeerId, stream, { metadata: { from: peer.id } });
  attachCallHandlers(call);

  callStatus.textContent = 'Calling';
  callStatus.className = 'status-pill busy';
  callSync.textContent = `Calling ${remotePeerId}`;
  setCallButtons(true);

  stopTimer();
  callTimerId = setInterval(updateCallTimer, 1000);
}

function endVideoCall() {
  if (activeCall) {
    activeCall.close();
    activeCall = null;
  }
  remoteVideo.srcObject = null;
  remoteOnline.textContent = 'Waiting';
  stopTimer();
  resetCallState();
}

function replaceVideoTrack(track) {
  if (!activeCall?.peerConnection) return;
  const sender = activeCall.peerConnection.getSenders().find((item) => item.track?.kind === 'video');
  if (sender) {
    sender.replaceTrack(track).catch(() => {
      callSync.textContent = 'Could not switch video track';
    });
  }
}

function showIncomingCallPopup(fromPeerId) {
  incomingCallText.textContent = `Incoming call from ${fromPeerId}`;
  incomingCallPopup.classList.remove('hidden');
}

function hideIncomingCallPopup() {
  incomingCallPopup.classList.add('hidden');
}

function setChatStatus(connected) {
  chatStatus.textContent = connected ? 'Connected' : 'Disconnected';
  chatStatus.className = `status-pill ${connected ? 'live' : 'idle'}`;
}

function setupDataConnection(connection) {
  if (activeDataConnection && activeDataConnection !== connection && activeDataConnection.open) {
    activeDataConnection.close();
  }
  activeDataConnection = connection;

  connection.on('open', () => {
    setChatStatus(true);
    addMessage(friendHistory, 'System', `P2P chat connected with ${connection.peer}`);
  });

  connection.on('data', (payload) => {
    const type = payload?.type || 'chat';
    const messageText = typeof payload?.text === 'string' ? payload.text : '';
    if (!messageText) return;

    if (type === 'code') {
      addMessage(friendHistory, connection.peer, `Shared Code:\n${messageText}`);
      shareCodeEditor.value = messageText;
    } else if (type === 'output') {
      addMessage(friendHistory, connection.peer, `Shared Output:\n${messageText}`);
      shareOutputConsole.value = messageText;
    } else {
      addMessage(friendHistory, connection.peer, messageText);
    }
    incrementUnreadChat();
  });

  connection.on('close', () => {
    if (activeDataConnection === connection) {
      activeDataConnection = null;
      setChatStatus(false);
      addMessage(friendHistory, 'System', 'P2P chat disconnected.');
    }
  });

  connection.on('error', (error) => {
    addMessage(friendHistory, 'System', `Chat error: ${error?.message || 'Unknown error'}`);
  });
}

function requireOpenDataConnection() {
  if (!activeDataConnection || !activeDataConnection.open) {
    throw new Error('Chat is not connected. Connect to a friend first.');
  }
  return activeDataConnection;
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  let data = {};
  try {
    data = await response.json();
  } catch {
    data = {};
  }
  if (!response.ok) {
    throw new Error(data.error || data.response || 'Request failed');
  }
  return data;
}

function postJson(url, payload) {
  return fetchJson(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

function renderDevices(devices) {
  if (!Array.isArray(devices) || devices.length === 0) {
    deviceList.innerHTML = '<small class="muted-text">No active devices — open the app on another device to see it here</small>';
    return;
  }
  deviceNameById.clear();
  devices.forEach((device) => {
    deviceNameById.set(device.deviceId, device.name);
  });
  deviceList.innerHTML = devices
    .filter((device) => currentDeviceId == null || device.deviceId !== currentDeviceId)
    .map((device) => {
      const peerId = devicePeerId(device.deviceId);
      return (
        `<div class="device-item">` +
          `<div><strong>${device.name}</strong> · <span class="device-type">${device.type}</span></div>` +
          `<div class="device-actions">` +
            `<button class="device-call-btn" data-peer-id="${peerId}" type="button">📞 Call</button>` +
            `<button class="device-chat-btn" data-peer-id="${peerId}" type="button">💬 Chat</button>` +
          `</div>` +
        `</div>`
      );
    })
    .join('') || '<small class="muted-text">No other active devices yet</small>';

  deviceList.querySelectorAll('.device-call-btn').forEach((btn) => {
    btn.addEventListener('click', () => quickCallDevice(btn.dataset.peerId));
  });
  deviceList.querySelectorAll('.device-chat-btn').forEach((btn) => {
    btn.addEventListener('click', () => quickChatDevice(btn.dataset.peerId));
  });
}

async function quickCallDevice(peerId) {
  remotePeerIdInput.value = peerId;
  setActiveTab('video');
  if (!localStream) {
    try {
      await ensureLocalMedia();
      callStatus.textContent = 'Ready';
      callStatus.className = 'status-pill live';
    } catch (err) {
      callSync.textContent = err.message || 'Unable to open camera/mic';
      return;
    }
  }
  try {
    await placeCall(peerId);
  } catch (err) {
    callSync.textContent = err.message || 'Unable to start call';
    setCallButtons(false);
  }
}

function quickChatDevice(peerId) {
  chatRemotePeerIdInput.value = peerId;
  setActiveTab('friends-chat');
  if (!peer?.open) {
    addMessage(friendHistory, 'System', 'Peer connection is not ready yet. Try again in a moment.');
    return;
  }
  setupDataConnection(peer.connect(peerId, { reliable: true }));
}

async function refreshDevices() {
  const devices = await fetchJson('/api/devices');
  renderDevices(devices);
}

async function registerCurrentDevice() {
  const device = await postJson('/api/device/register', {
    deviceId: currentDeviceId,
    name: currentDeviceName,
    type: currentDeviceType
  });
  currentDeviceLabel.textContent = `${device.name} (${device.deviceId})`;
  return device;
}

function initializePeer() {
  if (typeof window.Peer !== 'function') {
    callSync.textContent = 'PeerJS failed to load';
    myPeerIdLabel.textContent = 'Unavailable';
    return;
  }

  peer = new window.Peer(`peer-${currentDeviceId.replace(/[^a-zA-Z0-9-]/g, '')}`);

  peer.on('open', (id) => {
    myPeerIdLabel.textContent = id;
    remotePeerIdInput.placeholder = 'Friend Peer ID';
    chatRemotePeerIdInput.placeholder = 'Friend Peer ID for chat';
    callSync.textContent = 'P2P ready';
  });

  peer.on('call', (incomingCall) => {
    pendingIncomingCall = incomingCall;
    showIncomingCallPopup(incomingCall.peer);
  });

  peer.on('connection', (connection) => {
    setupDataConnection(connection);
  });

  peer.on('error', (error) => {
    callSync.textContent = `Peer error: ${error?.type || error?.message || 'unknown'}`;
  });
}

startCall.addEventListener('click', async () => {
  startCall.disabled = true;
  try {
    await ensureLocalMedia();
    callStatus.textContent = 'Ready';
    callStatus.className = 'status-pill live';
    callSync.textContent = 'Camera/mic ready for P2P call';
  } catch (error) {
    callSync.textContent = error.message || 'Unable to open camera/mic';
  } finally {
    startCall.disabled = false;
  }
});

joinCall.addEventListener('click', async () => {
  const remotePeerId = remotePeerIdInput.value.trim();
  if (!remotePeerId) {
    callSync.textContent = 'Enter a Friend Peer ID first';
    return;
  }
  joinCall.disabled = true;
  try {
    await placeCall(remotePeerId);
  } catch (error) {
    callSync.textContent = error.message || 'Unable to start call';
    setCallButtons(false);
  } finally {
    if (!activeCall) {
      joinCall.disabled = false;
    }
  }
});

endCall.addEventListener('click', () => {
  endVideoCall();
});

muteBtn.addEventListener('click', () => {
  if (!localStream) return;
  muted = !muted;
  applyTrackStates();
  muteBtn.textContent = muted ? 'Unmute' : 'Mute';
});

videoBtn.addEventListener('click', () => {
  if (!localStream) return;
  videoOff = !videoOff;
  applyTrackStates();
  videoBtn.textContent = videoOff ? 'Video On' : 'Video Off';
});

screenShareBtn.addEventListener('click', async () => {
  if (!activeCall || !navigator.mediaDevices?.getDisplayMedia) {
    callSync.textContent = 'Screen sharing is unavailable right now';
    return;
  }

  if (screenStream) {
    stopStreamTracks(screenStream);
    screenStream = null;
    if (originalCameraTrack) {
      replaceVideoTrack(originalCameraTrack);
    }
    screenShareBtn.textContent = 'Share Screen';
    callSync.textContent = 'Returned to camera';
    return;
  }

  try {
    screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
    const screenTrack = screenStream.getVideoTracks()[0];
    if (!screenTrack) {
      throw new Error('No screen track available');
    }
    replaceVideoTrack(screenTrack);
    screenShareBtn.textContent = 'Stop Sharing';
    callSync.textContent = 'Screen sharing live';

    screenTrack.addEventListener('ended', () => {
      if (screenStream) {
        stopStreamTracks(screenStream);
        screenStream = null;
      }
      if (originalCameraTrack) {
        replaceVideoTrack(originalCameraTrack);
      }
      screenShareBtn.textContent = 'Share Screen';
      callSync.textContent = 'Returned to camera';
    });
  } catch (error) {
    callSync.textContent = error.message || 'Failed to share screen';
  }
});

acceptCallBtn.addEventListener('click', async () => {
  const incoming = pendingIncomingCall;
  pendingIncomingCall = null;
  hideIncomingCallPopup();
  if (!incoming) return;

  try {
    const stream = await ensureLocalMedia();
    incoming.answer(stream);
    attachCallHandlers(incoming);

    callStatus.textContent = 'In Call';
    callStatus.className = 'status-pill busy';
    callSync.textContent = `Connected with ${incoming.peer}`;
    setCallButtons(true);

    stopTimer();
    callTimerId = setInterval(updateCallTimer, 1000);
  } catch (error) {
    callSync.textContent = error.message || 'Failed to accept call';
  }
});

rejectCallBtn.addEventListener('click', () => {
  if (pendingIncomingCall) {
    pendingIncomingCall.close();
  }
  pendingIncomingCall = null;
  hideIncomingCallPopup();
  callSync.textContent = 'Incoming call rejected';
});

connectChatBtn.addEventListener('click', () => {
  if (!peer?.open) {
    addMessage(friendHistory, 'System', 'Peer connection is not ready yet.');
    return;
  }
  const remotePeerId = chatRemotePeerIdInput.value.trim() || remotePeerIdInput.value.trim();
  if (!remotePeerId) {
    addMessage(friendHistory, 'System', 'Enter Friend Peer ID for chat.');
    return;
  }
  setupDataConnection(peer.connect(remotePeerId, { reliable: true }));
});

friendForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const msg = friendInput.value.trim();
  if (!msg) return;

  try {
    const conn = requireOpenDataConnection();
    conn.send({ type: 'chat', text: msg });
    addMessage(friendHistory, 'You', msg, true, 'p2p');
    friendInput.value = '';
  } catch (error) {
    addMessage(friendHistory, 'System', error.message);
  }
});

shareCodeBtn.addEventListener('click', () => {
  const payload = shareCodeEditor.value.trim();
  if (!payload) {
    addMessage(friendHistory, 'System', 'Write some Java code first.');
    return;
  }
  try {
    const conn = requireOpenDataConnection();
    conn.send({ type: 'code', text: payload });
    addMessage(friendHistory, 'You', `Shared Code:\n${payload}`, true, 'p2p');
  } catch (error) {
    addMessage(friendHistory, 'System', error.message);
  }
});

shareOutputBtn.addEventListener('click', () => {
  const payload = shareOutputConsole.value.trim();
  if (!payload) {
    addMessage(friendHistory, 'System', 'Add some console output first.');
    return;
  }
  try {
    const conn = requireOpenDataConnection();
    conn.send({ type: 'output', text: payload });
    addMessage(friendHistory, 'You', `Shared Output:\n${payload}`, true, 'p2p');
  } catch (error) {
    addMessage(friendHistory, 'System', error.message);
  }
});

tabs.forEach((btn) => {
  btn.addEventListener('click', () => setActiveTab(btn.dataset.tab));
});

refreshDevicesBtn.addEventListener('click', () => {
  registerCurrentDevice()
    .then(refreshDevices)
    .catch((error) => {
      renderDevices([]);
      currentDeviceLabel.textContent = `Device registration failed: ${error.message}`;
    });
});

registerCurrentDevice()
  .then(refreshDevices)
  .catch((error) => {
    currentDeviceLabel.textContent = `Registration failed: ${error.message} (click Refresh to retry)`;
    renderDevices([]);
  });

async function initServerInfo() {
  try {
    const info = await fetchJson('/api/server-info');
    if (info.url && info.ip && info.ip !== 'localhost' && info.ip !== '127.0.0.1') {
      connectBannerUrl.textContent = info.url;
      connectDeviceBanner.classList.remove('hidden');
      if (typeof window.QRCode !== 'undefined') {
        window.QRCode.toCanvas(connectQrCanvas, info.url, { width: 128, margin: 1 }, (err) => {
          if (err) connectQrCanvas.style.display = 'none';
        });
      }
    }
  } catch {
    // server-info is best-effort
  }
}

function initDeviceWebSocket() {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}/ws/group-chat`;
  let ws;
  let reconnectTimer = null;

  function connect() {
    ws = new WebSocket(wsUrl);
    ws.addEventListener('message', (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload?.type === 'device-registered') {
          refreshDevices().catch((err) => console.warn('Failed to refresh devices:', err));
        }
      } catch {
        // ignore non-JSON frames
      }
    });
    ws.addEventListener('close', () => {
      if (!reconnectTimer) {
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null;
          connect();
        }, 5000);
      }
    });
    ws.addEventListener('error', () => {
      ws.close();
    });
  }

  connect();
}

setInterval(() => {
  refreshDevices().catch((err) => console.warn('Failed to refresh devices:', err));
}, 15000);

initServerInfo();
initDeviceWebSocket();

aiForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const question = aiInput.value.trim();
  if (!question) return;

  addMessage(aiHistory, 'You', question, true);
  aiInput.value = '';
  aiLoading.classList.remove('hidden');

  try {
    if (question.startsWith('/')) {
      const result = await postJson('/api/command', { command: question });
      addMessage(aiHistory, 'AI', result.response);
    } else {
      const result = await postJson('/api/chat', { message: question });
      addMessage(aiHistory, 'AI', result.response);
    }
  } catch (error) {
    addMessage(aiHistory, 'AI', `Error: ${error.message}`);
  } finally {
    aiLoading.classList.add('hidden');
  }
});

setCallButtons(false);
resetCallState();
setChatStatus(false);
updateChatUnreadBadge();
addMessage(friendHistory, 'System', 'Connect to a friend Peer ID to start P2P chat.');
initializePeer();

fetch('/api/history')
  .then((response) => response.json())
  .then((history) => {
    if (!Array.isArray(history) || history.length === 0) {
      addMessage(aiHistory, 'AI', 'Hi! Ask your question and I will respond in real time.');
      return;
    }
    history.forEach((message) => {
      const isUser = message.role === 'user';
      addMessage(aiHistory, isUser ? 'You' : 'AI', message.content, isUser);
    });
  })
  .catch(() => {
    addMessage(aiHistory, 'AI', 'Hi! Ask your question and I will respond in real time.');
  });

window.addEventListener('beforeunload', () => {
  endVideoCall();
  stopStreamTracks(localStream);
  localStream = null;
  stopStreamTracks(screenStream);
  screenStream = null;
  if (activeDataConnection?.open) {
    activeDataConnection.close();
  }
  if (peer && !peer.destroyed) {
    peer.destroy();
  }
});
