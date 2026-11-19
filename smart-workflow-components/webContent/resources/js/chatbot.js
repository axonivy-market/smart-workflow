var AiChatbot = (function() {
  var TYPEWRITER_CHAR_MS = 12;
  var TYPEWRITER_MAX_CHARS = 150;
  var COPIED_FEEDBACK_MS = 1500;

  var TIME_FORMAT = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
  var FULL_TIME_FORMAT = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

  /* an ajax update replaces these elements, so look them up on each call and never keep them */
  function findParts(clientId) {
    var root = document.getElementById(clientId);
    return root && {
      root: root,
      input: root.querySelector('.chatbot-input'),
      send: root.querySelector('.chatbot-send'),
      thinking: root.querySelector('.chatbot-thinking'),
      requestError: root.querySelector('.chatbot-request-error'),
      newChat: root.querySelector('.chatbot-header-action'),
      scroll: root.querySelector('.chatbot-scroll')
    };
  }

  /* turns a function of the parts into a callback for Chatbot.xhtml, which only knows the client id */
  function withChat(handler) {
    return function(clientId, args) {
      var chat = findParts(clientId);
      return chat && handler(chat, args);
    };
  }

  /* called by Chatbot.xhtml on every render; safe to call repeatedly */
  function init(chat) {
    autoGrow(chat.input);
    updateSendButton(chat);
    refreshThread(chat);
  }

  /* request flow */

  function afterSend(chat, args) {
    // rejected as blank, or the request failed and args is undefined: keep the text in the input
    if (args?.accepted !== true) {
      refreshThread(chat);
      chat.input.focus();
      return;
    }
    clearInput(chat);
    waitForAnswer(chat);
  }

  function waitForAnswer(chat) {
    chat.requestError.hidden = true;
    setBusy(chat, true);
    refreshThread(chat);
    window[chat.root.getAttribute('data-answer-command')]?.();
  }

  function afterAnswer(chat) {
    setBusy(chat, false);
    autoGrow(chat.input);
    refreshThread(chat);
    typeOutNewAnswer(chat);
    chat.input.focus();
  }

  function requestFailed(chat) {
    chat.requestError.hidden = false;
  }

  function retryRequest(chat) {
    // a failed send kept its text in the input, which enables Send; a failed answer left the input empty
    if (chat.send.disabled) {
      waitForAnswer(chat);
    } else {
      chat.send.click();
    }
  }

  function afterRetry(chat) {
    waitForAnswer(chat);
  }

  function reset(chat) {
    clearInput(chat);
    setBusy(chat, false);
    chat.input.focus();
  }

  function setBusy(chat, busy) {
    chat.root.classList.toggle('chatbot-busy', busy);
    chat.thinking.hidden = !busy;
    chat.input.disabled = busy;
    // an <a> cannot be disabled; the class greys it out and isBusy() cancels its request
    chat.newChat.setAttribute('aria-disabled', String(busy));
    updateSendButton(chat);
  }

  function isBusy(chat) {
    return chat.root.classList.contains('chatbot-busy');
  }

  /* message box */

  function handleKeyDown(event, clientId) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing) { return; }
    // Enter sends; it must never submit the host form
    event.preventDefault();
    var chat = findParts(clientId);
    if (chat && !chat.send.disabled) { chat.send.click(); }
  }

  function handleInput(event, clientId) {
    autoGrow(event.target);
    var chat = findParts(clientId);
    if (chat) { updateSendButton(chat); }
  }

  function autoGrow(input) {
    input.style.height = 'auto';
    input.style.height = input.scrollHeight + 'px';
  }

  function clearInput(chat) {
    chat.input.value = '';
    autoGrow(chat.input);
  }

  function updateSendButton(chat) {
    var enabled = !chat.input.disabled && chat.input.value.trim().length > 0;
    chat.send.disabled = !enabled;
    chat.send.classList.toggle('ui-state-disabled', !enabled);
  }

  /* message thread */

  /* every update of the thread brings the times back unformatted */
  function refreshThread(chat) {
    formatTimes(chat);
    scrollToBottom(chat);
  }

  /* the server renders each message time as a UTC instant; show it in the user's time zone */
  function formatTimes(chat) {
    chat.root.querySelectorAll('time.chatbot-time[datetime]').forEach(function(time) {
      var date = new Date(time.getAttribute('datetime'));
      if (isNaN(date.getTime())) { return; }
      time.textContent = TIME_FORMAT.format(date);
      time.title = FULL_TIME_FORMAT.format(date);
    });
  }

  function scrollToBottom(chat) {
    requestAnimationFrame(function() {
      chat.scroll.scrollTop = chat.scroll.scrollHeight;
    });
  }

  /* answer animation */

  function typeOutNewAnswer(chat) {
    var answer = findNewAnswer(chat);
    if (answer && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      typeOut(answer);
    }
  }

  /* only animate when the newest message is the answer that just arrived */
  function findNewAnswer(chat) {
    if (chat.root.querySelector('.chatbot-error')) { return null; }
    var messages = chat.root.querySelectorAll('.chatbot-message:not(.chatbot-thinking)');
    var last = messages[messages.length - 1];
    return last?.classList.contains('chatbot-message-ai') ? last : null;
  }

  function typeOut(message) {
    var content = message.querySelector('.chatbot-answer');
    var text = content.textContent;

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
        typed.textContent += head[index];
        index++;
        setTimeout(typeNext, TYPEWRITER_CHAR_MS);
      } else {
        typed.textContent = text;
        cursor.remove();
      }
    }
    typeNext();
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

  return {
    afterAnswer: withChat(afterAnswer),
    afterRetry: withChat(afterRetry),
    afterSend: withChat(afterSend),
    copyAnswer: copyAnswer,
    handleInput: handleInput,
    handleKeyDown: handleKeyDown,
    init: withChat(init),
    isBusy: withChat(isBusy),
    requestFailed: withChat(requestFailed),
    reset: withChat(reset),
    retryRequest: withChat(retryRequest)
  };
}());

// components rendered before this script loaded queued their ids)
(window.AiChatbotPending || []).forEach(function(clientId) { AiChatbot.init(clientId); });
window.AiChatbotPending = [];
