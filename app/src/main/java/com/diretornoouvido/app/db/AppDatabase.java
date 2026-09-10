package com.diretornoouvido.app.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.diretornoouvido.app.model.AudioLocal;

@Database(entities = {AudioLocal.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract AudioLocalDao audioLocalDao();
    
    private static volatile AppDatabase instancia;
    
    public static AppDatabase obterInstancia(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "diretor_no_ouvido_db"
                    ).build();
                }
            }
        }
        return instancia;
    }
}
