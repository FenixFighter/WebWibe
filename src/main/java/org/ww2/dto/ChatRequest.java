package org.ww2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    private String type;
    private String chatId;
    private String message;
    private String category;
    private String token;
    private String customerName;
    private String customerEmail;
}
