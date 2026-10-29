package vn.edu.ute.utelearn;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.CustomUserDetailsService;
import vn.edu.ute.utelearn.security.JwtAuthenticationFilter;
import vn.edu.ute.utelearn.security.JwtTokenProvider;
import vn.edu.ute.utelearn.service.impl.AuthServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockedUserSecurityTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLockedUser_ApiRequest_KickedOutWith401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/unread-count");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid-token")).thenReturn("locked_user");
        when(userDetailsService.loadUserByUsername("locked_user"))
            .thenThrow(new DisabledException("Account disabled"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_LOCKED"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testLockedUser_PageRequest_RedirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid-token")).thenReturn("locked_user");
        when(userDetailsService.loadUserByUsername("locked_user"))
            .thenThrow(new DisabledException("Account disabled"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/login?error=account_locked", response.getRedirectedUrl());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testGetCurrentAuthenticatedUser_WhenLocked_ReturnsEmpty() {
        User lockedUser = new User();
        lockedUser.setId(10L);
        lockedUser.setUsername("locked_user");
        lockedUser.setIsActive(false);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("locked_user", null, java.util.List.of())
        );

        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(lockedUser));

        Optional<User> result = authService.getCurrentAuthenticatedUser();

        assertTrue(result.isEmpty());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
