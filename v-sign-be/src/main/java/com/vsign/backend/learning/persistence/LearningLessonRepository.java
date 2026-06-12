package com.vsign.backend.learning.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningLessonRepository extends JpaRepository<LearningLessonEntity, String> {
    List<LearningLessonEntity> findByChapterIdAndPublishedTrueOrderByOrderIndexAsc(String chapterId);

    Optional<LearningLessonEntity> findByLessonIdAndPublishedTrue(String lessonId);

    long countByChapterIdAndPublishedTrue(String chapterId);

    /** Batch lesson counts for a set of chapters — replaces per-chapter COUNT in listChapters(). */
    @Query("SELECT l.chapterId, COUNT(l) FROM LearningLessonEntity l WHERE l.published = true AND l.chapterId IN :chapterIds GROUP BY l.chapterId")
    List<Object[]> countPublishedByChapterIdIn(@Param("chapterIds") Collection<String> chapterIds);

    /** Fetch all published lessons for a set of chapters in one query — used for batch completion calc. */
    @Query("SELECT l FROM LearningLessonEntity l WHERE l.published = true AND l.chapterId IN :chapterIds ORDER BY l.chapterId ASC, l.orderIndex ASC")
    List<LearningLessonEntity> findPublishedByChapterIdIn(@Param("chapterIds") Collection<String> chapterIds);
}
