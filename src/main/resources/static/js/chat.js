// Global variables
let stompClient = null;
let chatId = null;
let isTyping = false;
window.customerName = "";
window.customerEmail = "";

// Initialize chat
function initChat() {
  connect();
  loadCategories();
  setupEventListeners();
}

// WebSocket connection
function connect() {
  const socket = new SockJS("/ws/chat");
  stompClient = Stomp.over(socket);

  stompClient.connect(
    {},
    function (frame) {
      console.log("Connected: " + frame);
      updateConnectionStatus("Connected", "connected");

      // Show customer info form when connected
      showCustomerInfoForm();

      // Subscribe to messages
      stompClient.subscribe("/topic/chat", function (message) {
        const messageData = JSON.parse(message.body);
        console.log("Received message:", messageData);
        displayMessage(messageData);
      });
    },
    function (error) {
      console.error("Connection error:", error);
      updateConnectionStatus("Disconnected", "disconnected");
    }
  );
}

// Update connection status
function updateConnectionStatus(message, className) {
  const statusDiv = document.getElementById("connectionStatus");
  statusDiv.textContent = message;
  statusDiv.className = `connection-status ${className}`;
}

// Show customer info form
function showCustomerInfoForm() {
  const chatMessages = document.getElementById("chatMessages");

  if (document.getElementById("customerInfoForm")) {
    return;
  }

  const formDiv = document.createElement("div");
  formDiv.id = "customerInfoForm";
  formDiv.className = "message system";

  formDiv.innerHTML = `
          <div class="message-bubble">
              <h4>Contact Information (Optional)</h4>
              <p>Help us assist you better by providing your contact details:</p>
              <div class="form-row">
                  <div class="form-group">
                      <input type="text" id="customerName" placeholder="Your name (optional)" />
                  </div>
                  <div class="form-group">
                      <input type="email" id="customerEmail" placeholder="your.email@example.com (optional)" />
                  </div>
                  <button onclick="updateCustomerInfo()" class="update-info-btn">Update Info</button>
              </div>
          </div>
      `;

  chatMessages.appendChild(formDiv);
  scrollToBottom();
}

// Update customer info
function updateCustomerInfo() {
  const customerName = document.getElementById("customerName").value.trim();
  const customerEmail = document.getElementById("customerEmail").value.trim();

  window.customerName = customerName;
  window.customerEmail = customerEmail;

  const form = document.getElementById("customerInfoForm");
  const successMsg = document.createElement("div");
  successMsg.className = "success-message";
  successMsg.textContent = "Contact information updated!";

  // Добавляем сообщение после формы, а не внутрь неё
  form.parentNode.insertBefore(successMsg, form.nextSibling);

  setTimeout(() => {
    if (successMsg.parentNode) {
      successMsg.parentNode.removeChild(successMsg);
    }
  }, 3000);
}

// Send message
function sendMessage() {
  const messageInput = document.getElementById("messageInput");
  const categorySelect = document.getElementById("categorySelect");
  const message = messageInput.value.trim();
  const category = categorySelect.value;

  if (message && stompClient) {
    const chatRequest = {
      type: "MESSAGE",
      chatId: chatId,
      message: message,
      category: category,
      customerName: window.customerName,
      customerEmail: window.customerEmail,
    };

    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatRequest));
    messageInput.value = "";

    // Show typing indicator
    showTypingIndicator();
  }
}

