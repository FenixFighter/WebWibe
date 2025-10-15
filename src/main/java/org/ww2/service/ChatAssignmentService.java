package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.User;
import org.ww2.repository.ChatAssignmentRepository;
import org.ww2.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAssignmentService {
    
    private final ChatAssignmentRepository chatAssignmentRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ChatAssignment assignChatToSupport(String chatId) {
        // Проверяем, есть ли уже назначение для этого чата
        Optional<ChatAssignment> existingAssignment = chatAssignmentRepository
            .findByChatIdAndStatus(chatId, ChatAssignment.AssignmentStatus.ACTIVE);
        
        if (existingAssignment.isPresent()) {
            log.info("Chat {} is already assigned to support", chatId);
            return existingAssignment.get();
        }
        
        // Находим доступного сотрудника техподдержки
        List<User> availableSupport = userRepository.findByRoleAndIsOnlineTrue(User.UserRole.SUPPORT);
        
        if (availableSupport.isEmpty()) {
            log.warn("No online support staff available for chat {}", chatId);
            return null;
        }
        
        // Назначаем чат первому доступному сотруднику
        User supportUser = availableSupport.get(0);
        
        ChatAssignment assignment = new ChatAssignment();
        assignment.setChatId(chatId);
        assignment.setAssignedUser(supportUser);
        assignment.setStatus(ChatAssignment.AssignmentStatus.ACTIVE);
        
        ChatAssignment savedAssignment = chatAssignmentRepository.save(assignment);
        log.info("Chat {} assigned to support user {}", chatId, supportUser.getUsername());
        
        return savedAssignment;
    }
    
    public Optional<ChatAssignment> getChatAssignment(String chatId) {
        return chatAssignmentRepository.findByChatIdAndStatus(chatId, ChatAssignment.AssignmentStatus.ACTIVE);
    }
    
    public List<ChatAssignment> getAssignedChats(Long userId) {
        return chatAssignmentRepository.findByAssignedUserIdAndStatus(userId, ChatAssignment.AssignmentStatus.ACTIVE);
    }
    
    @Transactional
    public void resolveChat(String chatId) {
        Optional<ChatAssignment> assignment = chatAssignmentRepository
            .findByChatIdAndStatus(chatId, ChatAssignment.AssignmentStatus.ACTIVE);
        
        if (assignment.isPresent()) {
            ChatAssignment chatAssignment = assignment.get();
            chatAssignment.setStatus(ChatAssignment.AssignmentStatus.RESOLVED);
            chatAssignment.setResolvedAt(java.time.LocalDateTime.now());
            chatAssignmentRepository.save(chatAssignment);
            log.info("Chat {} resolved", chatId);
        }
    }
    
    public List<ChatAssignment> getPendingAssignments() {
        return chatAssignmentRepository.findByStatus(ChatAssignment.AssignmentStatus.PENDING);
    }
}
