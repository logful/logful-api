package com.igexin.log.restapi;

import com.igexin.log.restapi.util.ExpandNativeUtil;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(LogfulProperties.class)
public class LogfulApplication implements AsyncConfigurer {

    public static void main(String[] args) {
        ExpandNativeUtil.expand();
        SpringApplication.run(LogfulApplication.class, args);
    }

    @Override
    @Bean
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(Constants.THREAD_POOL_SIZE);
        taskExecutor.setQueueCapacity(Constants.QUEUE_CAPACITY);
        taskExecutor.setThreadNamePrefix("logful-api-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
