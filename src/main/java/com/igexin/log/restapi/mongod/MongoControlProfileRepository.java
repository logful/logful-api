package com.igexin.log.restapi.mongod;

import com.igexin.log.restapi.entity.ControlProfile;
import com.igexin.log.restapi.entity.UserInfo;
import com.igexin.log.restapi.util.StringUtil;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoControlProfileRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoControlProfileRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public boolean save(ControlProfile profile) {
        if (!StringUtil.isEmpty(profile.getId())) {
            // Update record.
            operations.save(profile);
            return true;
        } else {
            Criteria criteria = Criteria.where("platform").is(profile.getPlatform());
            criteria.andOperator(
                    Criteria.where("uid").is(profile.getUid()),
                    Criteria.where("alias").is(profile.getAlias()),
                    Criteria.where("appId").is(profile.getAppId())
            );
            Query query = new Query(criteria);
            long count = operations.count(query, ControlProfile.class);
            if (count > 0) {
                return false;
            } else {
                operations.save(profile);
                return true;
            }
        }
    }

    public boolean delete(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        WriteResult writeResult = operations.remove(query, ControlProfile.class);
        return writeResult.getN() > 0;
    }

    public ControlProfile find(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return operations.findOne(query, ControlProfile.class);
    }

    public List<ControlProfile> findAllByUser(UserInfo info) {
        int platform = info.getPlatform();
        String uid = info.getUid();
        String alias = info.getAlias();
        String appId = info.getAppId();

        Criteria criteria = Criteria.where("platform").is(platform)
                .and("uid").in(uid, "")
                .and("alias").in(alias, "")
                .and("appId").in(appId, "");

        Query query = new Query(criteria);
        return operations.find(query, ControlProfile.class);
    }

    public List<ControlProfile> findAll() {
        return operations.findAll(ControlProfile.class);
    }

}
