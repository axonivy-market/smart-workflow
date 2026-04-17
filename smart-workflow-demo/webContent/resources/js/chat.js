const thinkingPhases = [
  'Searching documents',
  'Analyzing relevant content',
  'Generating response'
];

let thinkingPhaseIndex = 0;
let thinkingInterval = null;

function scrollToBottom() {
  const messagesArea = document.querySelector('.messages-area');
  if (messagesArea) {
    setTimeout(function() {
      messagesArea.scrollTop = messagesArea.scrollHeight;
    }, 100);
  }
}

function disableChatInput() {
  const chatInput = document.querySelector('.message-input');
  const sendBtn = document.querySelector('[id$="send-btn"]');

  if (chatInput) {
    chatInput.disabled = true;
    chatInput.classList.add('disabled-input');
  }
  if (sendBtn) {
    sendBtn.disabled = true;
    sendBtn.classList.add('disabled-btn');
  }
}

function enableChatInput() {
  const chatInput = document.querySelector('.message-input');
  const sendBtn = document.querySelector('[id$="send-btn"]');

  if (chatInput) {
    chatInput.disabled = false;
    chatInput.classList.remove('disabled-input');
  }
  if (sendBtn) {
    sendBtn.disabled = false;
    sendBtn.classList.remove('disabled-btn');
  }
}

function showThinking() {
  const thinkingStatus = document.getElementById('thinking-indicator');
  const thinkingText = document.getElementById('thinking-text');
  const chatInput = document.querySelector('.message-input');

  if (chatInput && chatInput.value.trim()) {
    appendUserMessage(chatInput.value.trim());
  }

  setTimeout(function() {
    if (chatInput) {
      chatInput.value = '';
    }
    disableChatInput();
  }, 50);

  if (thinkingStatus) {
    thinkingStatus.classList.remove('rag-hidden');
  }

  thinkingPhaseIndex = 0;

  if (thinkingText) {
    thinkingText.textContent = thinkingPhases[0] + '...';

    if (thinkingInterval) {
      clearInterval(thinkingInterval);
    }

    thinkingInterval = setInterval(function() {
      thinkingPhaseIndex = (thinkingPhaseIndex + 1) % thinkingPhases.length;
      thinkingText.textContent = thinkingPhases[thinkingPhaseIndex] + '...';
    }, 2000);
  }

  scrollToBottom();
}

function appendUserMessage(content) {
  const chatContent = document.querySelector('.chat-content');
  if (!chatContent) return;

  const welcomeMsg = chatContent.querySelector('.flex.flex-column.align-items-center');
  if (welcomeMsg) {
    welcomeMsg.remove();
  }

  const now = new Date();
  const timestamp = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const messageRow = document.createElement('div');
  messageRow.className = 'user-message-row flex justify-content-end mb-4 temp-user-message';
  messageRow.innerHTML = `
    <div class="user-message">
      <div class="message-bubble sent">${escapeHtml(content)}</div>
      <div class="message-time text-xs text-gray-400 mt-1 text-right">${timestamp}</div>
    </div>
  `;

  const thinkingIndicator = document.getElementById('thinking-indicator');
  if (thinkingIndicator) {
    chatContent.insertBefore(messageRow, thinkingIndicator);
  } else {
    chatContent.appendChild(messageRow);
  }

  scrollToBottom();
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function onChatComplete() {
  const thinkingStatus = document.getElementById('thinking-indicator');
  if (thinkingStatus) {
    thinkingStatus.classList.add('rag-hidden');
  }

  if (thinkingInterval) {
    clearInterval(thinkingInterval);
    thinkingInterval = null;
  }

  document.querySelectorAll('.temp-user-message').forEach(el => el.remove());

  enableChatInput();
  focusChatInput();
  scrollToBottom();
}

function handleChatInputKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();

    const sendBtn = document.querySelector('[id$="send-btn"]');
    const chatInput = document.querySelector('.message-input');

    if (chatInput && chatInput.disabled) {
      return;
    }

    if (sendBtn && !sendBtn.disabled) {
      sendBtn.click();
    }
  }
}

function focusChatInput() {
  const chatInput = document.querySelector('.message-input');
  if (chatInput && !chatInput.disabled) {
    setTimeout(function() {
      chatInput.focus();
    }, 100);
  }
}

function onClearChat() {
  if (thinkingInterval) {
    clearInterval(thinkingInterval);
    thinkingInterval = null;
  }
  thinkingPhaseIndex = 0;

  enableChatInput();
  focusChatInput();
}

$(document).ready(function() {
  scrollToBottom();
  focusChatInput();
});
