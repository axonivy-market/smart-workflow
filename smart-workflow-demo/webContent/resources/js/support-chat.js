/**
 * Support Chat JavaScript Functions
 */

/**
 * Handles key press events in the chat input textarea
 * - Enter: Creates new line
 * - Shift+Enter (or Cmd+Enter on Mac): Sends message
 * @param {Event} event - The keypress event
 * @param {string} sendBtnId - The ID of the send button to trigger
 */
function handleChatInputKeyPress(event, sendBtnId) {
    // Check if Enter key is pressed (keyCode 13)
    if (event.keyCode === 13) {
        // Check for Shift+Enter or Cmd+Enter (Mac compatibility)
        if (event.shiftKey || event.metaKey) {
            // Shift+Enter or Cmd+Enter: Send message
            event.preventDefault(); // Prevent default new line behavior
            
            // Trigger the send button click
            if (typeof PF !== 'undefined' && PF(sendBtnId)) {
                PF(sendBtnId).jq.click();
            } else {
                // Fallback: try to find button by ID and click it
                var sendBtn = document.getElementById(sendBtnId);
                if (sendBtn) {
                    sendBtn.click();
                }
            }
            return false;
        }
        // Just Enter: Allow default behavior (new line)
        return true;
    }
    // For all other keys, allow default behavior
    return true;
}

/**
 * Alternative function using modern event handling
 * @param {Event} event - The keydown event
 */
function handleChatInputKeyDown(event) {
    // Check if Enter key is pressed
    if (event.key === 'Enter') {
        // Check for Shift+Enter or Cmd+Enter (Mac compatibility)
        if (event.shiftKey || event.metaKey) {
            // Shift+Enter or Cmd+Enter: Send message
            event.preventDefault(); // Prevent default new line behavior

            // Check if the trimmed content is not blank before sending
            var inputElement = event.target;
            var messageContent = inputElement.value.trim();
            
            if (messageContent === '') {
                // Don't send if message is empty or only whitespace
                return false;
            }

            // call remote command send message
            sendMessage();
            return false;
        }
        // Just Enter: Allow default behavior (new line)
        return true;
    }
    // For all other keys, allow default behavior
    return true;
}

/**
 * Scrolls the chat area to the bottom to show the latest messages
 */
function scrollChatToBottom() {
    // Use setTimeout to ensure the DOM is updated before scrolling
    setTimeout(function() {
        // Try to find the chat area scroll panel
        var chatArea = document.querySelector('[id$="chatArea"]');
        if (chatArea) {
            // Find the scrollable content within the panel
            var scrollableContent = chatArea.querySelector('.ui-scrollpanel-content');
            if (scrollableContent) {
                scrollableContent.scrollTop = scrollableContent.scrollHeight;
            } else {
                // Fallback: scroll the chat area itself
                chatArea.scrollTop = chatArea.scrollHeight;
            }
        }
    }, 100); // Small delay to ensure content is rendered
}

/**
 * Enhanced send message function that also checks for blank content
 */
function sendMessageWithValidation() {
    // Get the user input element
    var inputElement = document.querySelector('[id$="userInput"]');
    if (inputElement) {
        var messageContent = inputElement.value.trim();
        
        if (messageContent === '') {
            // Don't send if message is empty or only whitespace
            return false;
        }
    }
    
    // Call the original sendMessage remote command
    if (typeof sendMessage === 'function') {
        sendMessage();
        // Scroll to bottom after sending
        scrollChatToBottom();
    }
    
    return false;
}