package com.boatmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pool for asynchronous audit logging.
 *
 * Why a separate pool?
 * - Audit writes are I/O-bound (DB insert).  Sharing the HTTP thread pool
 *   would allow slow audit inserts to exhaust threads serving real requests.
 * - A bounded queue (capacity 500) acts as a buffer during write spikes and
 *   applies back-pressure rather than creating unlimited threads.
 * - CallerRunsPolicy means: if the queue is full, the HTTP thread itself
 *   executes the audit write — we degrade gracefully instead of dropping logs.
 *
 * Sizing (defaults tunable via application.yml):
 *   core=2, max=4, queue=500
 * These are conservative defaults suitable for a low-to-medium traffic app.
 * Adjust audit.async.* properties in production based on observed throughput.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Value("${audit.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${audit.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${audit.async.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("audit-");
        // CallerRunsPolicy: never drop an audit record — fall back to inline execution.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Catch-all for unhandled exceptions thrown from @Async methods.
     * AuditLogService already swallows its own exceptions, but this handler
     * is a final safety net that logs anything that escapes.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Unhandled exception in async method {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
