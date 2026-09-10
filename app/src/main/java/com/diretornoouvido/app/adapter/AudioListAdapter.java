package com.diretornoouvido.app.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.diretornoouvido.app.model.AudioLocal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {
    
    private List<AudioLocal> audios = new ArrayList<>();
    private OnAudioClickListener playListener;
    private OnAudioClickListener menuListener;
    
    public interface OnAudioClickListener {
        void onClick(AudioLocal audio);
    }
    
    public AudioListAdapter(OnAudioClickListener playListener,
                           OnAudioClickListener menuListener) {
        this.playListener = playListener;
        this.menuListener = menuListener;
    }
    
    public void atualizar(List<AudioLocal> novosList) {
        this.audios = novosList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout item = new LinearLayout(parent.getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(12, 8, 12, 8);
        item.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new ViewHolder(item);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioLocal audio = audios.get(position);
        holder.bind(audio);
    }
    
    @Override
    public int getItemCount() {
        return audios.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        ViewHolder(View itemView) {
            super(itemView);
        }
        
        void bind(AudioLocal audio) {
            LinearLayout container = (LinearLayout) itemView;
            container.removeAllViews();
            
            // Linha 1: Título + Botão de menu
            LinearLayout tituloContainer = new LinearLayout(itemView.getContext());
            tituloContainer.setOrientation(LinearLayout.HORIZONTAL);
            tituloContainer.setWeightSum(1);
            
            TextView titulo = new TextView(itemView.getContext());
            titulo.setText(audio.titulo);
            titulo.setTextSize(16);
            titulo.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.85f);
            tituloContainer.addView(titulo, titleParams);
            
            Button menu = new Button(itemView.getContext());
            menu.setText("⋮");
            menu.setOnClickListener(v -> menuListener.onClick(audio));
            LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.15f);
            tituloContainer.addView(menu, menuParams);
            
            container.addView(tituloContainer);
            
            // Linha 2: Fonte + Offline
            LinearLayout metadataContainer = new LinearLayout(itemView.getContext());
            metadataContainer.setOrientation(LinearLayout.HORIZONTAL);
            
            TextView fonte = new TextView(itemView.getContext());
            fonte.setText("Fonte: " + audio.fonte);
            fonte.setTextSize(11);
            fonte.setTextColor(android.graphics.Color.GRAY);
            metadataContainer.addView(fonte);
            
            TextView offline = new TextView(itemView.getContext());
            offline.setText(" • ✓ Offline");
            offline.setTextSize(11);
            offline.setTextColor(android.graphics.Color.GREEN);
            metadataContainer.addView(offline);
            
            container.addView(metadataContainer);
            
            // Linha 3: Data + Duração
            LinearLayout infoContainer = new LinearLayout(itemView.getContext());
            infoContainer.setOrientation(LinearLayout.HORIZONTAL);
            
            TextView data = new TextView(itemView.getContext());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", 
                    Locale.getDefault());
            data.setText(sdf.format(new Date(audio.dataCriacao)));
            data.setTextSize(10);
            data.setTextColor(android.graphics.Color.GRAY);
            infoContainer.addView(data);
            
            TextView duracao = new TextView(itemView.getContext());
            long segundos = audio.duracao / 1000;
            duracao.setText(String.format(" • %02d:%02d", 
                    segundos / 60, segundos % 60));
            duracao.setTextSize(10);
            duracao.setTextColor(android.graphics.Color.GRAY);
            infoContainer.addView(duracao);
            
            container.addView(infoContainer);
            
            // Botão Play
            Button play = new Button(itemView.getContext());
            play.setText("▶ Reproduzir");
            play.setOnClickListener(v -> playListener.onClick(audio));
            container.addView(play);
            
            // Separador visual
            View separator = new View(itemView.getContext());
            separator.setBackgroundColor(android.graphics.Color.parseColor("#e0e0e0"));
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            sepParams.setMargins(0, 8, 0, 8);
            container.addView(separator, sepParams);
        }
    }
}
