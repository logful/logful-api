package com.getui.logful.server.mongod;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MongoConfigRepository {

    private final MongoOperations operations;

    @Autowired
    public MongoConfigRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public Config save(Config config) {
        if (config == null) {
            return null;
        }

        Query query = new Query().limit(1);
        Config exist = operations.findOne(query, Config.class);
        if (exist != null) {
            config.setId(exist.getId());
        }

        operations.save(config);

        return config;
    }

    public Config read() {
        Query query = new Query().limit(1);

        Config config = operations.findOne(query, Config.class);
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
    private Config defaultConfig() {
        Config config = new Config();
        config.setLevel(Constants.DEFAULT_GRAY_LEVEL);
        return config;
    }
}
