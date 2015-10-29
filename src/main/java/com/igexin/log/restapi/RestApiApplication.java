package com.igexin.log.restapi;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.InetSocketAddress;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RestApiProperties.class)
public class RestApiApplication implements AsyncConfigurer {

    private static InetSocketAddress socketAddress = new InetSocketAddress(Constants.GRAY_LOG_ADDRESS, Constants.GRAY_LOG_PORT);
    private static GelfConfiguration config = new GelfConfiguration(socketAddress)
            .transport(GelfTransports.TCP)
            .queueSize(512)
            .connectTimeout(5000)
            .reconnectDelay(10000)
            .tcpNoDelay(true)
            .sendBufferSize(32768);
    public static GelfTransport transport = GelfTransports.create(config);

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }

    @Override
    @Bean
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(100);
        taskExecutor.setQueueCapacity(Constants.QUEUE_CAPACITY);
        taskExecutor.setThreadNamePrefix("logfile-process-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
