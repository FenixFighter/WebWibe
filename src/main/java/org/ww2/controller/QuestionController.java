package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ww2.dto.QuestionRequest;
import org.ww2.dto.QuestionResponse;
import org.ww2.service.QuestionProcessingService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionProcessingService questionProcessingService;

    @PostMapping("/question")
    public ResponseEntity<QuestionResponse> processQuestion(@RequestBody QuestionRequest request) {
        try {
            QuestionResponse response = questionProcessingService.processQuestion(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new QuestionResponse("Error processing question: " + e.getMessage()));
        }
    }
}
