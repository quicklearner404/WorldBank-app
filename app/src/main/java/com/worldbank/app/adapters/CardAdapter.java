package com.worldbank.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.worldbank.app.R;
import com.worldbank.app.activities.AddCardActivity;
import com.worldbank.app.models.Card;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CARD = 1;
    private static final int TYPE_PLUS = 2;

    private final Context context;
    private final List<Card> cards;
    private int selectedPosition = 0; // Default first card selected
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(Card card, int position);
    }

    public CardAdapter(Context context, List<Card> cards, OnCardClickListener listener) {
        this.context = context;
        this.cards = cards;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == cards.size()) ? TYPE_PLUS : TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PLUS) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_card_plus, parent, false);
            return new PlusViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_card_small, parent, false);
            return new CardViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CardViewHolder) {
            Card card = cards.get(position);
            CardViewHolder cardHolder = (CardViewHolder) holder;
            
            cardHolder.tvSmallCardName.setText(card.getHolderName());
            cardHolder.tvSmallCardNumber.setText(card.getMaskedNumber());
            cardHolder.tvExpiry.setText("Exp " + card.getExpiry());
            
            if ("VISA".equalsIgnoreCase(card.getCardType())) {
                cardHolder.ivSmallCardLogo.setImageResource(R.drawable.ic_payments);
            } else {
                cardHolder.ivSmallCardLogo.setImageResource(R.drawable.ic_mastercard);
            }

            // Highlighting logic for selection
            if (selectedPosition == position) {
                cardHolder.itemView.setAlpha(1.0f);
                cardHolder.itemView.setScaleX(1.02f);
                cardHolder.itemView.setScaleY(1.02f);
            } else {
                cardHolder.itemView.setAlpha(0.6f);
                cardHolder.itemView.setScaleX(1.0f);
                cardHolder.itemView.setScaleY(1.0f);
            }

            cardHolder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                if (listener != null) listener.onCardClick(card, selectedPosition);
            });

        } else if (holder instanceof PlusViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddCardActivity.class);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return cards.size() + 1;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvSmallCardName, tvSmallCardNumber, tvExpiry;
        ImageView ivSmallCardLogo;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSmallCardName   = itemView.findViewById(R.id.tvSmallCardName);
            tvSmallCardNumber = itemView.findViewById(R.id.tvSmallCardNumber);
            tvExpiry          = itemView.findViewById(R.id.tvExpiry);
            ivSmallCardLogo   = itemView.findViewById(R.id.ivSmallCardLogo);
        }
    }

    public static class PlusViewHolder extends RecyclerView.ViewHolder {
        public PlusViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
