package com.diretornoouvido.app.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.diretornoouvido.app.model.AudioLocal;
import java.util.List;

@Dao
public interface AudioLocalDao {
    
    @Insert
    long inserir(AudioLocal audio);
    
    @Update
    void atualizar(AudioLocal audio);
    
    @Delete
    void deletar(AudioLocal audio);
    
    @Query("SELECT * FROM audios_locais ORDER BY dataCriacao DESC")
    LiveData<List<AudioLocal>> obterTodos();
    
    @Query("SELECT * FROM audios_locais WHERE id = :id")
    AudioLocal obterPorId(long id);
    
    @Query("SELECT * FROM audios_locais WHERE descricao = :descricao LIMIT 1")
    AudioLocal obterPorDescricao(String descricao);
    
    @Query("DELETE FROM audios_locais WHERE id = :id")
    void deletarPorId(long id);
    
    @Query("SELECT COUNT(*) FROM audios_locais")
    LiveData<Integer> contar();
}
