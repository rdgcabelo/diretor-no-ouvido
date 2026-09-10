package com.diretornoouvido.app.cloud;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.File;

/**
 * Gerenciador seguro de Google Cloud TTS.
 * 
 * Segurança:
 * - Chave de API NUNCA é hardcoded ou armazenada no APK
 * - Deve ser fornecida via:
 *   a) Backend seguro (proxy) - RECOMENDADO
 *   b) Arquivo de configuração não versionado
 *   c) Variáveis de ambiente em tempo de execução
 * 
 * Fallback: Android TTS nativo
 */
public class CloudTtsManager {
    
    private static final String TAG = "CloudTtsManager";
    private static final String BASE_URL_GOOGLE = 
            "https://texttospeech.googleapis.com/";
    private static final String BASE_URL_PROXY = 
            "https://seu-backend.com/";  // Será preenchido em tempo de execução
    
    private GoogleCloudTtsApi apiService;
    private String apiKey;
    private Context context;
    private String baseUrlAtual;
    
    public CloudTtsManager(Context context) {
        this.context = context;
        this.baseUrlAtual = BASE_URL_GOOGLE;
    }
    
    /**
     * Configura a chave de API para acesso ao Google Cloud TTS.
     * IMPORTANTE: Essa chave deve ser obtida de forma segura, NUNCA hardcoded.
     * 
     * Opções recomendadas:
     * 1. Backend proxy que você controla
     * 2. Arquivo de configuração seguro não versionado
     * 3. Variáveis de ambiente em CI/CD
     * 
     * @param apiKey Chave de API Google Cloud (não deve estar no código-fonte)
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        // Recria o cliente Retrofit com a chave configurada
        inicializarRetrofit();
    }
    
    /**
     * Configura um backend proxy seguro em vez de chamar Google Cloud direto.
     * RECOMENDADO: Use um proxy para adicionar uma camada de segurança.
     * 
     * @param baseUrl URL do seu backend (ex: https://seu-backend.com/api/)
     * @param apiKey Token de autenticação para o backend (se necessário)
     */
    public void configureBackendProxy(String baseUrl, String apiKey) {
        this.baseUrlAtual = baseUrl;
        this.apiKey = apiKey;
        inicializarRetrofit();
    }
    
    private void inicializarRetrofit() {
        if (apiService != null) {
            return;  // Já inicializado
        }
        
        // Cria o cliente Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrlAtual)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new okhttp3.OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            // Adiciona headers de segurança
                            okhttp3.Request.Builder requestBuilder = chain.request()
                                    .newBuilder()
                                    .addHeader("User-Agent", 
                                            "DiretorNoOuvido/1.0")
                                    .addHeader("X-Requested-With", 
                                            "com.diretornoouvido.app");
                            
                            // Se há chave, adiciona ao header (não à query)
                            if (apiKey != null && !apiKey.isEmpty()) {
                                requestBuilder.addHeader("Authorization", 
                                        "Bearer " + apiKey);
                            }
                            
                            return chain.proceed(requestBuilder.build());
                        })
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build())
                .build();
        
        apiService = retrofit.create(GoogleCloudTtsApi.class);
    }
    
    /**
     * Sintetiza fala usando Google Cloud TTS.
     * Retorna o áudio em base64 que deve ser decodificado e salvo localmente.
     * 
     * @param texto Texto a ser sintetizado
     * @param voiceName Nome da voz (ex: "pt-BR-Neural2-C" ou "pt-BR-Polyglot-1")
     * @param callback Callback com resultado ou erro
     */
    public void sintetizarFala(String texto, String voiceName, 
                               SynthesizeCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onErro("Chave de API não configurada. " +
                    "Usar Android TTS como fallback.");
            return;
        }
        
        CloudTtsModels.SynthesizeSpeechRequest request =
                new CloudTtsModels.SynthesizeSpeechRequest(
                        texto,
                        "pt-BR",
                        voiceName,
                        "MP3"
                );
        
        new Thread(() -> {
            try {
                retrofit2.Response<CloudTtsModels.SynthesizeSpeechResponse> 
                        response = apiService.synthesizeSpeech(request)
                        .execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String audioBase64 = response.body().audioContent;
                    byte[] audioData = Base64.decode(audioBase64, Base64.DEFAULT);
                    callback.onSucesso(audioData);
                } else {
                    callback.onErro("Erro " + response.code() + 
                            ": " + response.message());
                }
            } catch (Exception e) {
                callback.onErro("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }
    
    public interface SynthesizeCallback {
        void onSucesso(byte[] audioData);
        void onErro(String mensagem);
    }
}
