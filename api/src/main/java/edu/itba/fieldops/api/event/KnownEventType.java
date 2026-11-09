package edu.itba.fieldops.api.event;

import edu.itba.fieldops.domain.event.EventTypes;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = KnownEventType.Validator.class)
public @interface KnownEventType {
    String message() default "must be one of the known event types";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    final class Validator implements ConstraintValidator<KnownEventType, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || EventTypes.known().contains(value);
        }
    }
}
