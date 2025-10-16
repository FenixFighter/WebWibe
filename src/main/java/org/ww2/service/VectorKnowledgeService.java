package org.ww2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.ww2.entity.KnowledgeVector;
import org.ww2.repository.KnowledgeVectorRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorKnowledgeService {
    
    @Autowired
    private KnowledgeVectorRepository knowledgeVectorRepository;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public VectorKnowledgeService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://45.145.191.148:4000/v1")
            .defaultHeader("Authorization", "Bearer sk-DaEm7ghnNnWtYjrc3eiEug")
            .defaultHeader("Content-Type", "application/json")
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void initializeKnowledgeBase() {
        System.out.println("=== Initializing Vector Knowledge Base ===");
        
        // Проверяем, есть ли уже данные в базе
        long count = knowledgeVectorRepository.count();
        if (count > 0) {
            System.out.println("Knowledge base already initialized with " + count + " vectors");
            return;
        }
        
        // Загружаем данные из CSV файла
        loadAndVectorizeData();
    }
    
    private void loadAndVectorizeData() {
        try {
            System.out.println("Loading data from CSV file...");
            // Читаем CSV файл из resources
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("smart_support_vtb_belarus_faq_final.csv");
            if (inputStream == null) {
                System.err.println("CSV file not found in resources");
                return;
            }
            
            List<KnowledgeVector> vectors = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Пропускаем заголовок
                }
                
                String[] fields = parseCSVLine(line);
                if (fields.length >= 6) {
                    String category = fields[0].trim(); // Основная категория
                    String subcategory = fields[1].trim(); // Подкатегория
                    String question = fields[2].trim(); // Пример вопроса
                    String answer = fields[5].trim(); // Шаблонный ответ
                    
                    if (question != null && !question.isEmpty() && 
                        answer != null && !answer.isEmpty()) {
                        
                        // Временно отключаем создание эмбеддингов из-за проблем с API
                        // String embedding = createEmbedding(question);
                        
                        KnowledgeVector vector = new KnowledgeVector();
                        vector.setQuestion(question);
                        vector.setAnswer(answer);
                        vector.setCategory(category != null ? category.toLowerCase() : null);
                        // vector.setEmbedding(embedding);
                        
                        vectors.add(vector);
                        
                        System.out.println("Vectorized: " + question.substring(0, Math.min(50, question.length())) + "...");
                    }
                }
            }
            
            reader.close();
            inputStream.close();
            
            // Сохраняем все векторы в базу данных
            knowledgeVectorRepository.saveAll(vectors);
            
            System.out.println("Successfully vectorized and stored " + vectors.size() + " knowledge entries");
            
        } catch (IOException e) {
            System.err.println("Error loading knowledge base: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }
    
    private String createEmbedding(String text) {
        try {
            // Ограничиваем длину текста для API
            if (text.length() > 8000) {
                text = text.substring(0, 8000);
            }
            
            String requestBody = String.format("""
                {
                    "input": "%s",
                    "model": "text-embedding-3-small"
                }
                """, text.replace("\"", "\\\"").replace("\n", " ").replace("\r", " "));

            String response = webClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        System.err.println("API Error: " + clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                            .doOnNext(errorBody -> System.err.println("Error body: " + errorBody))
                            .flatMap(errorBody -> Mono.error(new RuntimeException("API Error: " + errorBody)));
                    })
                .bodyToMono(String.class)
                .block();

            if (response != null && response.contains("data")) {
                JsonNode jsonNode = objectMapper.readTree(response);
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode embeddingNode = dataNode.get(0).get("embedding");
                    if (embeddingNode.isArray()) {
                        List<String> embeddingList = new ArrayList<>();
                        for (JsonNode value : embeddingNode) {
                            embeddingList.add(value.asText());
                        }
                        return "[" + String.join(",", embeddingList) + "]";
                    }
                }
            }
            
            System.err.println("Failed to extract embedding from response");
            return "[" + String.join(",", Collections.nCopies(1536, "0.0")) + "]";
                    
        } catch (Exception e) {
            System.err.println("Error creating embedding: " + e.getMessage());
            // Возвращаем нулевой вектор в случае ошибки
            return "[" + String.join(",", Collections.nCopies(1536, "0.0")) + "]";
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
    
    public List<KnowledgeVector> findSimilarQuestions(String question, String category, int limit) {
        System.out.println("=== Two-Stage Search in PostgreSQL ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);
        
        try {
            // Этап 1: Прямой поиск по релевантности
            List<KnowledgeVector> allVectors;
            if (category != null && !category.trim().isEmpty()) {
                allVectors = knowledgeVectorRepository.findByCategoryIgnoreCase(category.toLowerCase());
            } else {
                allVectors = knowledgeVectorRepository.findAll();
            }
            
            // Сортируем по релевантности
            List<KnowledgeVector> similarVectors = allVectors.stream()
                .sorted((v1, v2) -> {
                    double score1 = calculateTextSimilarity(question, v1.getQuestion());
                    double score2 = calculateTextSimilarity(question, v2.getQuestion());
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
            
            // Проверяем, есть ли достаточно релевантные результаты
            List<KnowledgeVector> highRelevanceResults = similarVectors.stream()
                .filter(v -> calculateTextSimilarity(question, v.getQuestion()) > 0.3)
                .limit(limit)
                .collect(Collectors.toList());
            
            if (!highRelevanceResults.isEmpty()) {
                System.out.println("Found " + highRelevanceResults.size() + " high-relevance results");
                return highRelevanceResults;
            }
            
            // Этап 2: Если нет релевантных результатов - ищем по категориям
            System.out.println("No high-relevance results found, searching by categories...");
            
            // Получаем все уникальные категории
            List<String> allCategories = knowledgeVectorRepository.findDistinctCategories();
            
            // Находим наиболее релевантную категорию
            String bestCategory = findBestCategory(question, allCategories);
            System.out.println("Best matching category: " + bestCategory);
            
            // Возвращаем все вопросы из найденной категории
            List<KnowledgeVector> categoryResults = knowledgeVectorRepository.findByCategoryIgnoreCase(bestCategory);
            
            // Ограничиваем количество результатов
            if (categoryResults.size() > limit) {
                categoryResults = categoryResults.subList(0, limit);
            }
            
            System.out.println("Found " + categoryResults.size() + " results from category: " + bestCategory);
            return categoryResults;
            
        } catch (Exception e) {
            System.err.println("Error in two-stage search: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private String findBestCategory(String question, List<String> categories) {
        if (categories.isEmpty()) return null;
        
        String bestCategory = categories.get(0);
        double bestScore = 0.0;
        
        for (String category : categories) {
            double score = calculateTextSimilarity(question, category);
            if (score > bestScore) {
                bestScore = score;
                bestCategory = category;
            }
        }
        
        System.out.println("Category '" + bestCategory + "' scored: " + bestScore);
        return bestCategory;
    }
    
    private double calculateTextSimilarity(String question1, String question2) {
        if (question1 == null || question2 == null) return 0.0;
        
        String q1 = question1.toLowerCase().trim();
        String q2 = question2.toLowerCase().trim();
        
        // Точное совпадение
        if (q1.equals(q2)) return 1.0;
        
        // Содержит подстроку
        if (q1.contains(q2) || q2.contains(q1)) return 0.8;
        
        // Обработка сокращений
        q1 = expandAbbreviations(q1);
        q2 = expandAbbreviations(q2);
        
        // Содержит подстроку после расширения сокращений
        if (q1.contains(q2) || q2.contains(q1)) return 0.7;
        
        // Подсчет общих слов
        String[] words1 = q1.split("\\s+");
        String[] words2 = q2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) return 0.0;
        
        return (double) intersection.size() / union.size();
    }
    
    private String expandAbbreviations(String text) {
        // Расширяем распространенные сокращения
        text = text.replace("нач", "первоначальная");
        text = text.replace("ставка", "процентная ставка");
        text = text.replace("кредит", "кредитный продукт");
        text = text.replace("карта", "банковская карта");
        text = text.replace("вклад", "депозитный вклад");
        return text;
    }
    
    public String[] getCategoryAndSubcategory(String question, String category) {
        System.out.println("=== Getting Category and Subcategory ===");
        
        try {
            List<KnowledgeVector> similarVectors = findSimilarQuestions(question, category, 1);
            
            if (!similarVectors.isEmpty()) {
                KnowledgeVector bestMatch = similarVectors.get(0);
                String foundCategory = bestMatch.getCategory();
                
                // Получаем подкатегорию из базы данных
                String subcategory = getSubcategoryFromDatabase(foundCategory);
                
                System.out.println("Found category: " + foundCategory + ", subcategory: " + subcategory);
                return new String[]{foundCategory, subcategory};
            }
            
            return new String[]{null, null};
            
        } catch (Exception e) {
            System.err.println("Error getting category: " + e.getMessage());
            return new String[]{null, null};
        }
    }
    
    private String getSubcategoryFromDatabase(String category) {
        try {
            // Получаем первую запись из категории для определения подкатегории
            List<KnowledgeVector> categoryResults = knowledgeVectorRepository.findByCategoryIgnoreCase(category);
            if (!categoryResults.isEmpty()) {
                // Здесь можно добавить логику для определения подкатегории
                // Пока возвращаем null, так как в нашей структуре нет отдельного поля подкатегории
                return null;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting subcategory: " + e.getMessage());
            return null;
        }
    }
    
    public String generateFewShotPrompt(String question, String category) {
        System.out.println("=== Generating Few-Shot Prompt with Vector Search ===");
        
        // Находим похожие примеры с помощью векторного поиска
        List<KnowledgeVector> similarVectors = findSimilarQuestions(question, category, 3);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - AI-ассистент. Отвечай на вопросы клиентов используя примеры ниже как образец стиля и структуры ответов.\n\n");
        
        // Добавляем примеры из векторного поиска
        for (int i = 0; i < similarVectors.size(); i++) {
            KnowledgeVector vector = similarVectors.get(i);
            prompt.append("Пример ").append(i + 1).append(":\n");
            prompt.append("Вопрос: ").append(vector.getQuestion()).append("\n");
            prompt.append("Ответ: ").append(vector.getAnswer()).append("\n\n");
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
}
