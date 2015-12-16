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

    public MongoOperations getOperations() {
        return operations;
    }

    public LogFileMeta save(LogFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public List<LogFileMeta> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, LogFileMeta.class);
    }

}
