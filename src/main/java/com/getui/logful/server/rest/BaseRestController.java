package com.getui.logful.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.mongod.QueryCondition;
import com.getui.logful.server.util.DateTimeUtil;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;

public class BaseRestController {

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

    public Query queryCondition(WebRequest request) {
        QueryCondition condition = new QueryCondition(request);
        Query query = new Query();
        String[] keys = {"id", "clientId", "uid", "alias", "model", "imei", "macAddress", "osVersion", "appId", "versionString"};
        for (String key : keys) {
            String temp = request.getParameter(key);
            if (StringUtils.isNotEmpty(temp)) {
                query.addCriteria(Criteria.where(key).is(temp));
            }
        }

        String platformString = request.getParameter("platform");
        if (StringUtils.isNumeric(platformString)) {
            int platform = Integer.parseInt(platformString);
            if (platform != 0) {
                query.addCriteria(Criteria.where("platform").is(platform));
            }
        }

        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
            try {
                Date start = DateTimeUtil.dayStart(DateTime.parse(startDate).toDate());
                Date end = DateTimeUtil.dayEnd(DateTime.parse(endDate).toDate());
                query.addCriteria(Criteria.where("date").gte(start).lte(end));
            } catch (Exception e) {
                throw new BadRequestException();
            }
        }

        String levelString = request.getParameter("level");
        if (StringUtils.isNotEmpty(levelString)) {
            short level = Short.parseShort(levelString);
            if (level != 0) {
                query.addCriteria(Criteria.where("level").is(level));
            }
        }

        query.with(new Sort(condition.getOrder(), condition.getSort()));
        query.skip(condition.getOffset()).limit(condition.getLimit());

        return query;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class BadRequestException extends RuntimeException {
        public BadRequestException() {
            super();
        }

        public BadRequestException(String message) {
            super(message);
        }
    }

    public String listToJson(List<?> list) {
        if (list != null) {
            try {
                StringWriter writer = new StringWriter();
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(writer, list);
                writer.close();
                return writer.toString();
            } catch (Exception e) {
                throw new InternalServerException();
            }
        }
        throw new NotFoundException();
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
}
