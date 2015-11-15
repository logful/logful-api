package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.WeedLogFileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoWeedLogFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoWeedLogFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public MongoOperations getOperations() {
        return operations;
    }

    public WeedLogFileMeta save(WeedLogFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public List<WeedLogFileMeta> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, WeedLogFileMeta.class);
    }

}
