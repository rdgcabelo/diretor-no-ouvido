package com.diretornoouvido.app.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.diretornoouvido.app.db.AppDatabase;
import com.diretornoouvido.app.db.AudioLocalDao;
import com.diretornoouvido.app.model.AudioLocal;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Gerenciador de áudio local com suporte a:
 * - Armazenamento de arquivos de áudio
 * - Reprodução com controle de ↶ 10 segundos
 * - Persistência em banco de dados
 * - Operações CRUD
 */
public class AudioLocalManager {
    
    private static final String TAG = "AudioLocalManager";
    private static final String AUDIOS_DIR = "audio_guides";
    private static final long REWIND_MS = 10000;  // 10 segundos
    
    private Context context;
    private AudioLocalDao dao;
    private MediaPlayer mediaPlayer;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Callbacks de eventos
    private OnPlaybackListener playbackListener;
    
    public AudioLocalManager(Context context) {
        this.context = context;
        this.dao = AppDatabase.obterInstancia(context).audioLocalDao();
    }
    
    /**
     * Salva um arquivo de áudio localmente e registra no banco de dados.
     */
    public void salvarAudio(String titulo, String descricao, byte[] audioData,
                           String fonte, String estilo, 
                           SaveCallback callback) {
        new Thread(() -> {
            try {
                File audioDir = obterDiretorioAudio();
                String nomeArquivo = System.currentTimeMillis() + ".mp3";
                File audioFile = new File(audioDir, nomeArquivo);
                
                // Salva arquivo
                try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                    fos.write(audioData);
                }
                
                // Cria registro no banco
                AudioLocal audio = new AudioLocal(
                        titulo,
                        descricao,
                        audioFile.getAbsolutePath(),
                        0,  // Duração será calculada ao reproduzir
                        fonte,
                        estilo
                );
                
                long id = dao.inserir(audio);
                
                mainHandler.post(() -> 
                        callback.onSucesso(id, audioFile.getAbsolutePath())
                );
            } catch (Exception e) {
                mainHandler.post(() -> 
                        callback.onErro(e.getMessage())
                );
            }
        }).start();
    }
    
    /**
     * Obtém lista de todos os áudios armazenados.
     */
    public LiveData<List<AudioLocal>> obterTodosAudios() {
        return dao.obterTodos();
    }
    
    /**
     * Inicia reprodução de um áudio.
     */
    public void reproduzir(AudioLocal audio) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );
            
            mediaPlayer.setDataSource(audio.caminhoArquivo);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                audio.duracao = mp.getDuration();
                
                if (playbackListener != null) {
                    playbackListener.onPlayback(0, (int)audio.duracao);
                }
                
                // Atualiza UI a cada 100ms
                atualizarProgresso();
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                if (playbackListener != null) {
                    playbackListener.onCompletar();
                }
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (playbackListener != null) {
                    playbackListener.onErro("Erro ao reproduzir: " + what);
                }
                return true;
            });
            
            mediaPlayer.prepareAsync();
            
        } catch (Exception e) {
            if (playbackListener != null) {
                playbackListener.onErro(e.getMessage());
            }
        }
    }
    
    /**
     * Pausa a reprodução.
     */
    public void pausar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }
    
    /**
     * Retoma a reprodução.
     */
    public void retomar() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            atualizarProgresso();
        }
    }
    
    /**
     * Para a reprodução.
     */
    public void parar() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    /**
     * Retrocede 10 segundos na reprodução.
     * Se está pausado, apenas muda a posição.
     * Se está reproduzindo, continua reproduzindo após retrocesso.
     */
    public void retroceder10Segundos() {
        if (mediaPlayer == null) return;
        
        boolean estavaReproduzindo = mediaPlayer.isPlaying();
        int posicaoAtual = mediaPlayer.getCurrentPosition();
        int novaPosicao = (int) Math.max(0, posicaoAtual - REWIND_MS);
        
        mediaPlayer.seekTo(novaPosicao);
        
        if (estavaReproduzindo) {
            mediaPlayer.start();
        }
        
        if (playbackListener != null) {
            playbackListener.onRewind(novaPosicao);
        }
    }
    
    /**
     * Define posição de reprodução (em ms).
     */
    public void irPara(int ms) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(ms);
        }
    }
    
    /**
     * Obtém posição atual de reprodução (ms).
     */
    public int obterPosicaoAtual() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }
    
    /**
     * Obtém duração total (ms).
     */
    public int obterDuracao() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }
    
    /**
     * Verifica se está reproduzindo.
     */
    public boolean estaReproduzindo() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    
    /**
     * Renomeia um áudio.
     */
    public void renomear(AudioLocal audio, String novoTitulo) {
        new Thread(() -> {
            audio.titulo = novoTitulo;
            dao.atualizar(audio);
        }).start();
    }
    
    /**
     * Deleta um áudio (arquivo + registro BD).
     */
    public void deletar(AudioLocal audio) {
        new Thread(() -> {
            try {
                File arquivo = new File(audio.caminhoArquivo);
                if (arquivo.exists()) {
                    arquivo.delete();
                }
                dao.deletarPorId(audio.id);
            } catch (Exception e) {
                // Log error
            }
        }).start();
    }
    
    private void atualizarProgresso() {
        if (mediaPlayer == null || playbackListener == null) return;
        
        mainHandler.postDelayed(() -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                playbackListener.onPlayback(
                        mediaPlayer.getCurrentPosition(),
                        mediaPlayer.getDuration()
                );
                atualizarProgresso();
            }
        }, 100);
    }
    
    private File obterDiretorioAudio() {
        File dir = new File(context.getFilesDir(), AUDIOS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    public void setPlaybackListener(OnPlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    public interface OnPlaybackListener {
        void onPlayback(int posicaoMs, int duracaoMs);
        void onRewind(int novaPosicaoMs);
        void onCompletar();
        void onErro(String mensagem);
    }
    
    public interface SaveCallback {
        void onSucesso(long id, String caminho);
        void onErro(String mensagem);
    }
}
