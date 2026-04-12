package com.boatmanagement.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to enable Docker-dependent tests only when Docker is available.
 *
 * <p>Use this annotation on a test class or method that relies on Testcontainers.
 * When Docker is unavailable, the tests are skipped instead of failing.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {

    /**
     * Message shown when the condition disables the annotated test.
     */
    String disabledReason() default "Docker is not available";
}
