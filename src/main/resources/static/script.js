const tabs = document.querySelectorAll('.nav-btn');
const panels = document.querySelectorAll('.tab-panel');

const aiHistory = document.getElementById('ai-history');
const aiForm = document.getElementById('ai-form');
const aiInput = document.getElementById('ai-input');
const aiLoading = document.getElementById('ai-loading');

const friendHistory = document.getElementById('friend-history');
const friendForm = document.getElementById('friend-form');
const friendInput = document.getElementById('friend-input');

const startCall = document.getElementById('start-call');
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

let callSeconds = 0;
let callTimerId = null;
let muted = false;
let videoOff = false;
let localStream = null;
let localPeerConnection = null;
let remotePeerConnection = null;

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.response || 'Request failed');
  }
  return data;
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

tabs.forEach((btn) => {
  btn.addEventListener('click', () => setActiveTab(btn.dataset.tab));
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

friendForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const msg = friendInput.value.trim();
  if (!msg) return;

  addMessage(friendHistory, 'You', msg, true, '• sent');
  friendInput.value = '';

  setTimeout(() => addMessage(friendHistory, 'Friend', `Echo: ${msg}`, false, '• delivered'), 700);
});

function updateCallTimer() {
  callSeconds += 1;
  const mm = String(Math.floor(callSeconds / 60)).padStart(2, '0');
  const ss = String(callSeconds % 60).padStart(2, '0');
  callTimer.textContent = `${mm}:${ss}`;
}

function setCallButtons(inCall) {
  startCall.disabled = inCall;
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

function resetCallState() {
  setCallButtons(false);
  callStatus.textContent = 'Idle';
  callStatus.className = 'status-pill idle';
  callSync.textContent = 'Two-device ready (WebRTC hook)';
  localOnline.textContent = 'Offline';
  remoteOnline.textContent = 'Waiting';
  muted = false;
  videoOff = false;
  muteBtn.textContent = 'Mute';
  videoBtn.textContent = 'Video Off';
}

startCall.addEventListener('click', async () => {
  if (startCall.disabled) return;
  startCall.disabled = true;
  try {
    muted = false;
    videoOff = false;
    muteBtn.textContent = 'Mute';
    videoBtn.textContent = 'Video Off';
    await startVideoCall();
    setCallButtons(true);
    callStatus.textContent = 'In Call';
    callStatus.className = 'status-pill busy';
    callSync.textContent = 'Peer connected (local WebRTC loopback)';
    localOnline.textContent = 'Online';
    remoteOnline.textContent = 'Connected';
    callSeconds = 0;
    callTimer.textContent = '00:00';
    clearInterval(callTimerId);
    callTimerId = setInterval(updateCallTimer, 1000);
  } catch (error) {
    endVideoCall();
    resetCallState();
    callStatus.textContent = 'Camera Error';
    callSync.textContent = error.message || 'Unable to access camera/microphone.';
  } finally {
    setCallButtons(Boolean(localStream));
  }
});

endCall.addEventListener('click', () => {
  endVideoCall();
  resetCallState();
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

addMessage(friendHistory, 'Friend', 'Hey! Ready for a quick chat?', false, '• delivered');
resetCallState();

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
