package com.diretornoouvido.app.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Representa um áudio armazenado localmente no dispositivo.
 * Pode ser gerado via Google Cloud TTS ou outro sistema.
 */
@Entity(tableName = "audios_locais")
public class AudioLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String titulo;           // Nome do áudio
    public String descricao;        // Texto original
    public String caminhoArquivo;   // Caminho do arquivo MP3/WAV
    public long duracao;            // Duração em ms
    public long dataCriacao;        // Timestamp de criação
    public String fonte;            // "ANDROID_TTS" ou "GOOGLE_CLOUD_TTS"
    public String estilo;           // Ex: "Natural", "Calmo", etc
    public boolean disponivel;      // true se arquivo existe localmente
    
    public AudioLocal() {}
    
    public AudioLocal(String titulo, String descricao, String caminhoArquivo,
                      long duracao, String fonte, String estilo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.caminhoArquivo = caminhoArquivo;
        this.duracao = duracao;
        this.dataCriacao = System.currentTimeMillis();
        this.fonte = fonte;
        this.estilo = estilo;
        this.disponivel = true;
    }
}
