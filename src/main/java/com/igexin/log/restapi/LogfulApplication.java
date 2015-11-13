package com.igexin.log.restapi;

import com.igexin.log.restapi.util.ExpandNativeUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(LogfulProperties.class)
public class LogfulApplication {

    public static void main(String[] args) {
        ExpandNativeUtil.expand();
        SpringApplication.run(LogfulApplication.class, args);
    }

}
