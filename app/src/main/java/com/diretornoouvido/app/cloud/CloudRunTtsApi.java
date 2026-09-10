package com.diretornoouvido.app.cloud;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Interface Retrofit para o Backend Cloud Run TTS.
 * 
 * Backend URL: https://diretor-no-ouvido-tts-464833730167.southamerica-east1.run.app
 * POST /api/synthesize
 * 
 * Requer header: x-app-token
 */
public interface CloudRunTtsApi {
    
    /**
     * Sintetiza fala usando o backend Cloud Run.
     * 
     * @param request Contém o texto a sintetizar
     * @param appToken Token de autenticação (x-app-token header)
     * @return Response com audioBase64 e metadados
     */
    @POST("api/synthesize")
    Call<CloudRunTtsModels.SynthesizeResponse> synthesize(
            @Body CloudRunTtsModels.SynthesizeRequest request,
            @Header("x-app-token") String appToken
    );
}
