package com.example.suggestion.service;

import com.example.common.dto.PageResponse;
import com.example.common.security.Role;
import com.example.suggestion.dto.request.SuggestionCreateRequest;
import com.example.suggestion.dto.response.SuggestionAdminResponse;
import com.example.suggestion.dto.response.SuggestionResponse;
import com.example.suggestion.entity.Suggestion;
import com.example.suggestion.global.exception.SuggestionForbiddenException;
import com.example.suggestion.global.exception.SuggestionNotFoundException;
import com.example.suggestion.repository.SuggestionRepository;
import com.example.user.api.UserInternalApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserInternalApi userInternalApi;

    @Transactional
    public SuggestionResponse createSuggestion(String userId, SuggestionCreateRequest request) {
        Suggestion suggestion = Suggestion.create(userId, request.title(), request.content());
        suggestionRepository.save(suggestion);
        return SuggestionResponse.from(suggestion);
    }

    public PageResponse<SuggestionResponse> getMySuggestions(String userId, Pageable pageable) {
        Page<Suggestion> page = suggestionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, SuggestionResponse::from);
    }

    public PageResponse<SuggestionAdminResponse> getAllSuggestions(Role role, Pageable pageable) {
        requireAdmin(role);
        Page<Suggestion> page = suggestionRepository.findAll(pageable);
        return PageResponse.from(page, this::toAdminResponse);
    }

    @Transactional
    public SuggestionAdminResponse answerSuggestion(Role role, Long suggestionId, String answerContent) {
        requireAdmin(role);
        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new SuggestionNotFoundException("건의를 찾을 수 없습니다."));
        suggestion.answer(answerContent);
        return toAdminResponse(suggestion);
    }

    private SuggestionAdminResponse toAdminResponse(Suggestion suggestion) {
        String userName = userInternalApi.getUserName(suggestion.getUserId()).name();
        return SuggestionAdminResponse.from(suggestion, userName);
    }

    private void requireAdmin(Role role) {
        if (role != Role.ADMIN) {
            throw new SuggestionForbiddenException("관리자만 가능합니다.");
        }
    }
}
