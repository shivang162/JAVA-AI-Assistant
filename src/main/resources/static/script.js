const tabs = document.querySelectorAll('.nav-btn');
const panels = document.querySelectorAll('.tab-panel');

const aiHistory = document.getElementById('ai-history');
const aiForm = document.getElementById('ai-form');
const aiInput = document.getElementById('ai-input');
const aiLoading = document.getElementById('ai-loading');

const friendHistory = document.getElementById('friend-history');
const friendForm = document.getElementById('friend-form');
const friendInput = document.getElementById('friend-input');
const groupNameInput = document.getElementById('group-name-input');
const groupIdInput = document.getElementById('group-id-input');
const createGroupBtn = document.getElementById('create-group-btn');
const joinGroupBtn = document.getElementById('join-group-btn');
const activeGroupLabel = document.getElementById('active-group-label');

const startCall = document.getElementById('start-call');
const joinCall = document.getElementById('join-call');
const endCall = document.getElementById('end-call');
const muteBtn = document.getElementById('mute-btn');
const videoBtn = document.getElementById('video-btn');
const callStatus = document.getElementById('call-status');
const callTimer = document.getElementById('call-timer');
const callSync = document.getElementById('call-sync');
const localOnline = document.getElementById('local-online');
const remoteOnline = document.getElementById('remote-online');
const localVideo = document.getElementById('local-video');
const remoteVideo = document.getElementById('remote-video');
const videoSessionIdInput = document.getElementById('video-session-id');

const currentDeviceLabel = document.getElementById('current-device-label');
const refreshDevicesBtn = document.getElementById('refresh-devices');
const deviceList = document.getElementById('device-list');

let callSeconds = 0;
let callTimerId = null;
let muted = false;
let videoOff = false;
let localStream = null;
let localPeerConnection = null;
let remotePeerConnection = null;
let activeVideoSessionId = '';
let activeGroupId = '';
let groupSocket = null;
let groupSocketReconnectDelayId = null;
let groupSocketRetries = 0;

const deviceNameById = new Map();
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
    // no-op: storage can be unavailable in private/restricted contexts
  }
}
const currentDeviceName = `Browser-${currentDeviceId.slice(-4)}`;
const currentDeviceType = /Mobi|Android/i.test(navigator.userAgent) ? 'mobile' : 'desktop';

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  let data = {};
  try {
    data = await response.json();
  } catch (error) {
    console.error('Response JSON parse failed:', error);
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
  tabs.forEach((btn) => btn.classList.toggle('active', btn.dataset.tab === tabId));
  panels.forEach((panel) => panel.classList.toggle('active', panel.id === tabId));
}

