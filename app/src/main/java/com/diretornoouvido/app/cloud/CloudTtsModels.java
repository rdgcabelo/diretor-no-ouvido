package com.diretornoouvido.app.cloud;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Modelos de requisição e resposta para Google Cloud Text-to-Speech API.
 * A chave de API deve ser fornecida via backend/proxy seguro, NUNCA hardcoded.
 */
public class CloudTtsModels {
    
    /**
     * Requisição para gerar voz
     */
    public static class SynthesizeSpeechRequest {
        @SerializedName("input")
        public Input input;
        
        @SerializedName("voice")
        public Voice voice;
        
        @SerializedName("audioConfig")
        public AudioConfig audioConfig;
        
        public SynthesizeSpeechRequest(String texto, String languageCode,
                                       String voiceName, String audioFormat) {
            this.input = new Input(texto);
            this.voice = new Voice(languageCode, voiceName);
            this.audioConfig = new AudioConfig(audioFormat);
        }
    }
    
    public static class Input {
        @SerializedName("text")
        public String text;
        
        public Input(String text) {
            this.text = text;
        }
    }
    
    public static class Voice {
        @SerializedName("languageCode")
        public String languageCode;
        
        @SerializedName("name")
        public String name;
        
        public Voice(String languageCode, String name) {
            this.languageCode = languageCode;
            this.name = name;
        }
    }
    
    public static class AudioConfig {
        @SerializedName("audioEncoding")
        public String audioEncoding;
        
        @SerializedName("sampleRateHertz")
        public int sampleRateHertz;
        
        @SerializedName("speakingRate")
        public double speakingRate;
        
        public AudioConfig(String audioEncoding) {
            this.audioEncoding = audioEncoding;
            this.sampleRateHertz = 24000;
            this.speakingRate = 1.0;
        }
    }
    
    /**
     * Resposta da API Google Cloud TTS
     */
    public static class SynthesizeSpeechResponse {
        @SerializedName("audioContent")
        public String audioContent;  // Base64 encoded
        
        @SerializedName("audioConfig")
        public AudioConfig audioConfig;
    }
}
