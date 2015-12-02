package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.UserInfo;
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

    public UserInfo findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, UserInfo.class);
    }

    public UserInfo findByUidAndAppId(int platform, String uid, String appId) {
        Criteria criteria = Criteria.where("platform").is(platform)
                .and("uid").is(uid)
                .and("appId").is(appId);
        Query query = new Query(criteria);
        return operations.findOne(query, UserInfo.class);
    }

    public UserInfo save(UserInfo userInfo) {
        operations.save(userInfo);
        return userInfo;
    }

    public UserInfo findByUid(String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
        Query query = new Query(criteria);
        return operations.findOne(query, UserInfo.class);
    }

    public boolean delete(UserInfo userInfo) {
        Query query = new Query(Criteria.where("_id").is(userInfo.getId()));
        WriteResult writeResult = operations.remove(query, UserInfo.class);
        return writeResult.getN() > 0;
    }

    public List<UserInfo> findAll() {
        return operations.findAll(UserInfo.class);
    }

    public List<UserInfo> findAllByCriteria(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, UserInfo.class);
    }

}
