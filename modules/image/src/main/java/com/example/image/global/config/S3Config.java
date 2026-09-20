package com.example.image.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${cloud.aws.region}") String region,
            @Value("${cloud.aws.credentials.access-key:}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key:}") String secretKey
    ) {
        // 로컬 개발은 IAM 역할이 없어 .env의 정적 키를 쓰고, 운영(ECS/App Runner)은 그 값이 비어 있으니
        // SDK의 기본 체인(인스턴스/컨테이너 IAM 역할)으로 그대로 폴백한다 — 운영 동작은 바뀌지 않는다.
        AwsCredentialsProvider credentialsProvider = StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.create();

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
