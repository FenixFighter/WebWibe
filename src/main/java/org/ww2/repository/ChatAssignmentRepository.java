package org.ww2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ww2.entity.ChatAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAssignmentRepository extends JpaRepository<ChatAssignment, Long> {

    Optional<ChatAssignment> findByChatIdAndStatus(String chatId, ChatAssignment.AssignmentStatus status);

    List<ChatAssignment> findByAssignedUserIdAndStatus(Long userId, ChatAssignment.AssignmentStatus status);

    List<ChatAssignment> findByStatus(ChatAssignment.AssignmentStatus status);
}
