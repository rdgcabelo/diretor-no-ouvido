package com.diretornoouvido.app.tts;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.io.File;

/**
 * Utilidade para gravar síntese de fala (Android TTS) em arquivo local.
 * Permite reutilizar a mesma locução sem regenerá-la.
 */
public class TtsRecorder {
    
    private static final String TAG = "TtsRecorder";
    private Context context;
    private TextToSpeech tts;
    private MediaRecorder recorder;
    private String arquivoTemp;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public TtsRecorder(Context context, TextToSpeech tts) {
        this.context = context;
        this.tts = tts;
    }
    
    /**
     * Grava a síntese de fala em arquivo.
     * 
     * @param texto Texto a ser falado
     * @param callback Callback com caminho do arquivo ou erro
     */
    public void gravarTextoEmArquivo(String texto, RecordCallback callback) {
        new Thread(() -> {
            try {
                // Cria arquivo temporário
                File audiosDir = new File(context.getFilesDir(), "audio_guides");
                if (!audiosDir.exists()) {
                    audiosDir.mkdirs();
                }
                
                arquivoTemp = new File(audiosDir, 
                        System.currentTimeMillis() + "_temp.mp3").getAbsolutePath();
                
                // Configura recorder
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile(arquivoTemp);
                recorder.prepare();
                recorder.start();
                
                // Usa TTS para falar enquanto está gravando
                tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "GRAVACAO");
                
                // Aguarda fim da fala
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);  // Tempo máximo para fala
                        
                        // Para recorder
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                        
                        mainHandler.post(() -> 
                                callback.onSucesso(arquivoTemp)
                        );
                    } catch (Exception e) {
                        mainHandler.post(() -> 
                                callback.onErro(e.getMessage())
                        );
                    }
                }).start();
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onErro(e.getMessage()));
            }
        }).start();
    }
    
    public interface RecordCallback {
        void onSucesso(String caminhoArquivo);
        void onErro(String mensagem);
    }
}
