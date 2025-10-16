package org.ww2.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.ww2.entity.KnowledgeVector;

import java.util.List;

@Service
public class VectorSearchService {
    
    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;
    
    public List<KnowledgeVector> findSimilarQuestions(String question, String category, int limit) {
        return vectorKnowledgeService.findSimilarQuestions(question, category, limit);
    }
    
    public String generateFewShotPrompt(String question, String category) {
        return vectorKnowledgeService.generateFewShotPrompt(question, category);
    }
    
    public String[] getCategoryAndSubcategory(String question, String category) {
        return vectorKnowledgeService.getCategoryAndSubcategory(question, category);
    }
}
