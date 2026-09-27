package com.discipline309.app;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MayaMemoryEntity.class}, version = 1, exportSchema = false)
public abstract class MayaDatabase extends RoomDatabase {
    public abstract MayaMemoryDao mayaMemoryDao();

    private static volatile MayaDatabase INSTANCE;

    public static MayaDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (MayaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MayaDatabase.class,
                            "maya_memory.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
