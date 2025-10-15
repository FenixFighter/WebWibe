package org.ww2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.ww2.entity.TemplateAnswer;
import org.ww2.repository.TemplateAnswerRepository;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Order(1)
public class DataInitializationService implements CommandLineRunner {
    
    private final TemplateAnswerRepository templateAnswerRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Проверяем, есть ли уже данные
        long count = templateAnswerRepository.count();
        System.out.println("Current template answers count: " + count);
        
        if (count > 0) {
            System.out.println("Data already exists, skipping initialization");
            return;
        }
        
        System.out.println("Initializing test data...");
        
        // Создаем тестовые данные
        List<TemplateAnswer> sampleData = Arrays.asList(
            new TemplateAnswer(null, "Banking", "Credit Cards", "To apply for a credit card, please visit our nearest branch with your ID and proof of income."),
            new TemplateAnswer(null, "Banking", "Credit Cards", "Credit card interest rates vary from 15% to 25% depending on your credit score."),
            new TemplateAnswer(null, "Banking", "Loans", "Personal loans are available for amounts from $1,000 to $50,000 with flexible repayment terms."),
            new TemplateAnswer(null, "Banking", "Loans", "To qualify for a loan, you need a minimum credit score of 600 and stable income."),
            new TemplateAnswer(null, "Banking", "Savings", "Our savings accounts offer competitive interest rates up to 3.5% APY."),
            new TemplateAnswer(null, "Banking", "Savings", "You can open a savings account online or at any branch with a minimum deposit of $100."),
            new TemplateAnswer(null, "Technical Support", "Login Issues", "If you can't log in, try resetting your password using the 'Forgot Password' link."),
            new TemplateAnswer(null, "Technical Support", "Login Issues", "Make sure your username and password are correct, and check for any caps lock issues."),
            new TemplateAnswer(null, "Technical Support", "Mobile App", "Download our mobile app from the App Store or Google Play Store for easy banking."),
            new TemplateAnswer(null, "Technical Support", "Mobile App", "The mobile app requires iOS 12+ or Android 8+ to function properly."),
            new TemplateAnswer(null, "General", "Contact", "You can reach our customer service at 1-800-BANK-HELP or visit any branch."),
            new TemplateAnswer(null, "General", "Contact", "Our customer service is available Monday-Friday 8AM-6PM and Saturday 9AM-2PM.")
        );
        
        templateAnswerRepository.saveAll(sampleData);
        System.out.println("Test data initialized successfully. Total records: " + templateAnswerRepository.count());
    }
}
