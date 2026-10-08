package com.coloc.back.repository;

import com.coloc.back.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findBySharedHouseIdAndOwnerIdAndBoughtFalseOrderByIdAsc(Long sharedHouseId, Long ownerId);

    List<Article> findBySharedHouseIdAndOwnerIsNullAndBoughtFalseOrderByIdAsc(Long sharedHouseId);

    Optional<Article> findByIdAndSharedHouseId(Long id, Long sharedHouseId);
}