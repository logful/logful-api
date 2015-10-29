package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.DecryptError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoDecryptErrorRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoDecryptErrorRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public DecryptError findByUid(String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
        Query query = new Query(criteria);
        return operations.findOne(query, DecryptError.class);
    }

    public DecryptError save(DecryptError decryptError) {
        operations.save(decryptError);
        return decryptError;
    }

    public List<DecryptError> findAll() {
        return operations.findAll(DecryptError.class);
    }
}
