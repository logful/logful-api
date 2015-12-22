package com.getui.logful.server.mongod;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.GlobalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class GlobalConfigRepository {

    private final MongoOperations operations;

    @Autowired
    public GlobalConfigRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public boolean save(GlobalConfig config) {
        if (config == null) {
            return false;
        }

        Query query = new Query().limit(1);
        GlobalConfig exist = operations.findOne(query, GlobalConfig.class);
        if (exist != null) {
            config.setId(exist.getId());
        }

        operations.save(config);
        return true;
    }

    public GlobalConfig read() {
        Query query = new Query().limit(1);

        GlobalConfig config = operations.findOne(query, GlobalConfig.class);
        if (config == null) {
            return defaultConfig();
        }

        return config;
    }

    /**
     * Get default config.
     *
     * @return Default config
     */
    private GlobalConfig defaultConfig() {
        GlobalConfig config = new GlobalConfig();
        config.setLevel(Constants.DEFAULT_GRAY_LEVEL);
        return config;
    }
}
