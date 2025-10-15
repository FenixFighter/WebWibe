package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ww2.repository.TemplateAnswerRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final TemplateAnswerRepository templateAnswerRepository;

    @GetMapping("/categories")
    public List<String> getCategories() {
        return templateAnswerRepository.findDistinctCategories();
    }
}
