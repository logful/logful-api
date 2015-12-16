package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.AttachFileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AttachFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public AttachFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public AttachFileMeta save(AttachFileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public AttachFileMeta findOneByCriteria(Criteria criteria) {
        Query query = new Query(criteria).limit(1);
        return operations.findOne(query, AttachFileMeta.class);
    }

    public List<AttachFileMeta> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, AttachFileMeta.class);
    }

}
