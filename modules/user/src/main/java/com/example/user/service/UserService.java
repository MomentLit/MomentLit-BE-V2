package com.example.user.service;

import com.example.user.dto.request.SignUpRequest;
import com.example.user.dto.request.UserUpdateRequest;
import com.example.user.dto.response.SignUpResponse;
import com.example.user.dto.response.UserSearchResponse;
import com.example.user.entity.User;
import com.example.user.global.exception.BadRequestException;
import com.example.user.global.exception.DeletedUserException;
import com.example.user.global.exception.DuplicateEmailException;
import com.example.user.global.exception.UserNotFoundException;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 (유저 생성)
    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 존재하는 이메일");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(
                request.email(),
                encodedPassword,
                request.name(),
                request.phone()
        );
        userRepository.save(user);

        return SignUpResponse.from(user);
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserSearchResponse getMyProfile(String userId) {
        User user = getActiveUser(userId);
        return UserSearchResponse.from(user);
    }

    // 수정
    @Transactional
    public void update(String userId, UserUpdateRequest request) {
        User user = getActiveUser(userId);
        if (request.name() != null && request.name().isBlank()) {
            throw new BadRequestException("이름은 비어있을 수 없음");
        }
        user.update(request.name(), request.imageUrl(), request.phone(), request.intro());
    }

    // 삭제
    @Transactional
    public void delete(String userId) {
        User user = getActiveUser(userId);
        user.delete();
    }

    // 삭제 안 된 유저 찾기
    private User getActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저 없음"));

        if (user.getDeletedAt() != null) {
            throw new DeletedUserException("삭제된 유저");
        }

        return user;
    }
}
