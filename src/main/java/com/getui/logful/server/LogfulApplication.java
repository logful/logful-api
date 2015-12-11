package com.getui.logful.server;

import com.getui.logful.server.util.ExpandNativeUtil;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(LogfulProperties.class)
public class LogfulApplication implements AsyncConfigurer {

    @Autowired
    LogfulProperties logfulProperties;

    public static void main(String[] args) {
        ExpandNativeUtil.expand();
        SpringApplication.run(LogfulApplication.class, args);
    }

    @Override
    @Bean
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(logfulProperties.getParser().getMaxThreads());
        taskExecutor.setMaxPoolSize(logfulProperties.getParser().getMaxThreads());
        taskExecutor.setQueueCapacity(logfulProperties.getParser().getQueueCapacity());
        taskExecutor.setThreadNamePrefix("logful-api-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
