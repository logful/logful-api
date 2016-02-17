package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.CrashFileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CrashFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public CrashFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public CrashFileMeta findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, CrashFileMeta.class);
    }

    public CrashFileMeta save(CrashFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public List<CrashFileMeta> findAll(Query query) {
        return operations.find(query, CrashFileMeta.class);
    }

    public Long countAll(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("clientId").is(id));
        return operations.count(query, CrashFileMeta.class);
    }

}
