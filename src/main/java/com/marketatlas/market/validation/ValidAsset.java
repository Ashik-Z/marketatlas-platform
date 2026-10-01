package com.marketatlas.market.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint (validatedBy = AssetValidator.class)
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface ValidAsset {
    String message() default "Invalid asset";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
}
