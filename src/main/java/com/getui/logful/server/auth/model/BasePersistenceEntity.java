package com.getui.logful.server.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class BasePersistenceEntity implements Serializable{

    private int version;

    @Id
    private String id;

    private Date timeCreated;

    public BasePersistenceEntity() {
        this(UUID.randomUUID());
    }

    public BasePersistenceEntity(UUID guid) {
        Assert.notNull(guid, "UUID is required");
        id = guid.toString();
        this.timeCreated = new Date();
    }

    public BasePersistenceEntity(String guid) {
        Assert.notNull(guid, "UUID is required");
        id = guid;
        this.timeCreated = new Date();
    }

    public String getId() {
        return id;
    }

    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasePersistenceEntity that = (BasePersistenceEntity) o;
        return id.equals(that.id);
    }

    public int getVersion() {
        return version;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }
}
