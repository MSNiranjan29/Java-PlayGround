package com.JavaPlayground.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/user")
    public Map<String, Object> getUser(Authentication authentication) {
        // If no user is logged in, return loggedIn: false immediately.
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of("loggedIn", false);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String name = oauthUser.getAttribute("name");
            if (name == null) {
                name = oauthUser.getAttribute("login");
            }

            String picture = oauthUser.getAttribute("avatar_url"); // GitHub
            if (picture == null) {
                picture = oauthUser.getAttribute("picture"); // Google
            }
            return Map.of(
                    "loggedIn", true,
                    "name", name != null ? name : "User",
                    "avatar", picture != null ? picture : ""
            );
        }

        return Map.of("loggedIn", false);
    }
}