// Display message
function displayMessage(messageData) {
  const chatMessages = document.getElementById("chatMessages");
  const messageDiv = document.createElement("div");

  // Hide typing indicator
  hideTypingIndicator();

  if (messageData.type === "CHAT_CREATED") {
    chatId = messageData.chatId;
    messageDiv.className = "message system";
    messageDiv.innerHTML = `
              <div class="message-bubble">
                  Chat created: ${messageData.content}
                  <div class="message-time">${formatTime(
                    messageData.timestamp
                  )}</div>
              </div>
          `;
  } else if (messageData.type === "MESSAGE") {
    const senderClass = messageData.sender.toLowerCase();
    messageDiv.className = `message ${senderClass}`;

    // Рейтинг показывается только в интерфейсе поддержки, не в чате с пользователем
    let ratingHtml = "";

    let suggestionsHtml = "";
    if (
      messageData.sender === "AI" &&
      messageData.suggestions &&
      messageData.suggestions.length > 0
    ) {
      suggestionsHtml = createSuggestionsHtml(messageData.suggestions);
    }

    // Убираем [DATA_SOURCE] из контента сообщения
    const cleanContent = messageData.content.replace(/\[DATA_SOURCE\]/g, "");

    messageDiv.innerHTML = `
              <div class="message-bubble">
                  ${cleanContent}
                  <div class="message-time">${formatTime(
                    messageData.timestamp
                  )}</div>
                  ${ratingHtml}
                  ${suggestionsHtml}
              </div>
          `;
  } else if (messageData.type === "ESCALATION") {
    messageDiv.className = "message system";
    messageDiv.innerHTML = `
              <div class="message-bubble">
                  ${messageData.content}
                  <div class="message-time">${formatTime(
                    messageData.timestamp
                  )}</div>
              </div>
          `;
  } else if (messageData.type === "ERROR") {
    messageDiv.className = "message system";
    messageDiv.innerHTML = `
              <div class="message-bubble" style="background: #3a1a1a; color: #ff6b6b; border: 1px solid #f44336;">
                  ${messageData.content}
                  <div class="message-time">${formatTime(
                    messageData.timestamp
                  )}</div>
              </div>
          `;
  }

  chatMessages.appendChild(messageDiv);
  scrollToBottom();
}

// Рейтинг отображается только в интерфейсе поддержки

// Create suggestions HTML
function createSuggestionsHtml(suggestions) {
  if (!suggestions || suggestions.length === 0) return "";

  const suggestionsList = suggestions
    .map((suggestion) => {
      // Убираем [DATA_SOURCE] из предложений
      const cleanSuggestion = suggestion.replace(/\[DATA_SOURCE\]/g, "");
      return `<div class="suggestion-item" onclick="sendSuggestion('${cleanSuggestion.replace(
        /'/g,
        "\\'"
      )}')">${cleanSuggestion}</div>`;
    })
    .join("");

  return `
          <div class="assistant-suggestions">
              <div class="suggestions-title">💡 Related questions:</div>
              <div class="suggestions-list">
                  ${suggestionsList}
              </div>
          </div>
      `;
}

// Send suggestion as message
function sendSuggestion(suggestion) {
  document.getElementById("messageInput").value = suggestion;
  sendMessage();
}

// Show typing indicator
function showTypingIndicator() {
  if (!isTyping) {
    isTyping = true;
    document.getElementById("typingIndicator").classList.add("show");
    scrollToBottom();
  }
}

// Hide typing indicator
function hideTypingIndicator() {
  if (isTyping) {
    isTyping = false;
    document.getElementById("typingIndicator").classList.remove("show");
  }
}

// Escalate to support
function escalateToSupport() {
  if (stompClient && chatId) {
    const chatRequest = {
      type: "ESCALATION",
      chatId: chatId,
      message: "Customer requested escalation to support.",
    };
    stompClient.send("/app/chat.escalate", {}, JSON.stringify(chatRequest));
    alert("Your chat has been escalated to support!");
  } else {
    alert("Please start a chat before escalating.");
  }
}

// Load categories
function loadCategories() {
  fetch("/api/categories")
    .then((response) => response.json())
    .then((categories) => {
      const categorySelect = document.getElementById("categorySelect");
      categorySelect.innerHTML =
        '<option value="">Select a category...</option>';
      categories.forEach((category) => {
        const option = document.createElement("option");
        option.value = category;
        option.textContent = category;
        categorySelect.appendChild(option);
      });
    })
    .catch((error) => {
      console.error("Error loading categories:", error);
    });
}

// Setup event listeners
function setupEventListeners() {
  const messageInput = document.getElementById("messageInput");
  const sendButton = document.getElementById("sendButton");

  messageInput.addEventListener("keypress", function (e) {
    if (e.key === "Enter") {
      sendMessage();
    }
  });

  sendButton.addEventListener("click", sendMessage);

  // Enable input when connected
  messageInput.disabled = false;
  sendButton.disabled = false;
}

// Utility functions
function formatTime(timestamp) {
  if (!timestamp) return "";
  const date = new Date(timestamp);
  return date.toLocaleTimeString();
}

function scrollToBottom() {
  const chatMessages = document.getElementById("chatMessages");
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Initialize when page loads
document.addEventListener("DOMContentLoaded", initChat);
