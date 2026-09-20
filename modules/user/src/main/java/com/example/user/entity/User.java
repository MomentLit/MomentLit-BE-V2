package com.example.user.entity;

import com.example.user.global.exception.DeletedUserException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "users", schema = "users")
public class User {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true)
    private String phone;

    private String intro;

    @Column(name = "auth_provider")
    private String authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static User create(String email, String password, String name, String phone) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .role(Role.USER)
                .build();
    }

    public static User createOauth(
            String email,
            String name,
            String imageUrl,
            String authProvider,
            String providerId
    ) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .name(name)
                .imageUrl(imageUrl)
                .role(Role.USER)
                .authProvider(authProvider)
                .providerId(providerId)
                .build();
    }


    public void update(String name, String imageUrl, String phone, String intro) {
        if (this.deletedAt != null) {
            throw new DeletedUserException("탈퇴한 계정입니다.");
        }
        if (name != null) this.name = name;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (phone != null) this.phone = phone;
        if (intro != null) this.intro = intro;
    }

    public void delete() {
        if (this.deletedAt != null) {
            throw new DeletedUserException("탈퇴한 계정입니다.");
        }
        this.deletedAt = LocalDateTime.now();
    }
}
