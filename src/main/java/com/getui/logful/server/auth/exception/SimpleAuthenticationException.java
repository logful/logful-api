package com.getui.logful.server.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class SimpleAuthenticationException extends AuthenticationException {

    public SimpleAuthenticationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public SimpleAuthenticationException(String msg) {
        super(msg);
    }
}
