package com.JavaPlayground.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.JavaPlayground.security.OAuthUserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuthUserService oAuthUserService;

    public SecurityConfig(OAuthUserService oAuthUserService) {
        this.oAuthUserService = oAuthUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", // <--- Allow Root URL
                        "/index.html", // <--- Allow the actual file
                        "/login", // Allow Login URL
                        "/login.html", // Allow Login file
                        "/user", // Allow Auth Check
                        "/error", // Allow Error pages
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/api/**",
                        "/terminal"
                ).permitAll()
                .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuthUserService))
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
                )
                .logout(logout -> logout
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .permitAll()
                );

        return http.build();
    }
}
