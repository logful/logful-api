package com.getui.logful.server.auth.repository;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface SimpleClientDetailsRepository extends MongoRepository<SimpleClientDetails, Serializable> {

    SimpleClientDetails findByClientId(String clientId);
}
