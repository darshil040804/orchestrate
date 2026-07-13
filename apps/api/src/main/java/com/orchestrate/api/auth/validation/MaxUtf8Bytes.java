package com.orchestrate.api.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Validates that a string's UTF-8 encoded byte length is at most {@link #value()}. Character-count
 * constraints like {@code @Size(max = N)} are the wrong tool for BCrypt inputs: BCrypt truncates
 * (or misbehaves) at 72 *bytes*, and a multi-byte UTF-8 character (emoji, non-Latin scripts) can
 * push a string past that limit well before it hits N characters.
 */
@Documented
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
@Target({
  ElementType.METHOD,
  ElementType.FIELD,
  ElementType.ANNOTATION_TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.PARAMETER,
  ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

  int value();

  String message() default "must be at most {value} bytes when UTF-8 encoded";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int max;

    @Override
    public void initialize(MaxUtf8Bytes annotation) {
      this.max = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      // Null/blank is @NotBlank's job; this constraint only judges byte length.
      return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
  }
}
