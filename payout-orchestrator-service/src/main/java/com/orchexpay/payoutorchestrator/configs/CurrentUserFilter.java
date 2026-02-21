package com.orchexpay.payoutorchestrator.configs;

import com.orchexpay.payoutorchestrator.dtos.UserProfile;
import com.orchexpay.payoutorchestrator.clients.WalletServiceMeClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * For /api/v1/payouts/**: resolves current user by calling user-wallet GET /me with request's Authorization.
 * Sets request attribute "currentUser". Returns 401 if no token or /me fails.
 */
@Component
@Order(1)
public class CurrentUserFilter extends OncePerRequestFilter {

    private static final String PAYOUT_API_PREFIX = "/api/v1/payouts";
    private static final String CURRENT_USER_ATTR = "currentUser";

    private final WalletServiceMeClient meClient;

    public CurrentUserFilter(WalletServiceMeClient meClient) {
        this.meClient = meClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(PAYOUT_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        try {
            UserProfile profile = meClient.getMe(auth != null ? auth : "");
            request.setAttribute(CURRENT_USER_ATTR, profile);
            filterChain.doFilter(request, response);
        } catch (WalletServiceMeClient.UnauthorizedException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}
