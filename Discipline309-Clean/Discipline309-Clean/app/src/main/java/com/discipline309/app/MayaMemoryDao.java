package com.discipline309.app;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MayaMemoryDao {
    @Query("SELECT * FROM maya_memory ORDER BY id ASC")
    List<MayaMemoryEntity> getAll();

    @Query("SELECT * FROM maya_memory WHERE LOWER(fact) LIKE '%' || LOWER(:query) || '%' ORDER BY id ASC")
    List<MayaMemoryEntity> search(String query);

    @Query("SELECT COUNT(*) FROM maya_memory")
    int count();

    @Insert
    long insert(MayaMemoryEntity item);

    @Delete
    void delete(MayaMemoryEntity item);

    @Query("DELETE FROM maya_memory")
    void clear();
}
