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

    public AiRating evaluateAiResponse(String userQuestion, String aiResponse) {
        log.info("Evaluating AI response for question: {}", userQuestion);

        int score = calculateScore(userQuestion, aiResponse);
        String explanation = generateExplanation(score, userQuestion, aiResponse);

        AiRating rating = new AiRating(score, explanation);
        log.info("AI response rated: {}/100 - {}", score, explanation);

        return rating;
    }

    private int calculateScore(String userQuestion, String aiResponse) {
        int score = 50; 

        if (containsHelpfulIndicators(aiResponse)) {
            score += 20;
        }

        if (containsBankingTerms(aiResponse)) {
            score += 15;
        }

        if (isRelevantToQuestion(userQuestion, aiResponse)) {
            score += 15;
        }

        if (hasProfessionalTone(aiResponse)) {
            score += 10;
        }

        if (containsNegativeIndicators(aiResponse)) {
            score -= 30;
        }

        if (isGenericResponse(aiResponse)) {
            score -= 20;
        }

        return Math.max(0, Math.min(100, score));
    }

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

    private boolean isRelevantToQuestion(String question, String response) {
        String lowerQuestion = question.toLowerCase();
        String lowerResponse = response.toLowerCase();

        String[] questionWords = lowerQuestion.split("\\s+");
        int matches = 0;

        for (String word : questionWords) {
            if (word.length() > 3 && lowerResponse.contains(word)) {
                matches++;
            }
        }

        return matches >= Math.min(2, questionWords.length / 2);
    }

    private boolean hasProfessionalTone(String response) {
        String lowerResponse = response.toLowerCase();
        return !lowerResponse.contains("lol") &&
               !lowerResponse.contains("haha") &&
               !lowerResponse.contains("omg") &&
               !lowerResponse.contains("wtf") &&
               !lowerResponse.contains("dude") &&
               !lowerResponse.contains("bro");
    }

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

    private boolean isGenericResponse(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("hello") ||
               lowerResponse.contains("hi there") ||
               lowerResponse.contains("how can i help") ||
               lowerResponse.contains("what can i do") ||
               response.length() < 20;
    }

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

    public boolean shouldEscalateToSupport(AiRating rating) {
        return rating.getScore() < 30;
    }

    public WebSocketMessage createAiMessageWithRating(String chatId, String content, AiRating rating) {
        WebSocketMessage message = WebSocketMessage.createAiMessage(chatId, content);
        message.setRating(rating);
        return message;
    }
}
