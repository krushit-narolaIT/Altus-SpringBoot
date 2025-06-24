package com.narola.common.annotations;

import com.narola.common.enums.RoleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RoleAllowed(RoleType.ROLE_SUPER_ADMIN)
public @interface AdminOnly {}
