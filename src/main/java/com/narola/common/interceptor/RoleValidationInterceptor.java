package com.narola.common.interceptor;

import com.narola.common.CurrentUserContext;
import com.narola.common.Message;
import com.narola.common.annotations.RoleAllowed;
import com.narola.common.enums.RoleType;
import com.narola.common.exception.AuthException;
import com.narola.common.utils.JwtUtils;
import com.narola.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;

import static com.narola.common.constants.AppConstants.*;

public class RoleValidationInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Autowired
    public RoleValidationInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RoleAllowed roleAllowed = findRoleAllowedAnnotation(method);
        if (roleAllowed == null) {
            return true;
        }

        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            throw new AuthException(Message.Auth.TOKEN_EXPIRED_PLEASE_LOGIN);
        }

        String token = authHeader.substring(7);
        User user = jwtUtils.parseToken(token);

        if (user == null || user.getRole() == null) {
            throw new AuthException(Message.Auth.PLEASE_LOGIN);
        }

        for (RoleType allowed : roleAllowed.value()) {
            if (allowed == user.getRole().getRoleType()) {
                request.setAttribute("user", user);
                CurrentUserContext.setCurrentUserId(String.valueOf(user.getUserId()));
                CurrentUserContext.setCurrentUser(user);
                return true;
            }
        }

        throw new AuthException(Message.Auth.UNAUTHORIZED);
    }

    private RoleAllowed findRoleAllowedAnnotation(HandlerMethod method) {
        for (Annotation annotation : method.getMethod().getAnnotations()) {
            RoleAllowed roleAllowed = annotation.annotationType().getAnnotation(RoleAllowed.class);
            if (roleAllowed != null) return roleAllowed;
        }
        for (Annotation annotation : method.getBeanType().getAnnotations()) {
            RoleAllowed roleAllowed = annotation.annotationType().getAnnotation(RoleAllowed.class);
            if (roleAllowed != null) return roleAllowed;
        }
        return null;
    }
}
