var AiChatbot = (function() {
  var TYPEWRITER_CHAR_MS = 12;
  var TYPEWRITER_MAX_CHARS = 150;
  var COPIED_FEEDBACK_MS = 1500;
  // undefined locale = the browser's own locale and time zone
  var TIME_FORMAT = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
  var FULL_FORMAT = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

  function getRoot(clientId) {
    return document.getElementById(clientId);
  }

  function find(clientId, selector) {
    var root = getRoot(clientId);
    return root ? root.querySelector(selector) : null;
  }

  function rootIdOf(element) {
    var root = element.closest('.chatbot');
    return root ? root.id : null;
  }

  function scrollToBottom(clientId) {
    var scroll = find(clientId, '.chatbot-scroll');
    if (!scroll) { return; }
    requestAnimationFrame(function() {
      scroll.scrollTop = scroll.scrollHeight;
    });
  }

  /* the server renders each message time as a UTC instant; show it in the user's time zone */
  function formatTimes(clientId) {
    var root = getRoot(clientId);
    if (!root) { return; }
    root.querySelectorAll('time.chatbot-time[datetime]').forEach(function(time) {
      var date = new Date(time.getAttribute('datetime'));
      if (isNaN(date.getTime())) { return; }
      time.textContent = TIME_FORMAT.format(date);
      time.title = FULL_FORMAT.format(date);
    });
  }

  function autoGrow(input) {
    input.style.height = 'auto';
    input.style.height = input.scrollHeight + 'px';
  }

  function updateSendButton(clientId) {
    var input = find(clientId, '.chatbot-input');
    var send = find(clientId, '.chatbot-send');
    if (!input || !send) { return; }
    var enabled = !input.disabled && input.value.trim().length > 0;
    send.disabled = !enabled;
    send.classList.toggle('ui-state-disabled', !enabled);
  }

  function setBusy(clientId, busy) {
    var root = getRoot(clientId);
    if (root) { root.classList.toggle('chatbot-busy', busy); }
    var input = find(clientId, '.chatbot-input');
    if (input) { input.disabled = busy; }
    // an <a> cannot be disabled; the class greys it out and isBusy() cancels its request
    var newChat = find(clientId, '.chatbot-header-action');
    if (newChat) { newChat.setAttribute('aria-disabled', String(busy)); }
    updateSendButton(clientId);
  }

  function isBusy(clientId) {
    var root = getRoot(clientId);
    return !!root && root.classList.contains('chatbot-busy');
  }

  function showThinking(clientId) {
    var thinking = find(clientId, '.chatbot-thinking');
    if (thinking) { thinking.hidden = false; }
    setBusy(clientId, true);
    scrollToBottom(clientId);
  }

  function requestAnswer(clientId) {
    var root = getRoot(clientId);
    var command = root && root.getAttribute('data-answer-command');
    if (command && typeof window[command] === 'function') {
      window[command]();
    }
  }

  function streamLastAnswer(clientId) {
    var root = getRoot(clientId);
    if (!root || root.querySelector('.chatbot-error')) { return; }
    var messages = root.querySelectorAll('.chatbot-message:not(.chatbot-thinking)');
    var last = messages[messages.length - 1];
    // only animate when the newest message is the answer that just arrived
    if (!last || !last.classList.contains('chatbot-message-ai')) { return; }

    var content = last.querySelector('.chatbot-answer');
    if (!content) { return; }
    var meta = last.querySelector('.chatbot-answer-time');
    var text = content.textContent || '';
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) { return; }

    if (meta) { meta.classList.add('chatbot-time-hidden'); }
    content.textContent = '';
    var typed = document.createTextNode('');
    var cursor = document.createElement('span');
    cursor.className = 'chatbot-streaming-cursor';
    content.appendChild(typed);
    content.appendChild(cursor);

    // only the start is typed out, so long answers do not keep the user waiting;
    // Array.from splits by code point, so emoji are never cut in half
    var head = Array.from(text).slice(0, TYPEWRITER_MAX_CHARS);
    var index = 0;
    function typeNext() {
      if (index < head.length) {
        typed.data += head[index];
        index++;
        setTimeout(typeNext, TYPEWRITER_CHAR_MS);
      } else {
        typed.data = text;
        cursor.remove();
        if (meta) { meta.classList.remove('chatbot-time-hidden'); }
      }
    }
    typeNext();
  }

  function focusInput(clientId) {
    var input = find(clientId, '.chatbot-input');
    if (input) { input.focus(); }
  }

  /* callbacks wired in Chatbot.xhtml */

  function afterSend(clientId, args) {
    // the server rejected the message (e.g. too long): keep the text so the user can fix it
    if (args && args.accepted === false) {
      formatTimes(clientId);
      scrollToBottom(clientId);
      focusInput(clientId);
      return;
    }
    var input = find(clientId, '.chatbot-input');
    if (input) {
      input.value = '';
      autoGrow(input);
    }
    formatTimes(clientId);
    showThinking(clientId);
    requestAnswer(clientId);
  }

  function afterRetry(clientId) {
    formatTimes(clientId);
    showThinking(clientId);
    requestAnswer(clientId);
  }

  function afterAnswer(clientId) {
    formatTimes(clientId);
    setBusy(clientId, false);
    streamLastAnswer(clientId);
    focusInput(clientId);
    scrollToBottom(clientId);
  }

  function reset(clientId) {
    var input = find(clientId, '.chatbot-input');
    if (input) {
      input.value = '';
      autoGrow(input);
    }
    setBusy(clientId, false);
    focusInput(clientId);
  }

  function handleKeyDown(event) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing) { return; }
    // Enter sends; it must never submit the host form
    event.preventDefault();
    var clientId = rootIdOf(event.target);
    var send = clientId && find(clientId, '.chatbot-send');
    if (send && !send.disabled) { send.click(); }
  }

  function handleInput(event) {
    autoGrow(event.target);
    var clientId = rootIdOf(event.target);
    if (clientId) { updateSendButton(clientId); }
  }

  function copyAnswer(button) {
    var message = button.closest('.chatbot-message');
    var content = message && message.querySelector('.chatbot-answer');
    var icon = button.querySelector('.ti');
    if (!content || !navigator.clipboard) { return; }
    navigator.clipboard.writeText(content.textContent || '').then(function() {
      if (!icon) { return; }
      icon.classList.replace('ti-copy', 'ti-check');
      setTimeout(function() { icon.classList.replace('ti-check', 'ti-copy'); }, COPIED_FEEDBACK_MS);
    }).catch(function() {});
  }

  /* applies the minHeight / maxHeight attributes of the component; empty values leave the CSS default */
  function applySize(clientId) {
    var root = getRoot(clientId);
    if (!root) { return; }
    root.style.minHeight = root.getAttribute('data-min-height') || '';
    root.style.maxHeight = root.getAttribute('data-max-height') || '';
  }

  /* called by Chatbot.xhtml on every render; safe to call repeatedly */
  function init(clientId) {
    var input = find(clientId, '.chatbot-input');
    if (!input) { return; }
    applySize(clientId);
    autoGrow(input);
    formatTimes(clientId);
    updateSendButton(clientId);
    scrollToBottom(clientId);
  }

  return {
    afterAnswer: afterAnswer,
    afterRetry: afterRetry,
    afterSend: afterSend,
    copyAnswer: copyAnswer,
    handleInput: handleInput,
    handleKeyDown: handleKeyDown,
    init: init,
    isBusy: isBusy,
    reset: reset
  };
}());

// components rendered before this script loaded queued their ids (see Chatbot.xhtml)
(window.AiChatbotPending || []).forEach(function(clientId) { AiChatbot.init(clientId); });
window.AiChatbotPending = [];
