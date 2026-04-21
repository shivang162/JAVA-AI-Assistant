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
const remoteOnline = document.getElementById('remote-online');

let callSeconds = 0;
let callTimerId = null;
let muted = false;
let videoOff = false;

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

aiForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const question = aiInput.value.trim();
  if (!question) return;

  addMessage(aiHistory, 'You', question, true);
  aiInput.value = '';
  aiLoading.classList.remove('hidden');

  setTimeout(() => {
    aiLoading.classList.add('hidden');
    addMessage(
      aiHistory,
      'AI',
      `I received your question: "${question}". Connect this UI with your backend WebSocket/AI API for real-time answers across devices.`
    );
  }, 1200);
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

startCall.addEventListener('click', () => {
  setCallButtons(true);
  callStatus.textContent = 'In Call';
  callStatus.className = 'status-pill busy';
  remoteOnline.textContent = 'Connected';
  callSeconds = 0;
  callTimer.textContent = '00:00';
  clearInterval(callTimerId);
  callTimerId = setInterval(updateCallTimer, 1000);
});

endCall.addEventListener('click', () => {
  setCallButtons(false);
  callStatus.textContent = 'Idle';
  callStatus.className = 'status-pill idle';
  remoteOnline.textContent = 'Waiting';
  clearInterval(callTimerId);
  muted = false;
  videoOff = false;
  muteBtn.textContent = 'Mute';
  videoBtn.textContent = 'Video Off';
});

muteBtn.addEventListener('click', () => {
  muted = !muted;
  muteBtn.textContent = muted ? 'Unmute' : 'Mute';
});

videoBtn.addEventListener('click', () => {
  videoOff = !videoOff;
  videoBtn.textContent = videoOff ? 'Video On' : 'Video Off';
});

addMessage(aiHistory, 'AI', 'Hi! Ask your question and I will respond in real time.');
addMessage(friendHistory, 'Friend', 'Hey! Ready for a quick chat?', false, '• delivered');
setCallButtons(false);
