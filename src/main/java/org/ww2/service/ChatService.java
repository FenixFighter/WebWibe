package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ww2.entity.Chat;
import org.ww2.entity.ChatMessage;
import org.ww2.repository.ChatMessageRepository;
import org.ww2.repository.ChatRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    @Transactional
    public Chat createChat(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            chatId = generateChatId();
        }
        
        Optional<Chat> existingChat = chatRepository.findByChatId(chatId);
        if (existingChat.isPresent()) {
            log.info("Chat with ID {} already exists, returning existing chat", chatId);
            return existingChat.get();
        }
        
        Chat chat = new Chat();
        chat.setChatId(chatId);
        chat = chatRepository.save(chat);
        
        log.info("Created new chat with ID: {}", chatId);
        return chat;
    }
    
    @Transactional
    public Chat createChat(String chatId, String customerName, String customerEmail) {
        if (chatId == null || chatId.trim().isEmpty()) {
            chatId = generateChatId();
        }
        
        Optional<Chat> existingChat = chatRepository.findByChatId(chatId);
        if (existingChat.isPresent()) {
            // Update existing chat with customer info if not already set
            Chat chat = existingChat.get();
            if (chat.getCustomerName() == null && customerName != null) {
                chat.setCustomerName(customerName);
            }
            if (chat.getCustomerEmail() == null && customerEmail != null) {
                chat.setCustomerEmail(customerEmail);
            }
            chat = chatRepository.save(chat);
            log.info("Updated existing chat {} with customer info", chatId);
            return chat;
        }
        
        Chat chat = new Chat();
        chat.setChatId(chatId);
        chat.setCustomerName(customerName);
        chat.setCustomerEmail(customerEmail);
        chat = chatRepository.save(chat);
        
        log.info("Created new chat with ID: {} and customer info", chatId);
        return chat;
    }
    
    @Transactional
    public ChatMessage saveUserMessage(String chatId, String content) {
        getOrCreateChat(chatId);
        
        ChatMessage message = new ChatMessage();
        message.setChatId(chatId);
        message.setContent(content);
        message.setSenderType(ChatMessage.SenderType.USER);
        
        message = chatMessageRepository.save(message);
        log.info("Saved user message for chat: {}", chatId);
        return message;
    }
    
    @Transactional
    public ChatMessage saveAiMessage(String chatId, String content) {
        getOrCreateChat(chatId);
        
        ChatMessage message = new ChatMessage();
        message.setChatId(chatId);
        message.setContent(content);
        message.setSenderType(ChatMessage.SenderType.AI);
        
        message = chatMessageRepository.save(message);
        log.info("Saved AI message for chat: {}", chatId);
        return message;
    }
    
    public List<ChatMessage> getChatHistory(String chatId) {
        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }
    
    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }
    
    @Transactional
    public ChatMessage saveSupportMessage(String chatId, String content, String supportUsername) {
        getOrCreateChat(chatId);
        
        ChatMessage message = new ChatMessage();
        message.setChatId(chatId);
        message.setContent(content);
        message.setSenderType(ChatMessage.SenderType.SUPPORT);
        
        message = chatMessageRepository.save(message);
        log.info("Saved support message for chat: {} from {}", chatId, supportUsername);
        return message;
    }
    
    private Chat getOrCreateChat(String chatId) {
        Optional<Chat> chat = chatRepository.findByChatId(chatId);
        if (chat.isPresent()) {
            return chat.get();
        }
        
        return createChat(chatId);
    }
    
    private String generateChatId() {
        return UUID.randomUUID().toString();
    }
}
