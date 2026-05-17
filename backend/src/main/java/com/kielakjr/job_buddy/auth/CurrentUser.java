package com.kielakjr.job_buddy.auth;

import com.kielakjr.job_buddy.user.User;
import com.kielakjr.job_buddy.user.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserService userService;

    public User require(OAuth2User principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }
        String id = principal.getAttribute(CustomOAuth2UserService.PRINCIPAL_USER_ID_ATTR);
        if (id == null) {
            throw new IllegalStateException(
                "Principal missing '" + CustomOAuth2UserService.PRINCIPAL_USER_ID_ATTR + "' attribute"
            );
        }
        return userService.getById(UUID.fromString(id));
    }
}
