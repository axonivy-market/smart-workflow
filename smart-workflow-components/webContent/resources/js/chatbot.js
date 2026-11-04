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
    return getRoot(clientId)?.querySelector(selector);
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
    getRoot(clientId)?.querySelectorAll('time.chatbot-time[datetime]').forEach(function(time) {
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

  function clearInput(clientId) {
    var input = find(clientId, '.chatbot-input');
    if (input) {
      input.value = '';
      autoGrow(input);
    }
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
    getRoot(clientId)?.classList.toggle('chatbot-busy', busy);
    var input = find(clientId, '.chatbot-input');
    if (input) { input.disabled = busy; }
    // an <a> cannot be disabled; the class greys it out and isBusy() cancels its request
    find(clientId, '.chatbot-header-action')?.setAttribute('aria-disabled', String(busy));
    updateSendButton(clientId);
  }

  function isBusy(clientId) {
    return !!getRoot(clientId)?.classList.contains('chatbot-busy');
  }

  function showThinking(clientId) {
    var thinking = find(clientId, '.chatbot-thinking');
    if (thinking) { thinking.hidden = false; }
    setBusy(clientId, true);
    scrollToBottom(clientId);
  }

  function requestAnswer(clientId) {
    var command = getRoot(clientId)?.getAttribute('data-answer-command');
    window[command]?.();
  }

  function waitForAnswer(clientId) {
    formatTimes(clientId);
    showThinking(clientId);
    requestAnswer(clientId);
  }

  function streamLastAnswer(clientId) {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { return; }
    var root = getRoot(clientId);
    if (!root || root.querySelector('.chatbot-error')) { return; }
    var messages = root.querySelectorAll('.chatbot-message:not(.chatbot-thinking)');
    var last = messages[messages.length - 1];
    // only animate when the newest message is the answer that just arrived
    if (!last?.classList.contains('chatbot-message-ai')) { return; }

    var content = last.querySelector('.chatbot-answer');
    var meta = last.querySelector('.chatbot-answer-time');
    var text = content.textContent;

    meta.classList.add('chatbot-time-hidden');
    var typed = document.createTextNode('');
    var cursor = document.createElement('span');
    cursor.className = 'chatbot-streaming-cursor';
    content.replaceChildren(typed, cursor);

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
        meta.classList.remove('chatbot-time-hidden');
      }
    }
    typeNext();
  }

  function focusInput(clientId) {
    find(clientId, '.chatbot-input')?.focus();
  }

  /* callbacks wired in Chatbot.xhtml */

  function afterSend(clientId, args) {
    // the server rejected the message as blank: keep the text in the input
    if (args?.accepted === false) {
      formatTimes(clientId);
      scrollToBottom(clientId);
      focusInput(clientId);
      return;
    }
    clearInput(clientId);
    waitForAnswer(clientId);
  }

  function afterAnswer(clientId) {
    formatTimes(clientId);
    setBusy(clientId, false);
    streamLastAnswer(clientId);
    focusInput(clientId);
    scrollToBottom(clientId);
  }

  function reset(clientId) {
    clearInput(clientId);
    setBusy(clientId, false);
    focusInput(clientId);
  }

  function handleKeyDown(event, clientId) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing) { return; }
    // Enter sends; it must never submit the host form
    event.preventDefault();
    var send = find(clientId, '.chatbot-send');
    if (send && !send.disabled) { send.click(); }
  }

  function handleInput(event, clientId) {
    autoGrow(event.target);
    updateSendButton(clientId);
  }

  function copyAnswer(button) {
    var content = button.closest('.chatbot-message').querySelector('.chatbot-answer');
    var icon = button.querySelector('.ti');
    if (!navigator.clipboard) { return; }
    navigator.clipboard.writeText(content.textContent).then(function() {
      icon.classList.replace('ti-copy', 'ti-check');
      setTimeout(function() { icon.classList.replace('ti-check', 'ti-copy'); }, COPIED_FEEDBACK_MS);
    }).catch(function() {});
  }

  /* called by Chatbot.xhtml on every render; safe to call repeatedly */
  function init(clientId) {
    var input = find(clientId, '.chatbot-input');
    if (!input) { return; }
    autoGrow(input);
    formatTimes(clientId);
    updateSendButton(clientId);
    scrollToBottom(clientId);
  }

  return {
    afterAnswer: afterAnswer,
    afterRetry: waitForAnswer,
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
