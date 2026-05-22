package com.kaankaplan.movieService.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class UserAuthorizationClient {

    private static final String USER_SERVICE_BASE_URL = "http://user-service/api/user/users";

    private final WebClient.Builder webClientBuilder;

    public boolean isAdmin(String token) {
        return getAuthorizationResult("/isUserAdmin", token);
    }

    public boolean isCustomer(String token) {
        return getAuthorizationResult("/isUserCustomer", token);
    }

    private boolean getAuthorizationResult(String path, String token) {
        Boolean result = webClientBuilder.build().get()
                .uri(USER_SERVICE_BASE_URL + path)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        return Boolean.TRUE.equals(result);
    }
}
