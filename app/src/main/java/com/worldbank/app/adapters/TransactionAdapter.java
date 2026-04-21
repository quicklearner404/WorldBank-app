package com.worldbank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.worldbank.app.R;
import com.worldbank.app.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TransactionAdapter
 * ───────────────────
 * Owner : Dev 2
 * Used  : HomeActivity (recent list) + TransactionHistoryActivity (full list)
 *
 * Each row shows:
 *  - Initials box OR category icon
 *  - Transaction name / recipient
 *  - Date + time
 *  - Amount in red (debit) or green (credit)
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    // ══════════════════════════════════════════════════════════════
    //  VIEWHOLDER
    // ══════════════════════════════════════════════════════════════

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTransactionInitials;
        final ImageView ivTransactionIcon;
        final TextView tvTransactionName;
        final TextView tvTransactionDate;
        final TextView tvTransactionAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionInitials = itemView.findViewById(R.id.tvTransactionInitials);
            ivTransactionIcon     = itemView.findViewById(R.id.ivTransactionIcon);
            tvTransactionName     = itemView.findViewById(R.id.tvTransactionName);
            tvTransactionDate     = itemView.findViewById(R.id.tvTransactionDate);
            tvTransactionAmount   = itemView.findViewById(R.id.tvTransactionAmount);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ADAPTER METHODS
    // ══════════════════════════════════════════════════════════════

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction txn = transactions.get(position);

        // ── Name ────────────────────────────────────────────────
        String name = txn.getRecipientName() != null ? txn.getRecipientName() : txn.getCategory();
        holder.tvTransactionName.setText(name);

        // ── Date ────────────────────────────────────────────────
        if (txn.getTimestamp() != null) {
            Date date = txn.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM, yyyy  •  HH:mm", Locale.getDefault());
            holder.tvTransactionDate.setText(sdf.format(date));
        } else {
            holder.tvTransactionDate.setText("");
        }

        // ── Amount (colour + sign) ───────────────────────────────
        holder.tvTransactionAmount.setText(txn.getFormattedAmount());
        if (txn.isCredit()) {
            holder.tvTransactionAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.green_credit));
        } else {
            holder.tvTransactionAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.red_debit));
        }

        // ── Icon: initials for person transfers, icon for others ─
        String category = txn.getCategory() != null ? txn.getCategory() : "";
        if (category.equals(Transaction.CAT_TRANSFER)) {
            // Show initials from recipient name
            holder.tvTransactionInitials.setVisibility(View.VISIBLE);
            holder.ivTransactionIcon.setVisibility(View.GONE);
            holder.tvTransactionInitials.setText(getInitials(name));
        } else {
            // Show category icon
            holder.tvTransactionInitials.setVisibility(View.GONE);
            holder.ivTransactionIcon.setVisibility(View.VISIBLE);
            holder.ivTransactionIcon.setImageResource(getCategoryIcon(category));
        }
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns up to 2 initials from a name string.
     * "Ahmad Farhan" → "AF",  "Ahmad" → "A"
     */
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    /**
     * Returns a drawable resource for each transaction category.
     * Using Android built-ins for now — replace with custom ic_ drawables later.
     */
    private int getCategoryIcon(String category) {
        switch (category) {
            case Transaction.CAT_SHOPPING:
                return android.R.drawable.ic_menu_agenda;
            case Transaction.CAT_GAME:
                return android.R.drawable.ic_menu_manage;
            case Transaction.CAT_TOPUP:
                return android.R.drawable.ic_input_add;
            case Transaction.CAT_PAYMENT:
                return android.R.drawable.ic_menu_send;
            case Transaction.CAT_WITHDRAW:
                return android.R.drawable.ic_menu_revert;
            default:
                return android.R.drawable.ic_menu_agenda;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC METHODS (for updating list later)
    // ══════════════════════════════════════════════════════════════

    /** Call this to refresh the list with new data */
    public void updateList(List<Transaction> newList) {
        transactions.clear();
        transactions.addAll(newList);
        notifyDataSetChanged();
    }
}
