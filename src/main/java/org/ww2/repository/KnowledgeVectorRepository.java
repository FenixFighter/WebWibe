package org.ww2.repository;

import org.ww2.entity.KnowledgeVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeVectorRepository extends JpaRepository<KnowledgeVector, Long> {
    
    @Query(value = """
        SELECT *, embedding <-> CAST(:queryVector AS vector) AS distance
        FROM knowledge_vectors 
        WHERE category = :category OR :category IS NULL
        ORDER BY embedding <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarVectors(@Param("queryVector") String queryVector, 
                                    @Param("category") String category, 
                                    @Param("limit") int limit);
    
    @Query(value = """
        SELECT *, embedding <-> CAST(:queryVector AS vector) AS distance
        FROM knowledge_vectors 
        ORDER BY embedding <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarVectorsAll(@Param("queryVector") String queryVector, 
                                       @Param("limit") int limit);
    
    List<KnowledgeVector> findByCategory(String category);
    
    List<KnowledgeVector> findByCategoryIgnoreCase(String category);
    
    @Query("SELECT DISTINCT k.category FROM KnowledgeVector k WHERE k.category IS NOT NULL")
    List<String> findDistinctCategories();
}
