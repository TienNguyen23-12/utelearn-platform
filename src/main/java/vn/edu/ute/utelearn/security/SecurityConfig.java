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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtTokenProvider tokenProvider;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/ws/**",
                                "/oauth2/**",
                                "/admin/api/**",
                                "/admin/orders/api/**",
                                "/certificates/api/**",
                                "/checkout/payment/**",
                                "/cart/**",
                                "/instructor/assets/**"
                        ))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/learn",
                                "/courses/**",
                                "/course/**",
                                "/cohorts",
                                "/cohorts/**",
                                "/cohort/**",
                                "/modules/**",
                                "/module/**",
                                "/certificates/**",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/auth/**",
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/vendor/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/favicon.png",
                                "/apple-touch-icon.png",
                                "/error",
                                "/api/health",
                                "/api/ping",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/courses",
                                "/api/courses/**",
                                "/api/cohorts",
                                "/api/cohorts/**",
                                "/api/webhooks/**",
                                "/api/quizzes/**",
                                "/api/assignments/**",
                                "/api/notes",
                                "/api/notes/**",
                                "/oj/**",
                                "/ws",
                                "/ws/**")
                        .permitAll()

                        .requestMatchers("/admin/orders/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "FINANCE", "ROLE_FINANCE")
                        .requestMatchers("/admin/**", "/dashboard/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/api/instructor/assignments/**")
                        .hasAnyAuthority("INSTRUCTOR", "ROLE_INSTRUCTOR", "ADMIN", "ROLE_ADMIN", "MODERATOR", "ROLE_MODERATOR")
                        .requestMatchers("/instructor/**", "/api/instructor/**")
                        .hasAnyAuthority("INSTRUCTOR", "ROLE_INSTRUCTOR", "ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/moderator/**")
                        .hasAnyAuthority("MODERATOR", "ROLE_MODERATOR", "ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/student/**", "/profile/**", "/orders/**").authenticated()

                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2AuthenticationSuccessHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI()))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("UTELearn_Token", "JSESSIONID")
                        .addLogoutHandler((request, response, authentication) -> {
                            tokenProvider.clearTokenCookie(response);
                        })
                        .permitAll());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