function renderDevices(devices) {
  if (!Array.isArray(devices) || devices.length === 0) {
    deviceList.innerHTML = '<small class="muted-text">No active devices</small>';
    return;
  }
  deviceNameById.clear();
  devices.forEach((device) => {
    deviceNameById.set(device.deviceId, device.name);
  });
  deviceList.innerHTML = devices
    .map((device) => (
      `<div class="device-item"><strong>${device.name}</strong> · ${device.type} · ${device.deviceId}</div>`
    ))
    .join('');
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

function updateCallTimer() {
  callSeconds += 1;
  const mm = String(Math.floor(callSeconds / 60)).padStart(2, '0');
  const ss = String(callSeconds % 60).padStart(2, '0');
  callTimer.textContent = `${mm}:${ss}`;
}

function setCallButtons(inCall) {
  startCall.disabled = inCall;
  joinCall.disabled = inCall;
  endCall.disabled = !inCall;
  muteBtn.disabled = !inCall;
  videoBtn.disabled = !inCall;
}

function stopStreamTracks(stream) {
  if (!stream) return;
  stream.getTracks().forEach((track) => track.stop());
}

function closePeerConnections() {
  if (localPeerConnection) {
    localPeerConnection.close();
    localPeerConnection = null;
  }
  if (remotePeerConnection) {
    remotePeerConnection.close();
    remotePeerConnection = null;
  }
  remoteVideo.srcObject = null;
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

async function setupPeerConnection() {
  closePeerConnections();
  localPeerConnection = new RTCPeerConnection();
  remotePeerConnection = new RTCPeerConnection();

  localPeerConnection.onicecandidate = (event) => {
    if (event.candidate && remotePeerConnection) {
      remotePeerConnection.addIceCandidate(event.candidate).catch(console.error);
    }
  };

  remotePeerConnection.onicecandidate = (event) => {
    if (event.candidate && localPeerConnection) {
      localPeerConnection.addIceCandidate(event.candidate).catch(console.error);
    }
  };

  remotePeerConnection.ontrack = (event) => {
    const [stream] = event.streams;
    if (stream) {
      remoteVideo.srcObject = stream;
    }
  };

  localStream.getTracks().forEach((track) => {
    localPeerConnection.addTrack(track, localStream);
  });

  const offer = await localPeerConnection.createOffer();
  await localPeerConnection.setLocalDescription(offer);
  await remotePeerConnection.setRemoteDescription(offer);
  const answer = await remotePeerConnection.createAnswer();
  await remotePeerConnection.setLocalDescription(answer);
  await localPeerConnection.setRemoteDescription(answer);
}

async function startVideoCall() {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('Camera is not supported in this browser.');
  }

  localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  localVideo.srcObject = localStream;
  applyTrackStates();
  await setupPeerConnection();
}

function endVideoCall() {
  clearInterval(callTimerId);
  callTimerId = null;
  callSeconds = 0;
  callTimer.textContent = '00:00';
  stopStreamTracks(localStream);
  localStream = null;
  localVideo.srcObject = null;
  closePeerConnections();
}

function renderGroupMessages(messages) {
  friendHistory.innerHTML = '';
  if (!Array.isArray(messages) || messages.length === 0) {
    addMessage(friendHistory, 'System', 'No messages yet.');
    return;
  }
  messages.forEach((message) => {
    const outgoing = message.senderDeviceId === currentDeviceId;
    const displayName = outgoing ? 'You' : (deviceNameById.get(message.senderDeviceId) || message.senderDeviceId);
    addMessage(friendHistory, displayName, message.content, outgoing);
  });
}

async function loadGroupMessages(groupId) {
  const messages = await fetchJson(`/api/group-chat/${encodeURIComponent(groupId)}/messages`);
  renderGroupMessages(messages);
}

function resetCallState() {
  setCallButtons(false);
  callStatus.textContent = 'Idle';
  callStatus.className = 'status-pill idle';
  callSync.textContent = 'Ready for multi-device session';
  localOnline.textContent = 'Offline';
  remoteOnline.textContent = 'Waiting';
  muted = false;
  videoOff = false;
  muteBtn.textContent = 'Mute';
  videoBtn.textContent = 'Video Off';
}

function scheduleGroupSocketReconnect() {
  if (groupSocketReconnectDelayId) return;
  const cappedRetries = Math.min(groupSocketRetries, 5);
  const waitMs = Math.min(1000 * (2 ** cappedRetries), 10000);
  groupSocketRetries = Math.min(groupSocketRetries + 1, 5);
  groupSocketReconnectDelayId = setTimeout(() => {
    groupSocketReconnectDelayId = null;
    connectGroupSocket();
  }, waitMs);
}

function connectGroupSocket() {
  if (groupSocket && (groupSocket.readyState === WebSocket.OPEN || groupSocket.readyState === WebSocket.CONNECTING)) {
    return;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws/group-chat`;
  try {
    groupSocket = new WebSocket(wsUrl);
  } catch (error) {
    scheduleGroupSocketReconnect();
    return;
  }

  groupSocket.addEventListener('open', () => {
    groupSocketRetries = 0;
  });

  groupSocket.addEventListener('message', (event) => {
    try {
      const payload = JSON.parse(event.data);
      if (payload?.type !== 'group-message') return;
      if (!activeGroupId || payload.groupId !== activeGroupId) return;
      if (payload?.message?.senderDeviceId === currentDeviceId) return;
      const displayName = deviceNameById.get(payload.message.senderDeviceId) || payload.message.senderDeviceId;
      addMessage(friendHistory, displayName, payload.message.content, false, 'live');
    } catch (error) {
      console.error(`Failed to parse WebSocket message: ${error?.message || 'unknown error'}`, event.data);
    }
  });

  groupSocket.addEventListener('close', scheduleGroupSocketReconnect);
  groupSocket.addEventListener('error', () => {
    if (groupSocket?.readyState !== WebSocket.OPEN) {
      scheduleGroupSocketReconnect();
    }
  });
}

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

async function connectToVideoSession(joinExisting) {
  const requestSessionId = videoSessionIdInput.value.trim() || null;
  const endpoint = joinExisting ? '/api/video/join' : '/api/video/start';
  const payload = joinExisting
    ? { sessionId: requestSessionId, deviceId: currentDeviceId }
    : { sessionId: requestSessionId, hostDeviceId: currentDeviceId };

  const session = await postJson(endpoint, payload);
  activeVideoSessionId = session.sessionId;
  videoSessionIdInput.value = session.sessionId;
  await startVideoCall();
  setCallButtons(true);
  callStatus.textContent = 'In Call';
  callStatus.className = 'status-pill busy';
  callSync.textContent = `${session.connectionMode} · participants ${session.participantDeviceIds.length}`;
  localOnline.textContent = 'Online';
  remoteOnline.textContent = 'Connected';
  callSeconds = 0;
  callTimer.textContent = '00:00';
  clearInterval(callTimerId);
  callTimerId = setInterval(updateCallTimer, 1000);
}

startCall.addEventListener('click', async () => {
  if (startCall.disabled) return;
  startCall.disabled = true;
  try {
    await connectToVideoSession(false);
  } catch (error) {
    endVideoCall();
    resetCallState();
    callStatus.textContent = 'Session Error';
    callSync.textContent = error.message || 'Unable to start call.';
  } finally {
    if (!activeVideoSessionId) {
      setCallButtons(false);
    }
  }
});

joinCall.addEventListener('click', async () => {
  if (joinCall.disabled) return;
  joinCall.disabled = true;
  try {
    await connectToVideoSession(true);
  } catch (error) {
    endVideoCall();
    resetCallState();
    callStatus.textContent = 'Join Error';
    callSync.textContent = error.message || 'Unable to join session.';
  } finally {
    if (!activeVideoSessionId) {
      setCallButtons(false);
    }
  }
});

endCall.addEventListener('click', async () => {
  const sessionId = activeVideoSessionId;
  endVideoCall();
  resetCallState();
  activeVideoSessionId = '';
  if (sessionId) {
    try {
      await postJson('/api/video/end', { sessionId });
    } catch (error) {
      callSync.textContent = `Ended locally (${error.message})`;
    }
  }
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

createGroupBtn.addEventListener('click', async () => {
  const name = groupNameInput.value.trim();
  if (!name) return;
  try {
    const group = await postJson('/api/group-chat/create', {
      groupId: groupIdInput.value.trim() || null,
      name,
      creatorDeviceId: currentDeviceId
    });
    activeGroupId = group.groupId;
    groupIdInput.value = group.groupId;
    activeGroupLabel.textContent = group.groupId;
    await loadGroupMessages(activeGroupId);
  } catch (error) {
    addMessage(friendHistory, 'System', `Create failed: ${error.message}`);
  }
});

joinGroupBtn.addEventListener('click', async () => {
  const groupId = groupIdInput.value.trim();
  if (!groupId) return;
  try {
    await postJson('/api/group-chat/join', { groupId, deviceId: currentDeviceId });
    activeGroupId = groupId;
    activeGroupLabel.textContent = groupId;
    await loadGroupMessages(groupId);
  } catch (error) {
    addMessage(friendHistory, 'System', `Join failed: ${error.message}`);
  }
});

friendForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const msg = friendInput.value.trim();
  if (!msg) return;
  if (!activeGroupId) {
    addMessage(friendHistory, 'System', 'Create or join a group first.');
    return;
  }

  friendInput.value = '';
  try {
    await postJson('/api/group-chat/send', {
      groupId: activeGroupId,
      deviceId: currentDeviceId,
      message: msg
    });
    await loadGroupMessages(activeGroupId);
  } catch (error) {
    addMessage(friendHistory, 'System', `Send failed: ${error.message}`);
  }
});

resetCallState();
addMessage(friendHistory, 'System', 'Create or join a group chat to start messaging.');
connectGroupSocket();

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

window.addEventListener('beforeunload', endVideoCall);
window.addEventListener('beforeunload', () => {
  if (groupSocketReconnectDelayId) {
    clearTimeout(groupSocketReconnectDelayId);
  }
  if (groupSocket && groupSocket.readyState === WebSocket.OPEN) {
    groupSocket.close();
  }
});
