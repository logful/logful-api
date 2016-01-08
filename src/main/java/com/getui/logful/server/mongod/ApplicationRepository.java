package com.getui.logful.server.mongod;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ApplicationRepository {

    private final MongoOperations operations;

    @Autowired
    public ApplicationRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public SimpleClientDetails save(SimpleClientDetails clientDetails) {
        operations.save(clientDetails);
        return clientDetails;
    }

    public SimpleClientDetails findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, SimpleClientDetails.class);
    }

    public boolean delete(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        WriteResult writeResult = operations.remove(query, SimpleClientDetails.class);
        return writeResult.getN() > 0;
    }

    public List<SimpleClientDetails> findAll(QueryCondition condition) {
        return findAll(condition.getOrder(), condition.getSort(), condition.getOffset(), condition.getLimit());
    }

    public List<SimpleClientDetails> findAll(Sort.Direction order, String sort, int offset, int limit) {
        Query query = new Query();
        if (sort != null) {
            query.with(new Sort(order, sort));
        }
        query.skip(offset).limit(limit);
        query.fields()
                .include("name")
                .include("appId")
                .include("createDate")
                .include("updateDate")
                .include("clientId")
                .include("clientSecret");
        return operations.find(query, SimpleClientDetails.class);
    }

    public List<SimpleClientDetails> findByClientIds(Object[] clientId) {
        Query query = new Query(Criteria.where("clientId").in(clientId));
        return operations.find(query, SimpleClientDetails.class);
    }
}
