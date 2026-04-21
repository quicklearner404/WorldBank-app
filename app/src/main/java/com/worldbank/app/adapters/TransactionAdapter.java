package com.worldbank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.worldbank.app.R;
import com.worldbank.app.models.Transaction;
import java.util.List;

/**
 * TransactionAdapter
 * ------------------
 * RecyclerView adapter for transaction list items.
 * Used in HomeActivity (recent) and TransactionHistoryActivity (full list).
 *
 * TODO: Bind all views from item_transaction.xml per the mockup.
 * Owner: Dev 2
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction txn = transactions.get(position);
        // TODO: bind txn data to views
        // holder.tvTitle.setText(txn.getRecipientName());
        // holder.tvAmount.setText(txn.getFormattedAmount());
        // holder.tvAmount.setTextColor(txn.isCredit() ? green : red);
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // TODO: declare and find views from item_transaction.xml
        // TextView tvTitle, tvDate, tvAmount;
        // ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // TODO: bind views
        }
    }
}
