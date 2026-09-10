package com.diretornoouvido.app.cloud;

import android.content.Context;
import android.util.Base64;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

/**
 * Gerenciador seguro para síntese de voz usando Backend Cloud Run TTS.
 * 
 * URL: https://diretor-no-ouvido-tts-464833730167.southamerica-east1.run.app
 * 
 * O token é obtido de BuildConfig (configurado via local.properties)
 * e NUNCA é armazenado no código-fonte.
 */
public class CloudRunTtsManager {
    
    private static final String TAG = "CloudRunTtsManager";
    
    private CloudRunTtsApi apiService;
    private String appToken;
    private String backendUrl;
    private Context context;
    
    public CloudRunTtsManager(Context context, String appToken, String backendUrl) {
        this.context = context;
        this.appToken = appToken;
        this.backendUrl = backendUrl;
        inicializarRetrofit();
    }
    
    private void inicializarRetrofit() {
        // Configuração segura com timeouts apropriados
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(backendUrl.endsWith("/") ? backendUrl : backendUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        
        apiService = retrofit.create(CloudRunTtsApi.class);
    }
    
    /**
     * Sintetiza fala usando o backend Cloud Run.
     * 
     * @param texto Texto a sintetizar
     * @param callback Callback com sucesso ou erro
     */
    public void sintetizarFala(String texto, SynthesizeCallback callback) {
        if (appToken == null || appToken.isEmpty()) {
            callback.onErro("Token de API não configurado. Verifique local.properties");
            return;
        }
        
        if (texto == null || texto.trim().isEmpty()) {
            callback.onErro("Texto vazio");
            return;
        }
        
        CloudRunTtsModels.SynthesizeRequest request = 
                new CloudRunTtsModels.SynthesizeRequest(texto);
        
        // Executa em thread para não bloquear UI
        new Thread(() -> {
            try {
                retrofit2.Response<CloudRunTtsModels.SynthesizeResponse> response = 
                        apiService.synthesize(request, appToken).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    CloudRunTtsModels.SynthesizeResponse resposta = response.body();
                    
                    if (resposta.ok && resposta.audioBase64 != null && 
                            !resposta.audioBase64.isEmpty()) {
                        // Decodifica o Base64 para bytes
                        try {
                            byte[] audioData = Base64.decode(resposta.audioBase64, 
                                    Base64.DEFAULT);
                            callback.onSucesso(audioData, resposta.voiceName, 
                                    resposta.mimeType);
                        } catch (IllegalArgumentException e) {
                            callback.onErro("Erro ao decodificar áudio: " + e.getMessage());
                        }
                    } else {
                        // Resposta com erro
                        String erroMsg = resposta.error != null ? resposta.error : 
                                "Erro desconhecido da API";
                        callback.onErro(erroMsg);
                    }
                } else {
                    // Erro HTTP
                    String erroMsg = "Erro " + response.code() + ": " + 
                            response.message();
                    
                    if (response.code() == 401) {
                        erroMsg = "Token inválido. Verifique local.properties";
                    } else if (response.code() == 429) {
                        erroMsg = "Limite de requisições atingido. Tente novamente mais tarde";
                    } else if (response.code() == 500) {
                        erroMsg = "Erro no servidor. Tente novamente";
                    }
                    
                    callback.onErro(erroMsg);
                }
            } catch (java.net.ConnectException e) {
                callback.onErro("Erro de conexão. Verifique sua internet e a URL do backend");
            } catch (java.net.SocketTimeoutException e) {
                callback.onErro("Timeout - Backend demorou muito. Tente novamente");
            } catch (Exception e) {
                callback.onErro("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Callback para resultado da síntese de fala
     */
    public interface SynthesizeCallback {
        /**
         * Chamado quando síntese é bem-sucedida
         * @param audioData Bytes do áudio (MP3)
         * @param voiceName Nome da voz usada
         * @param mimeType Tipo MIME (ex: "audio/mpeg")
         */
        void onSucesso(byte[] audioData, String voiceName, String mimeType);
        
        /**
         * Chamado quando ocorre erro
         * @param mensagem Descrição do erro
         */
        void onErro(String mensagem);
    }
}
