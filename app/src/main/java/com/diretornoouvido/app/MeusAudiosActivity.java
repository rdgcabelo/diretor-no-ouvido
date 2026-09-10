package com.diretornoouvido.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.diretornoouvido.app.adapter.AudioListAdapter;
import com.diretornoouvido.app.media.AudioLocalManager;
import com.diretornoouvido.app.model.AudioLocal;
import java.util.List;

/**
 * Activity para gerenciar áudios armazenados localmente.
 * Permite reproduzir, renomear, excluir e selecionar como voz-guia.
 */
public class MeusAudiosActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private AudioListAdapter adapter;
    private AudioLocalManager audioManager;
    private TextView statusTexto;
    private LinearLayout playerContainer;
    private Button btnPlay;
    private Button btnRewind10;
    private Button btnFecha;
    private TextView tempoAtual;
    private ProgressBar progressBar;
    
    private AudioLocal audioAtual;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUI());
        
        audioManager = new AudioLocalManager(this);
        
        // Carrega lista de áudios
        audioManager.obterTodosAudios().observe(this, new Observer<List<AudioLocal>>() {
            @Override
            public void onChanged(List<AudioLocal> audios) {
                adapter.atualizar(audios);
                if (audios.isEmpty()) {
                    statusTexto.setText("Nenhum áudio salvo ainda.");
                } else {
                    statusTexto.setText(audios.size() + " áudio(s) salvo(s)");
                }
            }
        });
    }
    
    private android.view.View createUI() {
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(16, 16, 16, 16);
        
        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Meus Áudios");
        titulo.setTextSize(20);
        titulo.setPadding(0, 0, 0, 12);
        raiz.addView(titulo);
        
        // Status
        statusTexto = new TextView(this);
        statusTexto.setText("Carregando...");
        statusTexto.setTextSize(12);
        statusTexto.setPadding(0, 0, 0, 8);
        raiz.addView(statusTexto);
        
        // RecyclerView
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AudioListAdapter(
                audios -> reproduzirAudio(audios),
                audio -> mostrarMenuOpcoes(audio)
        );
        recyclerView.setAdapter(adapter);
        
        LinearLayout.LayoutParams recyclerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        raiz.addView(recyclerView, recyclerParams);
        
        // Container do player
        playerContainer = new LinearLayout(this);
        playerContainer.setOrientation(LinearLayout.VERTICAL);
        playerContainer.setPadding(8, 8, 8, 8);
        playerContainer.setBackgroundColor(android.graphics.Color.parseColor("#f0f0f0"));
        playerContainer.setVisibility(android.view.View.GONE);
        
        // Título do áudio em reprodução
        TextView tituloAtual = new TextView(this);
        tituloAtual.setTextSize(14);
        tituloAtual.setPadding(0, 0, 0, 4);
        playerContainer.addView(tituloAtual);
        
        // Progresso
        progressBar = new ProgressBar(this, null, 
                android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        playerContainer.addView(progressBar, progressParams);
        
        // Tempo
        tempoAtual = new TextView(this);
        tempoAtual.setTextSize(11);
        tempoAtual.setText("00:00 / 00:00");
        tempoAtual.setPadding(0, 4, 0, 8);
        playerContainer.addView(tempoAtual);
        
        // Botões
        LinearLayout botoesContainer = new LinearLayout(this);
        botoesContainer.setOrientation(LinearLayout.HORIZONTAL);
        botoesContainer.setWeightSum(3);
        
        btnPlay = new Button(this);
        btnPlay.setText("▶ Reproduzir");
        btnPlay.setOnClickListener(v -> {
            if (audioAtual != null) {
                if (audioManager.estaReproduzindo()) {
                    audioManager.pausar();
                    btnPlay.setText("▶ Reproduzir");
                } else {
                    audioManager.retomar();
                    btnPlay.setText("⏸ Pausar");
                }
            }
        });
        LinearLayout.LayoutParams btnParams1 =
                new LinearLayout.LayoutParams(0, 
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        btnParams1.setMargins(2, 0, 2, 0);
        botoesContainer.addView(btnPlay, btnParams1);
        
        btnRewind10 = new Button(this);
        btnRewind10.setText("↶ 10s");
        btnRewind10.setOnClickListener(v -> {
            if (audioAtual != null) {
                audioManager.retroceder10Segundos();
            }
        });
        LinearLayout.LayoutParams btnParams2 =
                new LinearLayout.LayoutParams(0,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        btnParams2.setMargins(2, 0, 2, 0);
        botoesContainer.addView(btnRewind10, btnParams2);
        
        btnFecha = new Button(this);
        btnFecha.setText("✕ Fechar");
        btnFecha.setOnClickListener(v -> {
            audioManager.parar();
            playerContainer.setVisibility(android.view.View.GONE);
        });
        LinearLayout.LayoutParams btnParams3 =
                new LinearLayout.LayoutParams(0,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        btnParams3.setMargins(2, 0, 2, 0);
        botoesContainer.addView(btnFecha, btnParams3);
        
        playerContainer.addView(botoesContainer);
        
        raiz.addView(playerContainer);
        
        // Botão voltar
        Button voltarBtn = new Button(this);
        voltarBtn.setText("← Voltar");
        voltarBtn.setOnClickListener(v -> {
            audioManager.parar();
            finish();
        });
        raiz.addView(voltarBtn);
        
        return raiz;
    }
    
    private void reproduzirAudio(AudioLocal audio) {
        if (audioAtual != null && audioAtual.id == audio.id && 
                audioManager.estaReproduzindo()) {
            // Já está reproduzindo, pausa
            audioManager.pausar();
            btnPlay.setText("▶ Reproduzir");
            return;
        }
        
        audioAtual = audio;
        playerContainer.setVisibility(android.view.View.VISIBLE);
        btnPlay.setText("⏸ Pausar");
        
        audioManager.setPlaybackListener(new AudioLocalManager.OnPlaybackListener() {
            @Override
            public void onPlayback(int posicaoMs, int duracaoMs) {
                progressBar.setMax(duracaoMs);
                progressBar.setProgress(posicaoMs);
                tempoAtual.setText(formatarTempo(posicaoMs) + " / " + 
                        formatarTempo(duracaoMs));
            }
            
            @Override
            public void onRewind(int novaPosicaoMs) {
                tempoAtual.setText(formatarTempo(novaPosicaoMs) + " / " +
                        formatarTempo(audioManager.obterDuracao()));
            }
            
            @Override
            public void onCompletar() {
                btnPlay.setText("▶ Reproduzir");
            }
            
            @Override
            public void onErro(String mensagem) {
                Toast.makeText(MeusAudiosActivity.this, 
                        mensagem, Toast.LENGTH_SHORT).show();
            }
        });
        
        audioManager.reproduzir(audio);
    }
    
    private void mostrarMenuOpcoes(AudioLocal audio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(audio.titulo);
        
        builder.setItems(new String[]{
                "Selecionar como voz-guia",
                "Renomear",
                "Excluir"
        }, (dialog, which) -> {
            switch (which) {
                case 0:
                    selecionarComoVozGuia(audio);
                    break;
                case 1:
                    renomearAudio(audio);
                    break;
                case 2:
                    deletarAudio(audio);
                    break;
            }
        });
        
        builder.show();
    }
    
    private void selecionarComoVozGuia(AudioLocal audio) {
        Intent intent = new Intent();
        intent.putExtra("audio_id", audio.id);
        intent.putExtra("audio_path", audio.caminhoArquivo);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private void renomearAudio(AudioLocal audio) {
        EditText input = new EditText(this);
        input.setText(audio.titulo);
        
        new AlertDialog.Builder(this)
                .setTitle("Renomear áudio")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String novoNome = input.getText().toString().trim();
                    if (!novoNome.isEmpty()) {
                        audioManager.renomear(audio, novoNome);
                        Toast.makeText(this, "Renomeado com sucesso", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    private void deletarAudio(AudioLocal audio) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir áudio?")
                .setMessage("Esta ação é irreversível.")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    audioManager.deletar(audio);
                    Toast.makeText(this, "Áudio excluído", 
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    private String formatarTempo(int ms) {
        int segundos = ms / 1000;
        int minutos = segundos / 60;
        segundos = segundos % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }
    
    @Override
    protected void onDestroy() {
        audioManager.parar();
        super.onDestroy();
    }
}
