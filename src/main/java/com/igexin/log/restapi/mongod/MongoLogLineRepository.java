package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.LogLine;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoLogLineRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoLogLineRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public LogLine findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, LogLine.class);
    }

    public LogLine save(LogLine logLine) {
        operations.save(logLine);
        return logLine;
    }

    public boolean delete(LogLine logLine) {
        Query query = new Query(Criteria.where("_id").is(logLine.getId()));
        WriteResult writeResult = operations.remove(query, LogLine.class);
        return writeResult.getN() > 0;
    }

    public List<LogLine> findAll() {
        return operations.findAll(LogLine.class);
    }

    public List<LogLine> findAllFailed() {
        Query query = new Query(Criteria.where("status").is(LogLine.STATE_FAILED));
        return operations.find(query, LogLine.class);
    }

    public List<LogLine> findAllFailedLimit(int limit) {
        Query query = new Query(Criteria.where("status").is(LogLine.STATE_FAILED));
        query.limit(limit);
        return operations.find(query, LogLine.class);
    }

}
