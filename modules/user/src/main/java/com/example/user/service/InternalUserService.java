package com.example.user.service;

import com.example.user.dto.request.SignInRequest;
import com.example.user.dto.request.UserOauthRequest;
import com.example.user.dto.response.UserAuthResponse;
import com.example.user.dto.response.UserNameResponse;
import com.example.user.api.UserInternalApi;
import com.example.user.entity.User;
import com.example.user.global.exception.BadRequestException;
import com.example.user.global.exception.DeletedUserException;
import com.example.user.global.exception.DuplicateEmailException;
import com.example.user.global.exception.InvalidPasswordException;
import com.example.user.global.exception.UserNotFoundException;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InternalUserService implements UserInternalApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserAuthResponse authenticate(SignInRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("유저 없음"));

        validateActiveUser(user);
        validatePassword(request.password(), user.getPassword());

        return UserAuthResponse.from(user);
    }

    @Transactional
    public UserAuthResponse authenticateOauth(UserOauthRequest request) {
        validateOauthRequest(request);

        User user = findOrCreateOauthUser(request);

        return UserAuthResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserNameResponse getUserName(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저 없음"));

        validateActiveUser(user);

        return UserNameResponse.from(user);
    }

    // 삭제 여부 확인
    private void validateActiveUser(User user) {
        if (user.getDeletedAt() != null) {
            throw new DeletedUserException("삭제된 유저");
        }
    }

    // 비밀번호 확인
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }
    }

    private void validateOauthRequest(UserOauthRequest request) {
        if (request == null
                || !StringUtils.hasText(request.provider())
                || !StringUtils.hasText(request.providerId())) {
            throw new BadRequestException("OAuth 사용자 정보를 확인할 수 없습니다.");
        }

        if (!StringUtils.hasText(request.email())) {
            throw new BadRequestException("이메일 제공 동의가 필요합니다.");
        }

        if (request.emailVerified() != null && !Boolean.TRUE.equals(request.emailVerified())) {
            throw new BadRequestException(request.provider() + " 이메일 인증이 필요합니다.");
        }
    }

    private User findOrCreateOauthUser(UserOauthRequest request) {
        return userRepository
                .findByAuthProviderAndProviderIdAndDeletedAtIsNull(request.provider(), request.providerId())
                .orElseGet(() -> createOauthUser(request));
    }

    private User createOauthUser(UserOauthRequest request) {
        userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .ifPresent(existingUser -> {
                    throw new DuplicateEmailException("이미 가입된 이메일입니다.");
                });

        User user = User.createOauth(
                request.email(),
                request.name(),
                request.imageUrl(),
                request.provider(),
                request.providerId()
        );

        return userRepository.save(user);
    }
}
