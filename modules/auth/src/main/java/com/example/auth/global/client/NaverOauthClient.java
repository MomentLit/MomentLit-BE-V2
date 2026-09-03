package com.example.auth.global.client;

import com.example.auth.global.client.dto.response.NaverTokenResponse;
import com.example.auth.global.client.dto.response.NaverUserInfoResponse;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverOauthClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;

    public NaverOauthClient(
            @Value("${oauth.naver.client-id}") String clientId,
            @Value("${oauth.naver.client-secret}") String clientSecret,
            @Value("${oauth.naver.redirect-uri}") String redirectUri,
            @Value("${oauth.naver.authorization-uri}") String authorizationUri,
            @Value("${oauth.naver.token-uri}") String tokenUri,
            @Value("${oauth.naver.user-info-uri}") String userInfoUri
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    public URI createAuthorizationUri(String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri);

        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }

        return builder.build()
                .encode()
                .toUri();
    }

    public NaverTokenResponse requestToken(String code, String state) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);

        if (StringUtils.hasText(state)) {
            body.add("state", state);
        }

        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(NaverTokenResponse.class);
    }

    public NaverUserInfoResponse requestUserInfo(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(NaverUserInfoResponse.class);
    }
}
