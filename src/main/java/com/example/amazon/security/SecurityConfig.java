package com.example.amazon.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${amazon.admin.username}")
    private String adminUsername;

    @Value("${amazon.admin.password}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** /admin/** 専用。Basic認証で、管理者1人だけ通す。 */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http, PasswordEncoder encoder) throws Exception {
        UserDetails admin = User.withUsername(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        http
                .securityMatcher("/admin/**")
                .userDetailsService(new InMemoryUserDetailsManager(admin))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /** それ以外(購入者向け)。従来どおりフォームログイン。 */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/products/sync"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/signup", "/login", "/css/**","/error").permitAll()
                        .requestMatchers("/products/sync").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/products", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }
}