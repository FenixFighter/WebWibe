// Global variables
let stompClient = null;
let currentUser = null;
let currentChatId = null;
let allChats = [];
let filteredChats = [];

// Initialize dashboard
function initDashboard() {
  setupEventListeners();
}

// Setup event listeners
function setupEventListeners() {
  document.getElementById("loginForm").addEventListener("submit", handleLogin);
  document
    .getElementById("messageInput")
    .addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        sendMessage();
      }
    });
  document.getElementById("sendButton").addEventListener("click", sendMessage);

  // Filter event listeners
  document
    .getElementById("statusFilter")
    .addEventListener("change", applyFilters);
  document
    .getElementById("ratingFilter")
    .addEventListener("change", applyFilters);
  document
    .getElementById("nameFilter")
    .addEventListener("change", applyFilters);
  document
    .getElementById("newnessFilter")
    .addEventListener("change", applyFilters);
  document
    .getElementById("importanceFilter")
    .addEventListener("change", applyFilters);
  document
    .getElementById("searchInput")
    .addEventListener("input", applyFilters);
  document
    .getElementById("clearFilters")
    .addEventListener("click", clearFilters);
}

// Handle login
async function handleLogin(e) {
  e.preventDefault();

  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    });

    const data = await response.json();

    if (response.ok && data.token) {
      currentUser = data;
      showDashboard();
      connectWebSocket();
    } else {
      alert("Login failed: " + (data.message || "Invalid credentials"));
    }
  } catch (error) {
    console.error("Login error:", error);
    alert("Login failed: " + error.message);
  }
}

// Show dashboard
function showDashboard() {
  document.getElementById("loginSection").style.display = "none";
  document.getElementById("filtersSection").style.display = "block";
  document.getElementById("chatsSection").style.display = "block";
  document.getElementById("userInfo").style.display = "block";
  document.getElementById("userDisplay").textContent = currentUser.username;
  loadAssignedChats();
}

// Connect to WebSocket
function connectWebSocket() {
  const socket = new SockJS("/ws/chat");
  stompClient = Stomp.over(socket);

  stompClient.connect(
    {},
    function (frame) {
      console.log(
        "Connected to WebSocket as support user:",
        currentUser.username
      );

      // Subscribe to general chat topic for new escalations
      stompClient.subscribe("/topic/chat", onMessageReceived);

      // Subscribe to chat activity notifications
      stompClient.subscribe("/topic/support/activity", function (message) {
        const messageData = JSON.parse(message.body);
        console.log("New chat activity:", messageData);
        loadAssignedChats();
      });

      loadAssignedChats();
    },
    function (error) {
      console.error("WebSocket connection error:", error);
    }
  );
}

// Load assigned chats
async function loadAssignedChats() {
  try {
    const response = await fetch("/api/support/all-chats", {
      headers: {
        Authorization: "Bearer " + currentUser.token,
      },
    });

    if (response.ok) {
      allChats = await response.json();
      filteredChats = [...allChats];
      applyFilters();
    }
  } catch (error) {
    console.error("Error loading all chats:", error);
  }
}

// Apply filters
function applyFilters() {
  const statusFilter = document.getElementById("statusFilter").value;
  const ratingFilter = document.getElementById("ratingFilter").value;
  const nameFilter = document.getElementById("nameFilter").value;
  const newnessFilter = document.getElementById("newnessFilter").value;
  const importanceFilter = document.getElementById("importanceFilter").value;
  const searchInput = document
    .getElementById("searchInput")
    .value.toLowerCase();

  filteredChats = allChats.filter((chat) => {
    // Status filter
    if (
      statusFilter &&
      statusFilter !== "all" &&
      chat.status.toLowerCase() !== statusFilter
    ) {
      return false;
    }

    // Rating filter
    if (ratingFilter && ratingFilter !== "all") {
      if (ratingFilter === "low" && !chat.lowRating) {
        return false;
      }
      if (ratingFilter === "normal" && chat.lowRating) {
        return false;
      }
    }

    // Name filter
    if (nameFilter && nameFilter !== "all") {
      if (nameFilter === "named" && !chat.customerName) {
        return false;
      }
      if (nameFilter === "anonymous" && chat.customerName) {
        return false;
      }
    }

    // Newness filter
    if (newnessFilter && newnessFilter !== "all") {
      const now = new Date();
      const chatDate = new Date(chat.createdAt || chat.lastMessageAt || 0);
      const hoursDiff = (now - chatDate) / (1000 * 60 * 60);

      if (newnessFilter === "new" && hoursDiff > 1) {
        return false;
      }
      if (newnessFilter === "recent" && (hoursDiff < 1 || hoursDiff > 24)) {
        return false;
      }
      if (newnessFilter === "old" && hoursDiff <= 24) {
        return false;
      }
    }

    // Importance filter
    if (importanceFilter && importanceFilter !== "all") {
      const importance = getChatImportance(chat);
      if (importance !== importanceFilter) {
        return false;
      }
    }

    // Search filter
    if (searchInput) {
      const searchText = `${chat.chatId} ${chat.customerName || ""} ${
        chat.customerEmail || ""
      }`.toLowerCase();
      if (!searchText.includes(searchInput)) {
        return false;
      }
    }

    return true;
  });

  displayChatRooms(filteredChats);
}

