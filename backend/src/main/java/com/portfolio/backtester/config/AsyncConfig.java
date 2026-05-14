package com.portfolio.backtester.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Value("${backtester.executor.core-pool-size:2}")
    private int corePoolSize;

    @Value("${backtester.executor.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${backtester.executor.queue-capacity:50}")
    private int queueCapacity;

    /**
     * Bounded executor for backtest jobs. We deliberately use platform threads
     * here (not virtual threads) because backtests are CPU-bound numeric work;
     * unbounded virtual-thread fan-out would oversubscribe cores. Virtual
     * threads remain enabled globally for I/O-bound work via
     * spring.threads.virtual.enabled.
     */
    @Bean(name = "backtestExecutor")
    public TaskExecutor backtestExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(corePoolSize);
        exec.setMaxPoolSize(maxPoolSize);
        exec.setQueueCapacity(queueCapacity);
        exec.setThreadNamePrefix("backtest-");
        exec.initialize();
        return exec;
    }

    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
