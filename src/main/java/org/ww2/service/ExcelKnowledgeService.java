package org.ww2.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelKnowledgeService {
    
    private List<KnowledgeEntry> knowledgeBase = new ArrayList<>();
    
    public ExcelKnowledgeService() {
        // loadKnowledgeBase(); // Отключено, используется VectorKnowledgeService
        System.out.println("=== Excel Knowledge Service initialized (disabled) ===");
    }
    
    private void loadKnowledgeBase() {
        try {
            System.out.println("=== Loading knowledge base from Excel file ===");
            FileInputStream file = new FileInputStream("smart_support_vtb_belarus_faq_final copy.xlsx");
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            
            System.out.println("Excel file loaded, rows: " + sheet.getLastRowNum());
            
            // Пропускаем заголовок (первую строку)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String question = getCellValueAsString(row.getCell(0));
                    String answer = getCellValueAsString(row.getCell(1));
                    String category = getCellValueAsString(row.getCell(2));
                    
                    if (question != null && !question.trim().isEmpty() && 
                        answer != null && !answer.trim().isEmpty()) {
                        knowledgeBase.add(new KnowledgeEntry(question, answer, category));
                        System.out.println("Loaded: " + question + " -> " + answer.substring(0, Math.min(50, answer.length())) + "...");
                    }
                }
            }
            
            workbook.close();
            file.close();
            
            System.out.println("Successfully loaded " + knowledgeBase.size() + " knowledge entries from Excel");
            
        } catch (IOException e) {
            System.err.println("Error loading knowledge base: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Falling back to empty knowledge base");
        }
    }
    
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    public List<KnowledgeEntry> searchSimilarQuestions(String question, String category, int limit) {
        System.out.println("=== Excel Knowledge Search ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);
        
        List<KnowledgeEntry> candidates = knowledgeBase;
        
        // Фильтруем по категории, если указана
        if (category != null && !category.trim().isEmpty()) {
            candidates = knowledgeBase.stream()
                .filter(entry -> entry.getCategory() != null && 
                               entry.getCategory().toLowerCase().contains(category.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // Если не нашли по категории, ищем по всем
        if (candidates.isEmpty()) {
            candidates = knowledgeBase;
        }
        
        // Вычисляем схожесть и сортируем
        List<SimilarityResult> similarities = candidates.stream()
            .map(entry -> new SimilarityResult(entry, calculateSimilarity(question, entry.getQuestion())))
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(limit)
            .collect(Collectors.toList());
        
        System.out.println("Found " + similarities.size() + " similar questions");
        
        return similarities.stream()
            .map(SimilarityResult::getEntry)
            .collect(Collectors.toList());
    }
    
    private double calculateSimilarity(String question1, String question2) {
        if (question1 == null || question2 == null) return 0.0;
        
        String q1 = question1.toLowerCase().trim();
        String q2 = question2.toLowerCase().trim();
        
        // Точное совпадение
        if (q1.equals(q2)) return 1.0;
        
        // Проверяем, содержит ли один вопрос другой
        if (q1.contains(q2) || q2.contains(q1)) return 0.9;
        
        // Улучшенный алгоритм схожести
        double similarity = 0.0;
        
        // 1. Jaccard similarity по словам
        String[] words1 = q1.split("\\s+");
        String[] words2 = q2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        similarity += jaccard * 0.4; // 40% веса
        
        // 2. Схожесть по подстрокам (n-grams)
        double ngramSimilarity = calculateNgramSimilarity(q1, q2);
        similarity += ngramSimilarity * 0.3; // 30% веса
        
        // 3. Схожесть по ключевым словам
        double keywordSimilarity = calculateKeywordSimilarity(q1, q2);
        similarity += keywordSimilarity * 0.3; // 30% веса
        
        return Math.min(1.0, similarity);
    }
    
    private double calculateNgramSimilarity(String s1, String s2) {
        int n = 2; // биграммы
        Set<String> ngrams1 = generateNgrams(s1, n);
        Set<String> ngrams2 = generateNgrams(s2, n);
        
        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);
        
        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private Set<String> generateNgrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        for (int i = 0; i <= text.length() - n; i++) {
            ngrams.add(text.substring(i, i + n));
        }
        return ngrams;
    }
    
    private double calculateKeywordSimilarity(String q1, String q2) {
        // Извлекаем ключевые слова из всех вопросов в базе знаний
        Set<String> allKeywords = extractKeywordsFromKnowledgeBase();
        
        int matches = 0;
        int total = 0;
        
        for (String keyword : allKeywords) {
            boolean inQ1 = q1.contains(keyword);
            boolean inQ2 = q2.contains(keyword);
            
            if (inQ1 || inQ2) {
                total++;
                if (inQ1 && inQ2) {
                    matches++;
                }
            }
        }
        
        return total == 0 ? 0.0 : (double) matches / total;
    }
    
    private Set<String> extractKeywordsFromKnowledgeBase() {
        Set<String> keywords = new HashSet<>();
        
        // Извлекаем слова из всех вопросов в базе знаний
        for (KnowledgeEntry entry : knowledgeBase) {
            String question = entry.getQuestion().toLowerCase();
            String[] words = question.split("\\s+");
            
            for (String word : words) {
                // Берем только значимые слова (длиннее 3 символов)
                if (word.length() > 3 && !isStopWord(word)) {
                    keywords.add(word);
                }
            }
        }
        
        System.out.println("Extracted " + keywords.size() + " keywords from knowledge base");
        return keywords;
    }
    
    private boolean isStopWord(String word) {
        String[] stopWords = {
            "как", "что", "где", "когда", "почему", "зачем", "для", "чего", 
            "это", "этот", "эта", "эти", "все", "вся", "всё", "можно", 
            "нужно", "необходимо", "требуется", "получить", "сделать"
        };
        
        for (String stopWord : stopWords) {
            if (word.equals(stopWord)) {
                return true;
            }
        }
        return false;
    }
    
    public String generateFewShotPrompt(String question, String category) {
        System.out.println("=== Generating Few-Shot Prompt from Excel Knowledge ===");
        
        // Находим похожие примеры из реальной базы знаний
        List<KnowledgeEntry> similarExamples = searchSimilarQuestions(question, category, 3);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - AI-ассистент банка ВТБ Беларусь. Отвечай на вопросы клиентов в профессиональном стиле, используя примеры ниже как образец стиля и структуры ответов.\n\n");
        
        // Добавляем примеры из реальной базы знаний
        for (int i = 0; i < similarExamples.size(); i++) {
            KnowledgeEntry example = similarExamples.get(i);
            prompt.append("Пример ").append(i + 1).append(":\n");
            prompt.append("Вопрос: ").append(example.getQuestion()).append("\n");
            prompt.append("Ответ: ").append(example.getAnswer()).append("\n\n");
        }
        
        // Добавляем инструкции по стилю
        prompt.append("ВАЖНЫЕ ИНСТРУКЦИИ:\n");
        prompt.append("- Отвечай ТОЛЬКО на основе примеров выше\n");
        prompt.append("- НЕ придумывай дополнительную информацию\n");
        prompt.append("- Будь кратким и точным\n");
        prompt.append("- Используй тот же стиль, что и в примерах\n");
        prompt.append("- Если не знаешь точного ответа, скажи 'Обратитесь в отделение банка для уточнения'\n\n");
        
        prompt.append("Теперь ответь на вопрос: ").append(question);
        
        return prompt.toString();
    }
    
    // Внутренние классы
    public static class KnowledgeEntry {
        private final String question;
        private final String answer;
        private final String category;
        
        public KnowledgeEntry(String question, String answer, String category) {
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
        
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getCategory() { return category; }
    }
    
    public static class SimilarityResult {
        private final KnowledgeEntry entry;
        private final double similarity;
        
        public SimilarityResult(KnowledgeEntry entry, double similarity) {
            this.entry = entry;
            this.similarity = similarity;
        }
        
        public KnowledgeEntry getEntry() { return entry; }
        public double getSimilarity() { return similarity; }
    }
}