// Get chat importance based on various factors
function getChatImportance(chat) {
  let score = 0;

  if (chat.lowRating) {
    score += 3;
  }

  // Escalated status increases importance
  if (chat.status.toLowerCase() === "escalated") {
    score += 2;
  }

  // Named customers are slightly more important
  if (chat.customerName) {
    score += 1;
  }

  // Recent chats are more important
  const now = new Date();
  const chatDate = new Date(chat.createdAt || chat.lastMessageAt || 0);
  const hoursDiff = (now - chatDate) / (1000 * 60 * 60);

  if (hoursDiff < 1) {
    score += 2;
  } else if (hoursDiff < 24) {
    score += 1;
  }

  // Determine priority based on score
  if (score >= 4) return "high";
  if (score >= 2) return "medium";
  return "low";
}

// Clear all filters
function clearFilters() {
  document.getElementById("statusFilter").value = "all";
  document.getElementById("ratingFilter").value = "all";
  document.getElementById("nameFilter").value = "all";
  document.getElementById("newnessFilter").value = "all";
  document.getElementById("importanceFilter").value = "all";
  document.getElementById("searchInput").value = "";
  applyFilters();
}

// Display chat rooms
function displayChatRooms(chats) {
  const chatRoomsList = document.getElementById("chatRoomsList");
  chatRoomsList.innerHTML = "";

  // Sort chats by creation date (newest first)
  const sortedChats = chats.sort((a, b) => {
    const dateA = new Date(a.createdAt || a.lastMessageAt || 0);
    const dateB = new Date(b.createdAt || b.lastMessageAt || 0);
    return dateB - dateA; // Newest first
  });

  sortedChats.forEach((chat) => {
    const roomDiv = document.createElement("div");
    roomDiv.className = "chat-item";
    roomDiv.onclick = () => openChat(chat.chatId);

    const customerInfo =
      chat.customerName || chat.customerEmail
        ? `<div class="customer-info">
                  ${
                    chat.customerName
                      ? `<div class="customer-name">${chat.customerName}</div>`
                      : ""
                  }
                  ${
                    chat.customerEmail
                      ? `<div class="customer-email">${chat.customerEmail}</div>`
                      : ""
                  }
                </div>`
        : '<div class="customer-info"><div class="customer-name">Anonymous customer</div></div>';

    const hasLowRating = chat.lowRating || false;
    if (hasLowRating) {
      roomDiv.classList.add("low-rating");
    }

    roomDiv.innerHTML = `
              <div class="chat-header">
                  <div class="chat-id">Chat: ${chat.chatId.substring(
                    0,
                    8
                  )}...</div>
                  <div class="chat-status status-${chat.status.toLowerCase()}">${
      chat.status
    }</div>
              </div>
              ${customerInfo}
               ${
                 hasLowRating
                   ? '<div class="rating-alert">⚠️ AI struggling - Support attention recommended</div>'
                   : ""
               }
              <div class="chat-actions">
                  <button onclick="assignChat('${
                    chat.chatId
                  }')" class="btn btn-success">Assign to Me</button>
              </div>
          `;

    chatRoomsList.appendChild(roomDiv);
  });
}

// Assign chat
async function assignChat(chatId) {
  try {
    const response = await fetch(`/api/support/assign-chat/${chatId}`, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + currentUser.token,
      },
    });

    if (response.ok) {
      alert("Chat assigned to you successfully!");
      loadAssignedChats();
    } else {
      const errorData = await response.json();
      alert("Error assigning chat: " + errorData.message);
    }
  } catch (error) {
    console.error("Error assigning chat:", error);
    alert("Error assigning chat: " + error.message);
  }
}

