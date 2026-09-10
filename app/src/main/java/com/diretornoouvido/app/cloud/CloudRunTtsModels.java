package com.diretornoouvido.app.cloud;

import com.google.gson.annotations.SerializedName;

/**
 * Modelos de requisição e resposta para o Backend Cloud Run TTS.
 */
public class CloudRunTtsModels {
    
    /**
     * Requisição para sintetizar fala
     */
    public static class SynthesizeRequest {
        @SerializedName("text")
        public String text;
        
        public SynthesizeRequest(String text) {
            this.text = text;
        }
    }
    
    /**
     * Resposta do backend com áudio em Base64
     */
    public static class SynthesizeResponse {
        @SerializedName("ok")
        public boolean ok;
        
        @SerializedName("mimeType")
        public String mimeType;  // "audio/mpeg" ou similar
        
        @SerializedName("voiceName")
        public String voiceName;  // Nome da voz usada
        
        @SerializedName("audioBase64")
        public String audioBase64;  // Áudio em Base64
        
        @SerializedName("error")
        public String error;  // Se ok=false, contém mensagem de erro
    }
}
