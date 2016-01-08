package com.getui.logful.server.mongod;

import com.getui.logful.server.entity.ClientUser;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoClientUserRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoClientUserRepository(MongoOperations operations) {
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

    public void save(ClientUser clientUser) {
        Criteria criteria = Criteria.where("uid").is(clientUser.getUid());
        criteria.and("appId").is(clientUser.getAppId());
        Query query = new Query(criteria);

        ClientUser exist = operations.findOne(query, ClientUser.class);
        if (exist != null) {
            // TODO check instance equal
            clientUser.setId(exist.getId());
        }
        operations.save(clientUser);
    }

    public boolean bindDeviceId(Criteria criteria, String deviceId) {
        WriteResult result = operations.updateFirst(new Query(criteria), Update.update("deviceId", deviceId), ClientUser.class);
        return result.getN() == 1;
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

    public List<ClientUser> findAll(QueryCondition condition, Criteria criteria) {
        return findAll(condition.getOrder(), condition.getSort(), condition.getOffset(), condition.getLimit(), criteria);
    }

    public List<ClientUser> findAll(Sort.Direction order, String sort, int offset, int limit, Criteria criteria) {
        Query query = new Query(criteria);
        if (sort != null) {
            query.with(new Sort(order, sort));
        }
        query.skip(offset).limit(limit);
        return operations.find(query, ClientUser.class);
    }

    public List<ClientUser> findAll(Criteria criteria) {
        Query query = new Query(criteria);
        return operations.find(query, ClientUser.class);
    }

    public List<ClientUser> findAll(Query query) {
        return operations.find(query, ClientUser.class);
    }

}
