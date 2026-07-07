var ErpChatbot = (function() {

  var SCROLL_DELAY_MS      = 100;
  var INPUT_FOCUS_DELAY_MS = 100;
  var TYPEWRITER_CHAR_MS   = 15;
  var THINKING_CYCLE_MS    = 2000;

  var _thinkingInterval = null;
  var _thinkingIndex    = 0;
  var _thinkingPhrases  = [
    'Searching knowledge base',
    'Analyzing supplier context',
    'Generating response'
  ];

  function _scrollToBottom(clientId) {
    var messagesArea = document.getElementById(clientId + '-messages-area');
    if (!messagesArea) { return; }
    setTimeout(function() {
      messagesArea.scrollTop = messagesArea.scrollHeight;
    }, SCROLL_DELAY_MS);
  }

  function _getWrapper(clientId) {
    return document.getElementById(clientId);
  }

  function _setInputState(clientId, disabled) {
    var wrapper = _getWrapper(clientId);
    var input   = wrapper ? wrapper.querySelector('.message-input') : null;
    var sendBtn = wrapper ? wrapper.querySelector('[id$="assistant-send"]') : null;
    var chips   = wrapper ? wrapper.querySelectorAll('.so-ai-chip') : [];
    if (input) {
      input.disabled = disabled;
      if (disabled) { input.classList.add('disabled-input'); }
      else          { input.classList.remove('disabled-input'); }
    }
    if (sendBtn) {
      sendBtn.disabled = disabled;
      if (disabled) { sendBtn.classList.add('disabled-btn'); }
      else          { sendBtn.classList.remove('disabled-btn'); }
    }
    chips.forEach(function(chip) {
      if (disabled) { chip.classList.add('disabled-chip'); }
      else          { chip.classList.remove('disabled-chip'); }
    });
  }

  function _focusInput(clientId) {
    var wrapper = _getWrapper(clientId);
    var input   = wrapper ? wrapper.querySelector('.message-input') : null;
    if (input) { setTimeout(function() { input.focus(); }, INPUT_FOCUS_DELAY_MS); }
  }

  function _animateLastUserMessage(clientId) {
    var wrapper = _getWrapper(clientId);
    if (!wrapper) { return; }
    var rows = wrapper.querySelectorAll('.user-message-row:not(.so-ai-message-enter)');
    if (rows.length === 0) { return; }
    rows[rows.length - 1].classList.add('so-ai-message-enter');
  }

  function _streamLastMessage(clientId) {
    var wrapper = _getWrapper(clientId);
    if (!wrapper) { return; }
    var rows = wrapper.querySelectorAll('.assistant-message-row:not(.thinking-status):not(.so-ai-streamed)');
    if (rows.length === 0) { return; }
    var lastRow = rows[rows.length - 1];
    lastRow.classList.add('so-ai-streamed', 'so-ai-assistant-enter');

    var messageContent = lastRow.querySelector('.message-content');
    if (!messageContent) { return; }

    var timeEl = lastRow.querySelector('.message-time');
    if (timeEl) { timeEl.classList.add('so-ai-time-hidden'); }

    var fullText = messageContent.textContent || '';
    messageContent.textContent = '';

    var cursor = document.createElement('span');
    cursor.className = 'so-ai-streaming-cursor';
    messageContent.appendChild(cursor);

    var charIndex = 0;

    function typeNext() {
      if (charIndex < fullText.length) {
        cursor.insertAdjacentText('beforebegin', fullText[charIndex]);
        charIndex++;
        setTimeout(typeNext, TYPEWRITER_CHAR_MS);
      } else {
        cursor.remove();
        if (timeEl) { timeEl.classList.remove('so-ai-time-hidden'); }
      }
    }
    typeNext();
  }

  function showThinking(clientId) {
    var thinkingIndicator = document.getElementById(clientId + '-thinking');
    if (thinkingIndicator) { thinkingIndicator.classList.remove('hidden'); }

    _thinkingIndex = 0;
    var thinkingText = document.getElementById(clientId + '-thinking-text');
    if (thinkingText) {
      thinkingText.textContent = _thinkingPhrases[0] + '...';
      if (_thinkingInterval) { clearInterval(_thinkingInterval); }
      _thinkingInterval = setInterval(function() {
        _thinkingIndex = (_thinkingIndex + 1) % _thinkingPhrases.length;
        thinkingText.textContent = _thinkingPhrases[_thinkingIndex] + '...';
      }, THINKING_CYCLE_MS);
    }

    _setInputState(clientId, true);
    _animateLastUserMessage(clientId);
    _scrollToBottom(clientId);
  }

  function afterAnswer(clientId) {
    if (_thinkingInterval) {
      clearInterval(_thinkingInterval);
      _thinkingInterval = null;
    }
    _setInputState(clientId, false);
    _streamLastMessage(clientId);
    _focusInput(clientId);
    _scrollToBottom(clientId);
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (!event.target.value || !event.target.value.trim()) { return; }
      PF('assistantSendCmd').jq.click();
    }
  }

  function chipClick(chip) {
    var question  = chip.getAttribute('data-question').trim();
    var wrapper   = chip.closest('.so-ai-chat-wrapper');
    var chipInput = wrapper ? wrapper.querySelector('[id$="assistant-chip-question"]') : null;
    if (chipInput) {
      chipInput.value = question;
    }
    assistantChipSend();
  }

  return {
    showThinking:  showThinking,
    afterAnswer:   afterAnswer,
    handleKeyDown: handleKeyDown,
    chipClick:     chipClick
  };
}());
