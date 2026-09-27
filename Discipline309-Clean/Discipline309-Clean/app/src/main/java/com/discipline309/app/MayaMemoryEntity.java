package com.discipline309.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "maya_memory")
public class MayaMemoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String fact;
    public long createdAt;

    public MayaMemoryEntity(String fact, long createdAt) {
        this.fact = fact;
        this.createdAt = createdAt;
    }
}
