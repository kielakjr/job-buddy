package com.kielakjr.job_buddy.auth;

import com.kielakjr.job_buddy.user.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUser currentUser;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = currentUser.require(principal);
        return ResponseEntity.ok(new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl()
        ));
    }

    public record MeResponse(UUID id, String email, String name, String avatarUrl) {}
}
