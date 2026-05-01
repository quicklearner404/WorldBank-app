package com.worldbank.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.worldbank.app.R;
import com.worldbank.app.models.Biller;
import java.util.List;

public class BillerAdapter extends RecyclerView.Adapter<BillerAdapter.ViewHolder> {

    private final List<Biller> billers;
    private final OnBillerClickListener listener;

    public interface OnBillerClickListener {
        void onBillerClick(Biller biller);
    }

    public BillerAdapter(List<Biller> billers, OnBillerClickListener listener) {
        this.billers = billers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_biller, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Biller biller = billers.get(position);
        holder.tvName.setText(biller.getName());
        holder.tvCategory.setText(biller.getCategory());
        // In a real app, use Glide to load logo from URL. Using placeholder here.
        holder.ivLogo.setImageResource(biller.getLogoRes());
        holder.itemView.setOnClickListener(v -> listener.onBillerClick(biller));
    }

    @Override
    public int getItemCount() {
        return billers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory;
        ImageView ivLogo;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvBillerName);
            tvCategory = itemView.findViewById(R.id.tvBillerCategory);
            ivLogo = itemView.findViewById(R.id.ivBillerLogo);
        }
    }
}
