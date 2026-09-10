package com.diretornoouvido.app;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import android.graphics.Color;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.*;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.diretornoouvido.app.media.AudioLocalManager;
import com.diretornoouvido.app.model.AudioLocal;
import com.diretornoouvido.app.cloud.CloudRunTtsManager;
import com.diretornoouvido.app.BuildConfig;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private PreviewView previewView;
    private EditText texto;
    private Spinner estilo;
    private TextView status;
    private Button gravar;
    private Button btnGerarVoz;
    private ProgressBar carregandoIndicador;
    private TextToSpeech tts;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private AudioLocalManager audioManager;
    private CloudRunTtsManager cloudRunTtsManager;
    private AudioLocal audioSelecionado;
    private LinearLayout controleGravacaoContainer;
    private Button btnPlayPauseGuia;
    private Button btnRewindGuia;
    private TextView tempoGuia;
    private boolean emGravacao = false;

    private final ActivityResultLauncher<String[]> permissoes =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> iniciarCamera()
            );
    
    private final ActivityResultLauncher<Intent> audioSeletor =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                long audioId = data.getLongExtra("audio_id", -1);
                                String audioPath = data.getStringExtra("audio_path");
                                if (audioId != -1) {
                                    audioSelecionado = new AudioLocal();
                                    audioSelecionado.id = audioId;
                                    audioSelecionado.caminhoArquivo = audioPath;
                                    status.setText("Áudio selecionado como voz-guia");
                                }
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = new AudioLocalManager(this);
        tts = new TextToSpeech(this, this);
        
        // Inicializa gerenciador de TTS do Cloud Run com token seguro
        cloudRunTtsManager = new CloudRunTtsManager(
                this, 
                BuildConfig.APP_TOKEN,
                BuildConfig.BACKEND_URL
        );

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(18, 18, 18, 18);

        // ========== SEÇÃO SUPERIOR: TÍTULO E STATUS ==========
        TextView titulo = new TextView(this);
        titulo.setText("Diretor no Ouvido");
        titulo.setTextSize(24);
        titulo.setPadding(0, 0, 0, 12);
        raiz.addView(titulo);

        status = new TextView(this);
        status.setText("Preparando câmera...");
        status.setPadding(0, 0, 0, 12);
        raiz.addView(status);

        // ========== PREVIEW CÂMERA ==========
        previewView = new PreviewView(this);
        LinearLayout.LayoutParams cameraParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        raiz.addView(previewView, cameraParams);

        // ========== SEÇÃO DE GUIA DE VOZ ==========
        texto = new EditText(this);
        texto.setHint("Cole aqui o texto que você quer ouvir no fone...");
        texto.setMinLines(4);
        texto.setMaxLines(7);
        raiz.addView(texto);

        estilo = new Spinner(this);
        String[] estilos = {
                "Natural",
                "Calmo",
                "Contemplativo",
                "Emocional",
                "Dramático"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                estilos
        );
        estilo.setAdapter(adapter);
        raiz.addView(estilo);

        // ========== BOTÕES DE REPRODUÇÃO ==========
        LinearLayout botoesReproducao = new LinearLayout(this);
        botoesReproducao.setOrientation(LinearLayout.HORIZONTAL);
        botoesReproducao.setWeightSum(2);

        Button ouvir = new Button(this);
        ouvir.setText("OUVIR GUIA");
        ouvir.setOnClickListener(v -> falarTexto());
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        param1.setMargins(2, 2, 2, 2);
        botoesReproducao.addView(ouvir, param1);

        btnGerarVoz = new Button(this);
        btnGerarVoz.setText("✨ GERAR VOZ");
        btnGerarVoz.setOnClickListener(v -> gerarVozCloud());
        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        param2.setMargins(2, 2, 2, 2);
        botoesReproducao.addView(btnGerarVoz, param2);

        raiz.addView(botoesReproducao);
        
        // Indicador de carregamento
        carregandoIndicador = new ProgressBar(this);
        carregandoIndicador.setIndeterminate(true);
        LinearLayout.LayoutParams indicadorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        indicadorParams.setMargins(0, 8, 0, 8);
        carregandoIndicador.setVisibility(View.GONE);
        raiz.addView(carregandoIndicador, indicadorParams);

        // Botão Parar Guia
        Button pararGuia = new Button(this);
        pararGuia.setText("PARAR GUIA");
        pararGuia.setOnClickListener(v -> {
            if (tts != null) tts.stop();
            audioManager.parar();
        });
        raiz.addView(pararGuia);

        // ========== BOTÕES DE GRAVAÇÃO ==========
        LinearLayout botoesGravacao = new LinearLayout(this);
        botoesGravacao.setOrientation(LinearLayout.HORIZONTAL);
        botoesGravacao.setWeightSum(2);

        gravar = new Button(this);
        gravar.setText("GRAVAR VÍDEO + GUIA NO FONE");
        gravar.setOnClickListener(v -> alternarGravacao());
        LinearLayout.LayoutParams param3 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        param3.setMargins(2, 2, 2, 2);
        botoesGravacao.addView(gravar, param3);

        Button meusAudios = new Button(this);
        meusAudios.setText("🎵 MEUS ÁUDIOS");
        meusAudios.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MeusAudiosActivity.class);
            audioSeletor.launch(intent);
        });
        LinearLayout.LayoutParams param4 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        param4.setMargins(2, 2, 2, 2);
        botoesGravacao.addView(meusAudios, param4);

        raiz.addView(botoesGravacao);

        // ========== CONTROLES DE GRAVAÇÃO (ESCONDIDOS POR PADRÃO) ==========
        controleGravacaoContainer = new LinearLayout(this);
        controleGravacaoContainer.setOrientation(LinearLayout.VERTICAL);
        controleGravacaoContainer.setBackgroundColor(Color.parseColor("#fff8f8"));
        controleGravacaoContainer.setPadding(8, 8, 8, 8);
        controleGravacaoContainer.setVisibility(View.GONE);

        TextView titleControles = new TextView(this);
        titleControles.setText("🎙️ Controles da Guia");
        titleControles.setTextSize(12);
        titleControles.setTextColor(Color.BLACK);
        controleGravacaoContainer.addView(titleControles);

        // Barra de progresso + tempo
        LinearLayout tempoContainer = new LinearLayout(this);
        tempoContainer.setOrientation(LinearLayout.HORIZONTAL);
        tempoContainer.setWeightSum(1);

        ProgressBar progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f);
        tempoContainer.addView(progressBar, progressParams);

        tempoGuia = new TextView(this);
        tempoGuia.setText("00:00");
        tempoGuia.setTextSize(11);
        tempoGuia.setPadding(8, 0, 0, 0);
        LinearLayout.LayoutParams tempoParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f);
        tempoContainer.addView(tempoGuia, tempoParams);

        controleGravacaoContainer.addView(tempoContainer);

        // Botões de controle
        LinearLayout botoesControle = new LinearLayout(this);
        botoesControle.setOrientation(LinearLayout.HORIZONTAL);
        botoesControle.setWeightSum(3);
        botoesControle.setPadding(0, 6, 0, 0);

        btnPlayPauseGuia = new Button(this);
        btnPlayPauseGuia.setText("▶");
        btnPlayPauseGuia.setOnClickListener(v -> {
            if (audioSelecionado == null) {
                Toast.makeText(this, "Selecione um áudio em 'Meus Áudios'",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (audioManager.estaReproduzindo()) {
                audioManager.pausar();
                btnPlayPauseGuia.setText("▶");
            } else {
                audioManager.retomar();
                btnPlayPauseGuia.setText("⏸");
            }
        });
        LinearLayout.LayoutParams btnParam1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnParam1.setMargins(2, 0, 2, 0);
        botoesControle.addView(btnPlayPauseGuia, btnParam1);

        btnRewindGuia = new Button(this);
        btnRewindGuia.setText("↶ 10s");
        btnRewindGuia.setTextSize(12);
        btnRewindGuia.setOnClickListener(v -> {
            if (audioSelecionado != null) {
                audioManager.retroceder10Segundos();
            }
        });
        LinearLayout.LayoutParams btnParam2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnParam2.setMargins(2, 0, 2, 0);
        botoesControle.addView(btnRewindGuia, btnParam2);

        Button parar = new Button(this);
        parar.setText("✕");
        parar.setOnClickListener(v -> {
            audioManager.parar();
            btnPlayPauseGuia.setText("▶");
            controleGravacaoContainer.setVisibility(View.GONE);
        });
        LinearLayout.LayoutParams btnParam3 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnParam3.setMargins(2, 0, 2, 0);
        botoesControle.addView(parar, btnParam3);

        controleGravacaoContainer.addView(botoesControle);
        raiz.addView(controleGravacaoContainer);

        setContentView(raiz);

        if (temPermissoes()) {
            iniciarCamera();
        } else {
            permissoes.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.INTERNET
            });
        }
    }

    private boolean temPermissoes() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                        this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                        this, Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void iniciarCamera() {
        if (!temPermissoes()) {
            status.setText("É necessário permitir câmera, microfone e internet.");
            return;
        }

        ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(this);

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider provider = futuro.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(
                                QualitySelector.from(
                                        Quality.HD,
                                        FallbackStrategy.lowerQualityOrHigherThan(
                                                Quality.SD)))
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);

                provider.unbindAll();

                try {
                    provider.bindToLifecycle(
                            this,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            videoCapture
                    );
                } catch (Exception e) {
                    provider.bindToLifecycle(
                            this,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            videoCapture
                    );
                }

                mostrarDispositivosAudio();

            } catch (Exception e) {
                status.setText("Erro ao iniciar câmera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void mostrarDispositivosAudio() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);

        boolean temFone = false;
        boolean temMicExterno = false;

        for (AudioDeviceInfo d :
                am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int t = d.getType();
            if (t == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    t == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    t == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    t == AudioDeviceInfo.TYPE_USB_HEADSET) {
                temFone = true;
            }
        }

        for (AudioDeviceInfo d :
                am.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            int t = d.getType();
            if (t == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    t == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    t == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    t == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    t == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                temMicExterno = true;
            }
        }

        status.setText(
                "Câmera pronta | Fone: " +
                        (temFone ? "detectado" : "não detectado") +
                        " | Mic externo: " +
                        (temMicExterno ? "detectado" : "não detectado")
        );
    }

    @Override
    public void onInit(int resultado) {
        if (resultado == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("pt", "BR"));
        }
    }

    private void configurarVoz() {
        int pos = estilo.getSelectedItemPosition();

        switch (pos) {
            case 1:
                tts.setSpeechRate(0.88f);
                tts.setPitch(1.0f);
                break;
            case 2:
                tts.setSpeechRate(0.78f);
                tts.setPitch(0.94f);
                break;
            case 3:
                tts.setSpeechRate(0.90f);
                tts.setPitch(1.03f);
                break;
            case 4:
                tts.setSpeechRate(0.74f);
                tts.setPitch(0.90f);
                break;
            default:
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
        }
    }

    private void falarTexto() {
        String s = texto.getText().toString().trim();

        if (s.isEmpty()) {
            Toast.makeText(
                    this,
                    "Cole um texto primeiro.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        configurarVoz();
        tts.speak(
                s,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "GUIA"
        );
    }

    private void gerarVozCloud() {
        String s = texto.getText().toString().trim();

        if (s.isEmpty()) {
            Toast.makeText(
                    this,
                    "Cole um texto primeiro.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        
        // Verifica se token está configurado
        if (BuildConfig.APP_TOKEN == null || BuildConfig.APP_TOKEN.isEmpty() || 
                BuildConfig.APP_TOKEN.equals("seu_token_aqui")) {
            Toast.makeText(
                    this,
                    "❌ Token não configurado. Verifique local.properties",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        
        // Mostra indicador de carregamento
        btnGerarVoz.setEnabled(false);
        carregandoIndicador.setVisibility(View.VISIBLE);
        status.setText("⏳ Gerando áudio no backend...");
        
        // Chama backend para sintetizar
        cloudRunTtsManager.sintetizarFala(s, new CloudRunTtsManager.SynthesizeCallback() {
            @Override
            public void onSucesso(byte[] audioData, String voiceName, String mimeType) {
                // Obtém nome do estilo selecionado
                String estiloSelecionado = estilo.getSelectedItem().toString();
                
                // Salva áudio localmente no banco de dados
                audioManager.salvarAudio(
                        "Gerado: " + estiloSelecionado + " - " + 
                                new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        .format(new Date()),
                        s,
                        audioData,
                        "CLOUD_RUN_TTS",
                        estiloSelecionado,
                        new AudioLocalManager.SaveCallback() {
                            @Override
                            public void onSucesso(long id, String caminho) {
                                // Atualiza seleção de áudio para reproduzir o novo áudio
                                audioSelecionado = new AudioLocal();
                                audioSelecionado.id = id;
                                audioSelecionado.caminhoArquivo = caminho;
                                
                                handler.post(() -> {
                                    btnGerarVoz.setEnabled(true);
                                    carregandoIndicador.setVisibility(View.GONE);
                                    status.setText("✅ Áudio gerado! Reproduzindo...");
                                    
                                    // Reproduz o áudio automaticamente
                                    audioManager.reproduzir(audioSelecionado);
                                    controleGravacaoContainer.setVisibility(View.VISIBLE);
                                    btnPlayPauseGuia.setText("⏸");
                                    
                                    Toast.makeText(MainActivity.this, 
                                            "Áudio salvo em 'Meus Áudios'",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                            
                            @Override
                            public void onErro(String mensagem) {
                                handler.post(() -> {
                                    btnGerarVoz.setEnabled(true);
                                    carregandoIndicador.setVisibility(View.GONE);
                                    status.setText("❌ Erro ao salvar: " + mensagem);
                                    Toast.makeText(MainActivity.this,
                                            "Erro ao salvar áudio: " + mensagem,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                );
            }
            
            @Override
            public void onErro(String mensagem) {
                handler.post(() -> {
                    btnGerarVoz.setEnabled(true);
                    carregandoIndicador.setVisibility(View.GONE);
                    status.setText("❌ Erro: " + mensagem);
                    Toast.makeText(MainActivity.this,
                            "Erro na API: " + mensagem,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void alternarGravacao() {
        if (recording != null) {
            audioManager.parar();
            recording.stop();
            emGravacao = false;
            controleGravacaoContainer.setVisibility(View.GONE);
            return;
        }

        if (videoCapture == null) {
            Toast.makeText(
                    this,
                    "A câmera ainda não está pronta.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String nome = "DiretorNoOuvido_" +
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(new Date());

        ContentValues valores = new ContentValues();
        valores.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                nome
        );
        valores.put(
                MediaStore.MediaColumns.MIME_TYPE,
                "video/mp4"
        );

        MediaStoreOutputOptions saida =
                new MediaStoreOutputOptions.Builder(
                        getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                        .setContentValues(valores)
                        .build();

        PendingRecording pendente =
                videoCapture.getOutput()
                        .prepareRecording(this, saida);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED) {
            pendente = pendente.withAudioEnabled();
        }

        recording = pendente.start(
                ContextCompat.getMainExecutor(this),
                evento -> {
                    if (evento instanceof VideoRecordEvent.Start) {
                        gravar.setText("PARAR GRAVAÇÃO");
                        status.setText(
                                "Gravando sua voz pelo microfone..."
                        );
                        emGravacao = true;
                        controleGravacaoContainer.setVisibility(View.VISIBLE);

                        // Reproduz áudio selecionado após 1.8s
                        if (audioSelecionado != null) {
                            handler.postDelayed(
                                    () -> {
                                        audioManager.reproduzir(audioSelecionado);
                                        btnPlayPauseGuia.setText("⏸");
                                    },
                                    1800
                            );
                        }
                    }

                    if (evento instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize fim =
                                (VideoRecordEvent.Finalize) evento;

                        recording = null;
                        emGravacao = false;
                        gravar.setText(
                                "GRAVAR VÍDEO + GUIA NO FONE"
                        );
                        controleGravacaoContainer.setVisibility(View.GONE);

                        if (fim.hasError()) {
                            status.setText(
                                    "Erro ao gravar: " +
                                            fim.getError()
                            );
                        } else {
                            status.setText(
                                    "Vídeo salvo na galeria."
                            );
                        }
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        audioManager.parar();
        super.onDestroy();
    }
}
