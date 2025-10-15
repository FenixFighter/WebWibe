package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ww2.dto.AiRating;
import org.ww2.dto.WebSocketMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRatingService {

    /**
     * Evaluates AI response and returns rating
     */
    public AiRating evaluateAiResponse(String userQuestion, String aiResponse) {
        log.info("Evaluating AI response for question: {}", userQuestion);
        
        int score = calculateScore(userQuestion, aiResponse);
        String explanation = generateExplanation(score, userQuestion, aiResponse);
        
        AiRating rating = new AiRating(score, explanation);
        log.info("AI response rated: {}/100 - {}", score, explanation);
        
        return rating;
    }

    /**
     * Calculates score based on various factors
     */
    private int calculateScore(String userQuestion, String aiResponse) {
        int score = 50; // Base score
        
        // Check for helpful indicators
        if (containsHelpfulIndicators(aiResponse)) {
            score += 20;
        }
        
        // Check for specific banking terms
        if (containsBankingTerms(aiResponse)) {
            score += 15;
        }
        
        // Check for question relevance
        if (isRelevantToQuestion(userQuestion, aiResponse)) {
            score += 15;
        }
        
        // Check for professional tone
        if (hasProfessionalTone(aiResponse)) {
            score += 10;
        }
        
        // Check for negative indicators
        if (containsNegativeIndicators(aiResponse)) {
            score -= 30;
        }
        
        // Check for generic responses
        if (isGenericResponse(aiResponse)) {
            score -= 20;
        }
        
        // Ensure score is between 0 and 100
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Checks for helpful indicators in AI response
     */
    private boolean containsHelpfulIndicators(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("i can help") ||
               lowerResponse.contains("let me assist") ||
               lowerResponse.contains("here's how") ||
               lowerResponse.contains("you can") ||
               lowerResponse.contains("to do this") ||
               lowerResponse.contains("step by step") ||
               lowerResponse.contains("specific information");
    }

    /**
     * Checks for banking-related terms
     */
    private boolean containsBankingTerms(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("account") ||
               lowerResponse.contains("balance") ||
               lowerResponse.contains("transaction") ||
               lowerResponse.contains("deposit") ||
               lowerResponse.contains("withdrawal") ||
               lowerResponse.contains("loan") ||
               lowerResponse.contains("credit") ||
               lowerResponse.contains("interest") ||
               lowerResponse.contains("banking") ||
               lowerResponse.contains("financial");
    }

    /**
     * Checks if response is relevant to the question
     */
    private boolean isRelevantToQuestion(String question, String response) {
        String lowerQuestion = question.toLowerCase();
        String lowerResponse = response.toLowerCase();
        
        // Simple keyword matching
        String[] questionWords = lowerQuestion.split("\\s+");
        int matches = 0;
        
        for (String word : questionWords) {
            if (word.length() > 3 && lowerResponse.contains(word)) {
                matches++;
            }
        }
        
        return matches >= Math.min(2, questionWords.length / 2);
    }

    /**
     * Checks for professional tone
     */
    private boolean hasProfessionalTone(String response) {
        String lowerResponse = response.toLowerCase();
        return !lowerResponse.contains("lol") &&
               !lowerResponse.contains("haha") &&
               !lowerResponse.contains("omg") &&
               !lowerResponse.contains("wtf") &&
               !lowerResponse.contains("dude") &&
               !lowerResponse.contains("bro");
    }

    /**
     * Checks for negative indicators
     */
    private boolean containsNegativeIndicators(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("i don't know") ||
               lowerResponse.contains("i can't help") ||
               lowerResponse.contains("i'm not sure") ||
               lowerResponse.contains("i don't understand") ||
               lowerResponse.contains("no answers found") ||
               lowerResponse.contains("cannot help") ||
               lowerResponse.contains("don't understand") ||
               lowerResponse.contains("contact support") ||
               lowerResponse.contains("human assistance") ||
               lowerResponse.contains("escalate");
    }

    /**
     * Checks for generic responses
     */
    private boolean isGenericResponse(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("hello") ||
               lowerResponse.contains("hi there") ||
               lowerResponse.contains("how can i help") ||
               lowerResponse.contains("what can i do") ||
               response.length() < 20;
    }

    /**
     * Generates explanation for the rating
     */
    private String generateExplanation(int score, String question, String response) {
        if (score >= 80) {
            return "Excellent response with specific banking information and helpful guidance";
        } else if (score >= 60) {
            return "Good response with relevant information and professional tone";
        } else if (score >= 40) {
            return "Fair response but could be more specific or helpful";
        } else if (score >= 20) {
            return "Poor response with limited helpful information";
        } else {
            return "Very poor response - customer needs human assistance";
        }
    }

    /**
     * Checks if rating indicates need for human support
     */
    public boolean shouldEscalateToSupport(AiRating rating) {
        return rating.getScore() < 30;
    }

    /**
     * Creates WebSocket message with rating
     */
    public WebSocketMessage createAiMessageWithRating(String chatId, String content, AiRating rating) {
        WebSocketMessage message = WebSocketMessage.createAiMessage(chatId, content);
        message.setRating(rating);
        return message;
    }
}
