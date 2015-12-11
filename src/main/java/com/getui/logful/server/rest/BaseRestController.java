package com.getui.logful.server.rest;

import com.getui.logful.server.util.DateTimeUtil;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BaseRestController {

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class BadRequestException extends RuntimeException {
        public BadRequestException() {
            super();
        }

        public BadRequestException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public class ForbiddenException extends RuntimeException {
        public ForbiddenException() {
            super();
        }

        public ForbiddenException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class NotFoundException extends RuntimeException {
        public NotFoundException() {
            super();
        }

        public NotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public class NotAcceptableException extends RuntimeException {
        public NotAcceptableException() {
            super();
        }

        public NotAcceptableException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.GONE)
    public class GoneException extends RuntimeException {
        public GoneException() {
            super();
        }

        public GoneException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
    public class UnprocessableEntityException extends RuntimeException {
        public UnprocessableEntityException() {
            super();
        }

        public UnprocessableEntityException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public class InternalServerException extends RuntimeException {
        public InternalServerException() {
            super();
        }

        public InternalServerException(String message) {
            super(message);
        }
    }

    public String ok() {
        JSONObject object = new JSONObject();
        object.put("status", "200 OK");
        return object.toString();
    }

    public String deleted() {
        JSONObject object = new JSONObject();
        object.put("status", "204 DELETED");
        return object.toString();
    }

    public String created() {
        JSONObject object = new JSONObject();
        object.put("status", "201 Created");
        return object.toString();
    }

    public String updated() {
        JSONObject object = new JSONObject();
        object.put("updatedAt", DateTimeUtil.timeString(System.currentTimeMillis()));
        return object.toString();
    }
}
