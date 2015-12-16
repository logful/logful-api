package com.getui.logful.server.mongod;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Repository;

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
}
