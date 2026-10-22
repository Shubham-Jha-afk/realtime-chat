const $ = (id) => document.getElementById(id);

const state = {
    client: null,
    username: null,
    connected: false,
    current: null,
    subs: [],
    rooms: new Map()
};

function notify(text) {
    const toast = $('toast');
    toast.textContent = text;
    toast.classList.remove('hidden');
    clearTimeout(notify.timer);
    notify.timer = setTimeout(() => toast.classList.add('hidden'), 4000);
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || data.error || 'Request failed');
    }
    return data;
}

function connect(username) {
    state.username = username;
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new StompJs.Client({
        brokerURL: `${scheme}://${location.host}/ws`,
        connectHeaders: { username },
        reconnectDelay: 0,
        onConnect: onConnected,
        onStompError: (frame) => {
            if (state.connected) {
                notify(`Server error: ${frame.headers.message || 'request rejected'}`);
            } else {
                $('login-error').textContent = 'Connection rejected: the username is invalid or already taken.';
            }
        },
        onWebSocketClose: () => {
            if (state.connected) {
                state.connected = false;
                notify('Disconnected. Reload the page to reconnect.');
            } else if (!$('login-error').textContent) {
                $('login-error').textContent = 'Could not reach the server.';
            }
        }
    });
    state.client = client;
    client.activate();
}

function onConnected() {
    state.connected = true;
    $('login').classList.add('hidden');
    $('app').classList.remove('hidden');
    $('me').textContent = `Signed in as ${state.username}`;

    state.client.subscribe('/topic/rooms', (frame) => addRoom(JSON.parse(frame.body)));
    state.client.subscribe('/user/queue/history', (frame) => applySnapshot(JSON.parse(frame.body)));

    loadRooms().then(() => enterRoom('general'));
}

async function loadRooms() {
    const list = await api('/api/rooms');
    list.forEach(addRoom);
}

function addRoom(room) {
    state.rooms.set(room.id, room);
    renderRooms();
}

function renderRooms() {
    const list = $('room-list');
    list.replaceChildren();
    const rooms = [...state.rooms.values()].sort((a, b) => a.name.localeCompare(b.name));
    rooms.forEach((room) => {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = `${room.type === 'PRIVATE' ? '\u{1F512} ' : '# '}${room.name}`;
        if (room.id === state.current) {
            button.classList.add('active');
        }
        button.addEventListener('click', () => enterRoom(room.id));
        item.append(button);
        list.append(item);
    });
}

function leaveRoom() {
    state.subs.forEach((subscription) => subscription.unsubscribe());
    state.subs = [];
}

function enterRoom(id) {
    if (!state.connected || id === state.current) {
        return;
    }
    leaveRoom();
    state.current = id;
    const room = state.rooms.get(id);
    $('room-title').textContent = room ? room.name : id;
    $('room-code').textContent = room && room.type === 'PRIVATE' ? `Invite code: ${room.id}` : '';
    $('messages').replaceChildren();
    $('users').replaceChildren();
    $('message-input').disabled = false;
    $('message-input').focus();

    state.subs.push(state.client.subscribe(`/topic/room.${id}.users`, (frame) => {
        renderUsers(JSON.parse(frame.body).users);
    }));
    state.subs.push(state.client.subscribe(`/topic/room.${id}`, (frame) => {
        appendMessage(JSON.parse(frame.body));
    }));
    renderRooms();
}

function applySnapshot(snapshot) {
    if (snapshot.roomId !== state.current) {
        return;
    }
    $('messages').replaceChildren();
    snapshot.messages.forEach(appendMessage);
    renderUsers(snapshot.users);
}

function renderUsers(users) {
    const list = $('users');
    list.replaceChildren();
    users.forEach((name) => {
        const item = document.createElement('li');
        item.textContent = name;
        list.append(item);
    });
}

function appendMessage(message) {
    const box = $('messages');
    const element = document.createElement('div');
    if (message.type === 'CHAT') {
        element.className = message.sender === state.username ? 'msg mine' : 'msg';
        const meta = document.createElement('div');
        meta.className = 'meta';
        const time = new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        meta.textContent = `${message.sender} \u00B7 ${time}`;
        const text = document.createElement('div');
        text.className = 'text';
        text.textContent = message.content;
        element.append(meta, text);
    } else {
        element.className = 'event';
        element.textContent = message.content;
    }
    box.append(element);
    box.scrollTop = box.scrollHeight;
}

$('login-form').addEventListener('submit', (event) => {
    event.preventDefault();
    $('login-error').textContent = '';
    connect($('username').value.trim());
});

$('send-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const input = $('message-input');
    const content = input.value.trim();
    if (!content || !state.current || !state.connected) {
        return;
    }
    state.client.publish({
        destination: `/app/chat/${state.current}`,
        body: JSON.stringify({ content }),
        headers: { 'content-type': 'application/json' }
    });
    input.value = '';
});

$('create-type').addEventListener('change', (event) => {
    $('create-password').classList.toggle('hidden', event.target.value !== 'PRIVATE');
    $('create-password').required = event.target.value === 'PRIVATE';
});

$('create-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        const room = await api('/api/rooms', {
            method: 'POST',
            body: JSON.stringify({
                name: $('create-name').value.trim(),
                type: $('create-type').value,
                password: $('create-password').value,
                username: state.username
            })
        });
        event.target.reset();
        $('create-password').classList.add('hidden');
        addRoom(room);
        enterRoom(room.id);
    } catch (error) {
        notify(error.message);
    }
});

$('join-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        const code = encodeURIComponent($('join-code').value.trim());
        const room = await api(`/api/rooms/${code}/join`, {
            method: 'POST',
            body: JSON.stringify({
                username: state.username,
                password: $('join-password').value
            })
        });
        event.target.reset();
        addRoom(room);
        enterRoom(room.id);
    } catch (error) {
        notify(error.message);
    }
});
