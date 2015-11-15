package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.WeedAttachFileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoWeedAttachFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoWeedAttachFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public WeedAttachFileMeta save(WeedAttachFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public WeedAttachFileMeta findOneByCriteria(Criteria criteria) {
        Query query = new Query(criteria).limit(1);
        return operations.findOne(query, WeedAttachFileMeta.class);
    }

    public List<WeedAttachFileMeta> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, WeedAttachFileMeta.class);
    }

}
