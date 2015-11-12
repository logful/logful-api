package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.FileMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

// TODO
@Repository
public class MongoFileMetaRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoFileMetaRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public FileMeta save(FileMeta fileMeta) {
        operations.save(fileMeta);
        return fileMeta;
    }

    public List<FileMeta> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, FileMeta.class);
    }

}
