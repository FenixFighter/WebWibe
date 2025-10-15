package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ww2.dto.AuthRequest;
import org.ww2.dto.AuthResponse;
import org.ww2.entity.User;
import org.ww2.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final Map<String, User> activeTokens = new HashMap<>();
    
    public AuthResponse authenticate(AuthRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Простая проверка пароля (в реальном приложении нужно использовать BCrypt)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Генерируем токен
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, user);
        
        // Обновляем статус онлайн
        user.setIsOnline(true);
        userRepository.save(user);
        
        log.info("User {} authenticated successfully", user.getUsername());
        
        return new AuthResponse(token, user.getRole(), user.getUsername(), user.getId());
    }
    
    public void logout(String token) {
        User user = activeTokens.remove(token);
        if (user != null) {
            user.setIsOnline(false);
            userRepository.save(user);
            log.info("User {} logged out", user.getUsername());
        }
    }
    
    public User getUserByToken(String token) {
        return activeTokens.get(token);
    }
    
    public boolean isTokenValid(String token) {
        return activeTokens.containsKey(token);
    }
    
    @Transactional
    public User createUser(String username, String password, String email, User.UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // В реальном приложении нужно хешировать
        user.setEmail(email);
        user.setRole(role);
        user.setIsOnline(false);
        
        return userRepository.save(user);
    }
}
