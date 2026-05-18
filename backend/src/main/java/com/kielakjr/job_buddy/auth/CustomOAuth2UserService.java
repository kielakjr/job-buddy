package com.kielakjr.job_buddy.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;

import com.kielakjr.job_buddy.user.OAuthProfile;
import com.kielakjr.job_buddy.user.User;
import com.kielakjr.job_buddy.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String PRINCIPAL_USER_ID_ATTR = "userId";

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        var oauthUser = delegate.loadUser(request);
        var provider = request.getClientRegistration().getRegistrationId();
        var profile = toProfile(provider, oauthUser.getAttributes());

        if (profile.email() == null) {
            throw new OAuth2AuthenticationException("Email not provided by " + provider);
        }

        User user = userService.upsertFromOAuth(profile);

        Map<String, Object> enriched = new HashMap<>(oauthUser.getAttributes());
        enriched.put(PRINCIPAL_USER_ID_ATTR, user.getId().toString());

        return new DefaultOAuth2User(
                Collections.singleton(new OAuth2UserAuthority(enriched)), enriched, PRINCIPAL_USER_ID_ATTR);
    }

    private static OAuthProfile toProfile(String provider, Map<String, Object> a) {
        return switch (provider) {
            case "google", "linkedin" ->
                new OAuthProfile(
                        provider, str(a.get("sub")), str(a.get("email")), str(a.get("name")), str(a.get("picture")));
            default -> new OAuthProfile(provider, str(a.get("sub")), str(a.get("email")), str(a.get("name")), null);
        };
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
