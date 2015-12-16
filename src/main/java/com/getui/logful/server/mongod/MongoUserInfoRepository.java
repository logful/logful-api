package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.ClientUser;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoUserInfoRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoUserInfoRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public ClientUser findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, ClientUser.class);
    }

    public ClientUser findByUidAndAppId(int platform, String uid, String appId) {
        Criteria criteria = Criteria.where("platform").is(platform)
                .and("uid").is(uid)
                .and("appId").is(appId);
        Query query = new Query(criteria);
        return operations.findOne(query, ClientUser.class);
    }

    public boolean save(ClientUser clientUser) {
        clientUser.generateHashString();
        try {
            operations.save(clientUser);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ClientUser findByUid(String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
        Query query = new Query(criteria);
        return operations.findOne(query, ClientUser.class);
    }

    public boolean delete(ClientUser clientUser) {
        Query query = new Query(Criteria.where("_id").is(clientUser.getId()));
        WriteResult writeResult = operations.remove(query, ClientUser.class);
        return writeResult.getN() > 0;
    }

    public List<ClientUser> findAll() {
        return operations.findAll(ClientUser.class);
    }

    public List<ClientUser> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, ClientUser.class);
    }

}
