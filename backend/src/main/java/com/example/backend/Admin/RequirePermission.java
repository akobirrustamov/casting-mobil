package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Endpoint uchun kerakli ruxsat yoki minimal rol.
 *
 * <h2>Nega annotatsiya</h2>
 * Ilgari har bir metod ichida {@code require(Permission.X)} chaqirilardi.
 * Bu ishlaydi, lekin bitta chaqiruvni unutish yetarli — va endpoint
 * himoyasiz qoladi, buni ko'rish uchun esa metod tanasini o'qish kerak.
 *
 * Annotatsiya tekshiruvni metod IMZOSIGA chiqaradi: kod ko'rigida darhol
 * ko'rinadi, va {@code AdminEndpointGuardTest} har bir endpointda uning
 * borligini tekshiradi.
 *
 * @see PermissionAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** Kerakli ruxsat. Bo'sh bo'lsa {@link #role()} ishlatiladi. */
    Permission[] value() default {};

    /** Minimal rol. {@code USER} — tekshirilmaydi degani. */
    PlatformRole role() default PlatformRole.USER;
}
