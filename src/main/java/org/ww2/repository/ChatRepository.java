package org.ww2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ww2.entity.Chat;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    
    Optional<Chat> findByChatId(String chatId);
}
