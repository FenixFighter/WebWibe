package org.ww2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ww2.entity.TemplateAnswer;

import java.util.List;

@Repository
public interface TemplateAnswerRepository extends JpaRepository<TemplateAnswer, Long> {

    List<TemplateAnswer> findByCategory(String category);

    List<TemplateAnswer> findByCategoryAndSubcategory(String category, String subcategory);

    @Query("SELECT DISTINCT t.subcategory FROM TemplateAnswer t WHERE t.category = :category AND t.subcategory IS NOT NULL")
    List<String> findDistinctSubcategoriesByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT t.category FROM TemplateAnswer t ORDER BY t.category")
    List<String> findDistinctCategories();
}
