package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.LogMessage;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LogMessageRepository {

    private final MongoOperations operations;

    @Autowired
    public LogMessageRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public MongoOperations getOperations() {
        return operations;
    }

    public LogMessage findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, LogMessage.class);
    }

    public LogMessage save(LogMessage logMessage) {
        operations.save(logMessage);
        return logMessage;
    }

    public boolean delete(LogMessage logMessage) {
        Query query = new Query(Criteria.where("_id").is(logMessage.getId()));
        WriteResult writeResult = operations.remove(query, LogMessage.class);
        return writeResult.getN() > 0;
    }

    public List<LogMessage> findAll() {
        return operations.findAll(LogMessage.class);
    }

    public List<LogMessage> findAllNotSend() {
        Query query = new Query(Criteria.where("status").is(LogMessage.STATE_NORMAL));
        return operations.find(query, LogMessage.class);
    }

    public List<LogMessage> findAllNotSendLimit(int limit) {
        Query query = new Query(Criteria.where("status").is(LogMessage.STATE_NORMAL));
        query.limit(limit);
        return operations.find(query, LogMessage.class);
    }

    public void deleteAllSendSuccessRecord() {
        Query query = new Query(Criteria.where("status").is(LogMessage.STATE_SUCCESSFUL));
        operations.findAndRemove(query, LogMessage.class);
    }

}
