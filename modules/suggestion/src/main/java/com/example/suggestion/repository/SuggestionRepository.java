package com.example.suggestion.repository;

import com.example.suggestion.entity.Suggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    Page<Suggestion> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
