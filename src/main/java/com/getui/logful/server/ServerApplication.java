package com.getui.logful.server;

import com.getui.logful.server.util.ExpandNativeUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.velocity.VelocityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.security.Security;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(ServerProperties.class)
@EnableAutoConfiguration(exclude = VelocityAutoConfiguration.class)
public class ServerApplication extends WebMvcConfigurerAdapter implements AsyncConfigurer {

    @Autowired
    ServerProperties serverProperties;

    @Override
    @Bean
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(serverProperties.getParser().getMaxThreads());
        taskExecutor.setMaxPoolSize(serverProperties.getParser().getMaxThreads());
        taskExecutor.setQueueCapacity(serverProperties.getParser().getQueueCapacity());
        taskExecutor.setThreadNamePrefix("logful-server-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        ExpandNativeUtil.expand();
        SpringApplication.run(ServerApplication.class, args);
    }
}
