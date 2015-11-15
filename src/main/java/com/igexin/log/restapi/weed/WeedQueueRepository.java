package com.igexin.log.restapi.weed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WeedQueueRepository {

    private final MongoOperations operations;

    @Autowired
    public WeedQueueRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public WeedFSMeta save(WeedFSMeta meta) {
        operations.save(meta);
        return meta;
    }

    public List<WeedFSMeta> findAllNotWriteLimit(int limit) {
        Query query = new Query(Criteria.where("status").is(WeedFSMeta.STATE_NORMAL));
        query.limit(limit);
        return operations.find(query, WeedFSMeta.class);
    }
}
