package com.diretornoouvido.app.cloud;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Interface Retrofit para Google Cloud Text-to-Speech.
 * 
 * Por segurança, a chave de API é adicionada via interceptor em tempo de execução
 * ou através de um backend seguro (recomendado).
 * 
 * Dois cenários suportados:
 * 1. Backend proxy próprio: https://seu-backend.com/api/synthesize
 * 2. Google Cloud direto: https://texttospeech.googleapis.com/v1/text:synthesize
 */
public interface GoogleCloudTtsApi {
    
    /**
     * Sintetiza fala a partir do texto.
     * 
     * @param request Configuração de síntese (texto, voz, idioma, etc)
     * @return Response com audioContent em base64
     */
    @POST("v1/text:synthesize")
    Call<CloudTtsModels.SynthesizeSpeechResponse> synthesizeSpeech(
            @Body CloudTtsModels.SynthesizeSpeechRequest request
    );
}
