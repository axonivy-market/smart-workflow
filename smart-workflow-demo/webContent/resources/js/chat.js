const thinkingPhases = [
  'Searching documents',
  'Analyzing relevant content',
  'Generating response'
];

let thinkingPhaseIndex = 0;
let thinkingInterval = null;
let streamingInProgress = false;

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

  // Hide unrendered response immediately to prevent flash before streaming begins
  document.querySelectorAll('.markdown-content:not(.markdown-rendered)').forEach(function(el) {
    el.style.visibility = 'hidden';
  });

  setTimeout(function() {
    renderMarkdownWithStreaming();
    scrollToBottom();
  }, 150);
}

function renderMarkdownWithStreaming() {
  const markdownElements = document.querySelectorAll('.markdown-content');

  markdownElements.forEach(function(element, index) {
    if (element.classList.contains('markdown-rendered')) {
      return;
    }

    const rawContent = element.textContent || element.innerText;

    if (!rawContent || rawContent.trim() === '') {
      return;
    }

    try {
      if (typeof marked !== 'undefined' && marked.parse) {
        marked.setOptions({
          breaks: true,
          gfm: true
        });

        const htmlContent = marked.parse(rawContent);
        const isLastMessage = index === markdownElements.length - 1;

        if (isLastMessage && !streamingInProgress) {
          streamText(element, htmlContent);
        } else {
          element.innerHTML = htmlContent;
          element.classList.add('markdown-rendered');
          element.style.visibility = '';
        }
      }
    } catch (error) {
      console.warn('Markdown rendering error:', error);
      element.innerHTML = rawContent;
      element.classList.add('markdown-rendered');
      element.style.visibility = '';
      enableChatInput();
      focusChatInput();
    }
  });

  if (markdownElements.length === 0) {
    enableChatInput();
    focusChatInput();
  }
}

function streamText(element, htmlContent) {
  streamingInProgress = true;
  element.classList.add('markdown-rendered', 'streaming');
  element.innerHTML = '';
  element.style.visibility = '';

  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = htmlContent;

  const textContent = tempDiv.textContent || tempDiv.innerText;
  const words = textContent.split(/(\s+)/);
  const streamLimit = 100;
  let currentIndex = 0;
  const streamSpeed = 20;

  const streamContainer = document.createElement('div');
  streamContainer.className = 'stream-content';
  element.appendChild(streamContainer);

  function streamNextWord() {
    if (currentIndex < words.length && currentIndex < streamLimit) {
      streamContainer.textContent += words[currentIndex];
      currentIndex++;

      if (currentIndex % 5 === 0) {
        scrollToBottom();
      }

      setTimeout(streamNextWord, streamSpeed);
    } else {
      setTimeout(function() {
        element.innerHTML = htmlContent;
        element.classList.remove('streaming');
        element.classList.add('stream-complete');
        streamingInProgress = false;

        enableChatInput();
        focusChatInput();
        scrollToBottom();
      }, 100);
    }
  }

  streamNextWord();
}

function renderAllMarkdown() {
  const markdownElements = document.querySelectorAll('.markdown-content');

  markdownElements.forEach(function(element) {
    if (element.classList.contains('markdown-rendered')) {
      return;
    }

    const rawContent = element.textContent || element.innerText;

    if (!rawContent || rawContent.trim() === '') {
      return;
    }

    try {
      if (typeof marked !== 'undefined' && marked.parse) {
        marked.setOptions({
          breaks: true,
          gfm: true
        });

        element.innerHTML = marked.parse(rawContent);
        element.classList.add('markdown-rendered');
      }
    } catch (error) {
      console.warn('Markdown rendering error:', error);
    }
  });
}

function handleChatInputKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();

    const sendBtn = document.querySelector('[id$="send-btn"]');
    const chatInput = document.querySelector('.message-input');

    if (streamingInProgress || (chatInput && chatInput.disabled)) {
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
  streamingInProgress = false;

  enableChatInput();
  focusChatInput();
}

function loadMarkedLibrary(callback) {
  if (typeof marked !== 'undefined') {
    if (callback) callback();
    return;
  }

  const script = document.createElement('script');
  script.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
  script.onload = function() {
    console.log('Marked library loaded');
    if (callback) callback();
  };
  script.onerror = function() {
    console.error('Failed to load marked library');
  };
  document.head.appendChild(script);
}

$(document).ready(function() {
  loadMarkedLibrary(function() {
    renderAllMarkdown();
    scrollToBottom();
    focusChatInput();
  });
});
