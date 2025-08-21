package com.example.artbookjava;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.artbookjava.databinding.RecyclerRowBinding;

import java.util.ArrayList;

public class ArtAdapter extends RecyclerView.Adapter<ArtAdapter.ArtHolder> {
    ArrayList<Art> artArrayList;
    public ArtAdapter(ArrayList<Art> artArrayList) {
        this.artArrayList = artArrayList;
    }

    @NonNull
    @Override
    public ArtHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerRowBinding binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ArtHolder(binding);
    }

    // DOĞRU KULLANIM:
    @Override
    public void onBindViewHolder(@NonNull ArtHolder holder, int position) {
        // 'position' parametresini sadece anlık veri bağlama için kullanın.
        holder.binding.recyclerViewTextView.setText(artArrayList.get(position).name);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // OnClickListener içinde güncel pozisyonu almak için holder.getAdapterPosition() kullanın.
                int currentPosition = holder.getAdapterPosition();

                // getAdapterPosition() RecyclerView.NO_POSITION dönebilir, bu durumu kontrol edin.
                // Bu durum, eleman henüz layout'a tam olarak yerleşmediyse veya silinmek üzereyse olabilir.
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Intent intent = new Intent(holder.itemView.getContext(), ArtActivity.class);
                    intent.putExtra("info", "old");
                    intent.putExtra("artId", artArrayList.get(currentPosition).id);
                    holder.itemView.getContext().startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return artArrayList.size();
    };

    public static class ArtHolder extends RecyclerView.ViewHolder {
        private RecyclerRowBinding binding;
        public ArtHolder(RecyclerRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
