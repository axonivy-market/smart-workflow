/**
 * Axon Ivy Info Chat — JavaScript interaction
 */

/**
 * Handles key events in the chat input textarea.
 * Shift+Enter or Cmd+Enter sends the message; plain Enter creates a new line.
 */
function handleInfoChatKeyDown(event) {
  if (event.key === 'Enter') {
    if (event.shiftKey || event.metaKey) {
      event.preventDefault();
      var messageContent = event.target.value.trim();
      if (messageContent === '') {
        return false;
      }
      sendMessage();
      return false;
    }
    return true;
  }
  return true;
}

/**
 * Scrolls the chat messages area to the bottom.
 */
function scrollInfoChatToBottom() {
  setTimeout(function() {
    var chatArea = document.querySelector('[id$="chatArea"]');
    if (chatArea) {
      var scrollableContent = chatArea.querySelector('.ui-scrollpanel-content');
      if (scrollableContent) {
        scrollableContent.scrollTop = scrollableContent.scrollHeight;
      } else {
        chatArea.scrollTop = chatArea.scrollHeight;
      }
    }
  }, 150);
}

/**
 * Validates that the input is not empty, then sends the message.
 */
function sendInfoMessageWithValidation() {
  var inputElement = document.querySelector('[id$="userInput"]');
  if (inputElement) {
    var messageContent = inputElement.value.trim();
    if (messageContent === '') {
      return false;
    }
  }
  if (typeof sendMessage === 'function') {
    sendMessage();
    scrollInfoChatToBottom();
  }
  return false;
}
