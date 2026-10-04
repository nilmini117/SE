package com.driveflow.demo_driveflow.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation that verifies:
 * 1. The email conforms to standard syntactic email pattern.
 * 2. The domain portion exists and possesses valid MX (Mail Exchange) DNS records.
 *
 * Automatically rejects fake, unregistered, or inactive domains with 400 Bad Request.
 */
@Documented
@Constraint(validatedBy = ValidDomainEmailValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDomainEmail {

    String message() default "Please enter a valid email address with an active domain.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
