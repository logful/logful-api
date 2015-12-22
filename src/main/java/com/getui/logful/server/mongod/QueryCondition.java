package com.getui.logful.server.mongod;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.request.WebRequest;

public class QueryCondition {

    private static final int DEFAULT_LIMIT = 20;

    private static final int DEFAULT_OFFSET = 0;

    private Sort.Direction order;

    private String sort;

    private int limit;

    private int offset;

    public QueryCondition(WebRequest request) {
        String sort = request.getParameter("sort");
        String order = request.getParameter("order");
        String limit = request.getParameter("limit");
        String offset = request.getParameter("offset");

        if (StringUtils.isEmpty(sort)) {
            this.sort = "_id";
        } else {
            this.sort = sort;
        }

        if (StringUtils.isEmpty(order)) {
            this.order = Sort.Direction.ASC;
        } else {
            if (StringUtils.equalsIgnoreCase(order, "asc")) {
                this.order = Sort.Direction.ASC;
            } else if (StringUtils.equalsIgnoreCase(order, "desc")) {
                this.order = Sort.Direction.DESC;
            } else {
                this.order = Sort.Direction.ASC;
            }
        }

        if (StringUtils.isNumeric(limit)) {
            this.limit = Integer.parseInt(limit);
        } else {
            this.limit = DEFAULT_LIMIT;
        }

        if (StringUtils.isNumeric(offset)) {
            this.offset = Integer.parseInt(offset);
        } else {
            this.offset = DEFAULT_OFFSET;
        }
    }

    public Sort.Direction getOrder() {
        return order;
    }

    public String getSort() {
        return sort;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
