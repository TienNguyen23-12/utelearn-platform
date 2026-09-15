package vn.edu.ute.utelearn.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/home",
                    "/courses/**",
                    "/login",
                    "/register",
                    "/auth/**",
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/vendor/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // CHỈ ADMIN MỚI ĐƯỢC VÀO TRANG QUẢN TRỊ
                .requestMatchers("/admin/**", "/dashboard/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers("/instructor/**").hasAnyAuthority("INSTRUCTOR", "ROLE_INSTRUCTOR", "ADMIN", "ROLE_ADMIN")
                .requestMatchers("/moderator/**").hasAnyAuthority("MODERATOR", "ROLE_MODERATOR", "ADMIN", "ROLE_ADMIN")
                .requestMatchers("/student/**", "/profile/**").authenticated()

                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("UTELearn_Token", "JSESSIONID")
                .permitAll()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
