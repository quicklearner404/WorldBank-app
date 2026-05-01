package com.worldbank.app.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.CardAdapter;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;
import java.util.ArrayList;
import java.util.List;

public class PaymentSettingsActivity extends AppCompatActivity implements CardAdapter.OnCardClickListener {

    private RecyclerView rvSavedCards;
    private CardAdapter cardAdapter;
    private final List<Card> cardList = new ArrayList<>();
    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_settings);

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        rvSavedCards = findViewById(R.id.rvSavedCards);
        cardAdapter = new CardAdapter(this, cardList, this);
        rvSavedCards.setLayoutManager(new LinearLayoutManager(this));
        rvSavedCards.setAdapter(cardAdapter);

        loadCards();
    }

    private void loadCards() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }
            cardAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onCardClick(Card card, int position) {
        // Option to remove or set as primary
    }
}