// Open chat
async function openChat(chatId) {
  currentChatId = chatId;

  // Update UI
  document.getElementById("welcomeScreen").classList.add("hidden");
  document.getElementById("chatContainer").classList.add("active");
  document.getElementById(
    "currentChatTitle"
  ).textContent = `Chat: ${chatId.substring(0, 8)}...`;
  document.getElementById("messageInput").disabled = false;
  document.getElementById("sendButton").disabled = false;
  document.getElementById("releaseButton").disabled = false;

  // Load chat history
  await loadChatHistory(chatId);

  // Update chat item appearance
  document.querySelectorAll(".chat-item").forEach((item) => {
    item.classList.remove("active");
  });
  event.currentTarget.classList.add("active");
}

// Load chat history
async function loadChatHistory(chatId) {
  try {
    const response = await fetch(`/api/support/chat-history/${chatId}`, {
      headers: {
        Authorization: "Bearer " + currentUser.token,
      },
    });

    if (response.ok) {
      const messages = await response.json();
      displayChatHistory(messages);
    }
  } catch (error) {
    console.error("Error loading chat history:", error);
  }
}

// Display chat history
function displayChatHistory(messages) {
  const chatMessages = document.getElementById("chatMessages");
  chatMessages.innerHTML = "";

  messages.forEach((message) => {
    const messageDiv = document.createElement("div");
    messageDiv.className = `message ${message.senderType.toLowerCase()}`;

    let ratingHtml = "";
    if (message.senderType === "AI" && message.rating) {
      ratingHtml = createRatingHtml(message.rating);
    }

    messageDiv.innerHTML = `
              <div class="message-bubble">
                  ${message.content}
                  <div class="message-time">${formatTime(
                    message.createdAt
                  )}</div>
                  ${ratingHtml}
              </div>
          `;

    chatMessages.appendChild(messageDiv);
  });

  scrollToBottom();
}

// Create rating HTML
function createRatingHtml(rating) {
  const score = rating.score || 0;
  const answer = rating.answer || "";

  let ratingClass = "bad";
  if (score >= 80) ratingClass = "excellent";
  else if (score >= 60) ratingClass = "good";
  else if (score >= 40) ratingClass = "fair";
  else if (score >= 20) ratingClass = "poor";

  return `
          <div class="ai-rating">
              <div class="rating-score">
                  <span class="rating-text">${score}/100</span>
                  <div class="rating-bar">
                      <div class="rating-fill ${ratingClass}" style="width: ${score}%"></div>
                  </div>
              </div>
              ${
                answer
                  ? `<div style="font-size: 11px; color: #666; margin-top: 4px;">${answer}</div>`
                  : ""
              }
          </div>
      `;
}

// Send message
function sendMessage() {
  const messageInput = document.getElementById("messageInput");
  const message = messageInput.value.trim();

  if (message && stompClient && currentChatId) {
    const chatRequest = {
      type: "MESSAGE",
      chatId: currentChatId,
      message: message,
      token: currentUser.token,
    };

    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatRequest));
    messageInput.value = "";
  }
}

// Handle received messages
function onMessageReceived(message) {
  const messageData = JSON.parse(message.body);
  console.log("Received message:", messageData);

  if (currentChatId && messageData.chatId === currentChatId) {
    displayMessage(messageData);
  }
}

// Display message
function displayMessage(messageData) {
  const chatMessages = document.getElementById("chatMessages");
  const messageDiv = document.createElement("div");

  const senderClass = messageData.sender.toLowerCase();
  messageDiv.className = `message ${senderClass}`;

  let ratingHtml = "";
  if (messageData.sender === "AI" && messageData.rating) {
    ratingHtml = createRatingHtml(messageData.rating);
  }

  messageDiv.innerHTML = `
          <div class="message-bubble">
              ${messageData.content}
              <div class="message-time">${formatTime(
                messageData.timestamp
              )}</div>
              ${ratingHtml}
          </div>
      `;

  chatMessages.appendChild(messageDiv);
  scrollToBottom();
}

// Release chat
function releaseChat() {
  if (stompClient && currentChatId) {
    const chatRequest = {
      type: "RELEASE",
      chatId: currentChatId,
      token: currentUser.token,
    };

    stompClient.send("/app/chat.release", {}, JSON.stringify(chatRequest));

    // Close the chat
    closeChat();
    alert("Chat released! AI assistant is now available for this chat.");
  }
}

// Close chat
function closeChat() {
  document.getElementById("chatContainer").classList.remove("active");
  document.getElementById("welcomeScreen").classList.remove("hidden");
  document.getElementById("messageInput").disabled = true;
  document.getElementById("sendButton").disabled = true;
  document.getElementById("releaseButton").disabled = true;
  currentChatId = null;
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
document.addEventListener("DOMContentLoaded", initDashboard);
