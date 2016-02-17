package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.LogFileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LogFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public LogFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public LogFileMeta findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, LogFileMeta.class);
    }

    public MongoOperations getOperations() {
        return operations;
    }

    public LogFileMeta save(LogFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public List<LogFileMeta> findAll(Query query) {
        return operations.find(query, LogFileMeta.class);
    }

    public Long countAll(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("clientId").is(id));
        return operations.count(query, LogFileMeta.class);
    }

}
