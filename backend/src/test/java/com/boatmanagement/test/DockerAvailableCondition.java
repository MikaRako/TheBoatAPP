package com.boatmanagement.test;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit execution condition for Docker-backed integration tests.
 *
 * <p>This condition checks whether a valid Docker environment is available via
 * Testcontainers. If Docker cannot be reached, the annotated test class or
 * test method is disabled instead of failing.</p>
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Docker is available");
    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("Docker is not available");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            return DockerClientFactory.instance().isDockerAvailable() ? ENABLED : DISABLED;
        } catch (Exception e) {
            return DISABLED;
        }
    }
}
