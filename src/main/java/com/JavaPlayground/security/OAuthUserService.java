package com.JavaPlayground.security;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.JavaPlayground.model.User;
import com.JavaPlayground.repository.UserRepository;

@Service
public class OAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuthUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            // 1. Load user from Google/GitHub
            OAuth2User oauthUser = super.loadUser(userRequest);
            System.out.println("OAuth User Loaded: " + oauthUser.getAttributes());

            // 2. Extract Details
            String provider = userRequest.getClientRegistration().getRegistrationId();
            String providerId = oauthUser.getName();
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");

            // GitHub fallback
            if (name == null) {
                name = oauthUser.getAttribute("login");
            }
            // Email fallback (if provider returns null)
            if (email == null) {
                email = name + "@" + provider + ".com";
            }

            // 3. Save to DB
            saveOrUpdateUser(email, name, provider, providerId);

            return oauthUser;

        } catch (Exception e) {
            // Log the error so we know why login failed
            e.printStackTrace();
            throw new OAuth2AuthenticationException("Login failed: " + e.getMessage());
        }
    }

    private void saveOrUpdateUser(String email, String name, String provider, String providerId) {
        Optional<User> existingUser = userRepository.findByEmailAndProvider(email, provider);

        if (existingUser.isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProvider(provider);
            user.setProviderId(providerId);
            userRepository.save(user);
            System.out.println("New User Saved: " + email);
        } else {
            User user = existingUser.get();
            user.setName(name);
            userRepository.save(user);
            System.out.println("User Updated: " + email);
        }
    }
}
