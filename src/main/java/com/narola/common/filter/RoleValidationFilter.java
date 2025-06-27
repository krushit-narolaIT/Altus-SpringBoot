package com.narola.common.filter;

import com.narola.common.CurrentUserContext;
import com.narola.common.Message;
import com.narola.common.annotations.RoleAllowed;
import com.narola.common.enums.RoleType;
import com.narola.common.exception.AuthException;
import com.narola.common.utils.JwtUtils;
import com.narola.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.lang.annotation.Annotation;

import static com.narola.common.constants.AppConstants.*;

@Component
public class RoleValidationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public RoleValidationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Object handler = request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingHandler");

        if (!(handler instanceof HandlerMethod method)) {
            filterChain.doFilter(request, response);
            return;
        }

        RoleAllowed roleAllowed = findRoleAllowedAnnotation(method);
        if (roleAllowed == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION);

        try {
            if (authHeader == null || !authHeader.startsWith(BEARER)) {
                throw new AuthException(Message.Auth.TOKEN_EXPIRED_PLEASE_LOGIN);
            }

            String token = authHeader.substring(7);
            User user = jwtUtils.parseToken(token);

            if (user == null || user.getRole() == null) {
                throw new AuthException(Message.Auth.PLEASE_LOGIN);
            }

            RoleType userRole = user.getRole().getRoleType();
            if (isRoleAllowed(userRole, roleAllowed)) {
                request.setAttribute("user", user);
                CurrentUserContext.setCurrentUserId(String.valueOf(user.getUserId()));
                CurrentUserContext.setCurrentUser(user);
                filterChain.doFilter(request, response);
            } else {
                throw new AuthException(Message.Auth.UNAUTHORIZED);
            }
        } catch (AuthException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        }
    }

    private boolean isRoleAllowed(RoleType userRole, RoleAllowed roleAllowed) {
        for (RoleType allowed : roleAllowed.value()) {
            if (allowed == userRole) {
                return true;
            }
        }
        return false;
    }

    private RoleAllowed findRoleAllowedAnnotation(HandlerMethod method) {
        RoleAllowed roleAllowed = method.getMethodAnnotation(RoleAllowed.class);
        if (roleAllowed != null) {
            return roleAllowed;
        }
        return method.getBeanType().getAnnotation(RoleAllowed.class);
    }
}
