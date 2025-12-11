package com.mybible.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import java.io.IOException;

/**
 * RequestContext - Stateless request context extraction from JWT tokens
 *
 * This class provides a bridge between session-based and JWT-based auth.
 * It tries JWT first, falls back to session for backward compatibility.
 *
 * Usage:
 *   RequestContext ctx = RequestContext.fromRequest(request);
 *   if (!ctx.isAuthenticated()) {
 *       response.setStatus(401);
 *       return;
 *   }
 *   String userId = ctx.getUserId();
 */
public class RequestContext {

    // Instance fields
    private String userId;
    private String email;
    private String role;
    private String name;
    private boolean authenticated;
    private String authMethod;  // "jwt" or "session"

    /**
     * Private constructor - use fromRequest() factory method
     */
    private RequestContext() {
    }

    /**
     * Extract context from request (JWT first, then session)
     *
     * @param request The HTTP servlet request
     * @return RequestContext with user information if authenticated
     */
    public static RequestContext fromRequest(HttpServletRequest request) {
        RequestContext ctx = new RequestContext();

        // Try JWT from Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                JWTUtil.JWTPayload payload = JWTUtil.verify(token);

                ctx.userId = payload.userId;
                ctx.email = payload.email;
                ctx.role = payload.role;
                ctx.authenticated = true;
                ctx.authMethod = "jwt";

                return ctx;
            } catch (Exception e) {
                // JWT invalid - fall through to cookie/session
                System.out.println("[RequestContext] JWT header verification failed: " + e.getMessage());
            }
        }

        // Try JWT from cookie (for browser-based auth without sessions)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    try {
                        String token = cookie.getValue();
                        JWTUtil.JWTPayload payload = JWTUtil.verify(token);

                        ctx.userId = payload.userId;
                        ctx.email = payload.email;
                        ctx.role = payload.role;
                        ctx.authenticated = true;
                        ctx.authMethod = "jwt-cookie";

                        return ctx;
                    } catch (Exception e) {
                        // Invalid cookie token - continue to session
                        System.out.println("[RequestContext] JWT cookie verification failed: " + e.getMessage());
                    }
                    break;
                }
            }
        }

        // Fall back to session (for backward compatibility during transition)
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            ctx.userId = (String) session.getAttribute("userId");
            ctx.email = (String) session.getAttribute("email");
            ctx.name = (String) session.getAttribute("name");
            ctx.role = (String) session.getAttribute("role");
            ctx.authenticated = true;
            ctx.authMethod = "session";

            return ctx;
        }

        // Not authenticated
        ctx.authenticated = false;
        return ctx;
    }

    // Getters
    public String getUserId() {
        return this.userId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return this.name;
    }

    public String getRole() {
        return this.role;
    }

    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public String getAuthMethod() {
        return this.authMethod;
    }

    public boolean isAdmin() {
        return "admin".equals(this.role);
    }

    /**
     * Check auth and send 401 if not authenticated
     *
     * @param response The HTTP servlet response
     * @return true if authenticated, false if 401 was sent
     */
    public boolean requireAuth(HttpServletResponse response) {
        if (!this.authenticated) {
            try {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}}");
            } catch (IOException e) {
                // Ignore write errors
            }
            return false;
        }
        return true;
    }

    /**
     * Check auth and redirect to login if not authenticated (for HTML pages)
     *
     * @param response The HTTP servlet response
     * @param loginPath The path to redirect to (e.g., "/login")
     * @return true if authenticated, false if redirect was sent
     */
    public boolean requireAuthOrRedirect(HttpServletResponse response, String loginPath) {
        if (!this.authenticated) {
            try {
                response.sendRedirect(loginPath);
            } catch (IOException e) {
                // Ignore redirect errors
            }
            return false;
        }
        return true;
    }

    /**
     * String representation for debugging
     */
    @Override
    public String toString() {
        return String.format("RequestContext[authenticated=%s, method=%s, userId=%s, email=%s, role=%s]",
            authenticated, authMethod, userId, email, role);
    }
}
