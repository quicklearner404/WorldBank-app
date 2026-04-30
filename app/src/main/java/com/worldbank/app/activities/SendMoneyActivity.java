package com.worldbank.app.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * SendMoneyActivity handles the full transfer flow.
 * DEV_BYPASS mode skips Firestore and goes straight to TransferSuccessActivity.
 */
public class SendMoneyActivity extends AppCompatActivity {

    // views on the main send money screen
    ImageButton ibBack;
    TextView tvCardNumber;
    TextView tvCardHolder;
    TextView tvCardExpiry;
    TextView tvRecipientName;
    TextView tvRecipientAccount;
    TextView tvTransferAmount;
    TextView btnChange;
    Button btnSendMoney;
    EditText etAmount;
    LinearLayout llRecipientRow;

    // firebase and session helpers
    TransactionRepository repo;
    FirebaseAuth auth;
    SessionManager sessionManager;
    FirebaseFirestore db;

    // ids passed from HomeActivity
    String cardId    = "";
    String accountId = "";

    // currently selected recipient details
    String selectedRecipientName      = "";
    String selectedRecipientAccount   = "";
    String selectedRecipientBank      = "";
    String selectedRecipientUid       = "";
    String selectedRecipientAccountId = "";
    String selectedContactId          = "";
    String selectedTransferType       = Transaction.TRANSFER_IBFT;

    // loaded card for the top widget
    Card currentCard;

    // the permanent prefix that the user cannot delete or move past
    // PK36WBNK is the World Bank prefix for internal accounts
    // for external banks just PK is locked and the user types the rest
    static final String IBAN_PREFIX = "PK36WBNK";

    // guards the iban watcher from triggering itself
    boolean isFormattingIban = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        cardId    = getIntent().getStringExtra("cardId");
        accountId = getIntent().getStringExtra("accountId");
        if (cardId    == null) cardId    = "";
        if (accountId == null) accountId = "";

        repo           = new TransactionRepository();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        db             = FirebaseFirestore.getInstance();

        init();
        loadCardData();
        setupClickListeners();
        setupAmountWatcher();
        promptRecipientIfEmpty();
    }

    // finds and stores all views from the layout
    private void init() {
        ibBack             = findViewById(R.id.ibBack);
        tvCardNumber       = findViewById(R.id.tvCardNumber);
        tvCardHolder       = findViewById(R.id.tvCardHolder);
        tvCardExpiry       = findViewById(R.id.tvCardExpiry);
        tvRecipientName    = findViewById(R.id.tvRecipientName);
        tvRecipientAccount = findViewById(R.id.tvRecipientAccount);
        tvTransferAmount   = findViewById(R.id.tvTransferAmount);
        btnChange          = findViewById(R.id.btnChange);
        btnSendMoney       = findViewById(R.id.btnSendMoney);
        etAmount           = findViewById(R.id.etAmount);
        llRecipientRow     = findViewById(R.id.llRecipientRow);
    }

    // loads the card from Firestore and populates the purple card widget
    private void loadCardData() {
        if (cardId.isEmpty()) {
            showPlaceholderCard();
            return;
        }
        repo.getCard(cardId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentCard = doc.toObject(Card.class);
                if (currentCard != null) {
                    currentCard.setCardId(doc.getId());
                    tvCardNumber.setText(currentCard.getMaskedNumber());
                    tvCardHolder.setText(currentCard.getHolderName());
                    tvCardExpiry.setText("Exp " + currentCard.getExpiry());
                }
            }
        }).addOnFailureListener(e -> showPlaceholderCard());
    }

    // fills the card widget with blank dashes when no card is loaded
    private void showPlaceholderCard() {
        tvCardNumber.setText("**** **** **** ****");
        tvCardHolder.setText(sessionManager.getUserName());
        tvCardExpiry.setText("Exp --/--");
    }

    // wires the back button and send money button
    private void setupClickListeners() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        ibBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        btnChange.setOnClickListener(v -> showRecipientPicker());

        btnSendMoney.setOnClickListener(v -> validateAndSend());
    }

    // updates the pkr preview label whenever the amount field changes
    private void setupAmountWatcher() {
        if (etAmount == null) return;
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAmountDisplay(s.toString());
            }
        });
    }

    // formats and sets the pkr preview text below the amount input
    private void updateAmountDisplay(String raw) {
        try {
            double amount = Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
            tvTransferAmount.setText(String.format("Rs. %,.0f", amount));
        } catch (NumberFormatException e) {
            tvTransferAmount.setText("Rs. 0");
        }
    }

    // opens the picker dialog right away when no recipient has been chosen
    private void promptRecipientIfEmpty() {
        if (selectedRecipientAccount.isEmpty()) {
            showRecipientPicker();
        }
    }

    // builds and shows the recipient picker dialog with all its logic
    private void showRecipientPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recipient_picker);
        dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        // prevent tapping outside from closing so user must use the X button
        dialog.setCanceledOnTouchOutside(false);

        EditText etRecipientAccount = dialog.findViewById(R.id.etRecipientAccount);
        EditText etRecipientName    = dialog.findViewById(R.id.etRecipientName);
        ImageButton btnCloseDialog  = dialog.findViewById(R.id.btnCloseDialog);
        TextView tvBankHBL          = dialog.findViewById(R.id.tvBankHBL);
        TextView tvBankMeezan       = dialog.findViewById(R.id.tvBankMeezan);
        TextView tvBankUBL          = dialog.findViewById(R.id.tvBankUBL);
        TextView tvBankMCB          = dialog.findViewById(R.id.tvBankMCB);
        TextView tvBankWorldBank    = dialog.findViewById(R.id.tvBankWorldBank);
        TextView tvBankJazz         = dialog.findViewById(R.id.tvBankJazz);
        Button btnConfirmRecipient  = dialog.findViewById(R.id.btnConfirmRecipient);
        RecyclerView rvContacts     = dialog.findViewById(R.id.rvContacts);

        // set the locked prefix immediately so the user sees it on first tap
        etRecipientAccount.setText(IBAN_PREFIX);
        etRecipientAccount.setSelection(IBAN_PREFIX.length());

        // attach the watcher that locks the prefix and auto formats the rest
        setupLockedIbanWatcher(etRecipientAccount);

        // X button dismisses the dialog and goes back if nothing was selected
        btnCloseDialog.setOnClickListener(v -> {
            dialog.dismiss();
            if (selectedRecipientAccount.isEmpty()) {
                finish();
            }
        });

        // load this users saved contacts into the horizontal chip row
        loadContactsIntoDialog(rvContacts, etRecipientAccount, etRecipientName);

        View[] bankButtons = { tvBankHBL, tvBankMeezan, tvBankUBL, tvBankMCB,
                tvBankWorldBank, tvBankJazz };
        String[] bankNames = { Contact.BANK_HBL, Contact.BANK_MEEZAN, Contact.BANK_UBL,
                Contact.BANK_MCB, Contact.BANK_WORLDBANK, Contact.BANK_JAZZCASH };

        // start with HBL highlighted so something is always selected
        final String[] chosenBank = { Contact.BANK_HBL };
        bankButtons[0].setAlpha(1.0f);
        for (int j = 1; j < bankButtons.length; j++) bankButtons[j].setAlpha(0.4f);

        for (int i = 0; i < bankButtons.length; i++) {
            int idx = i;
            bankButtons[i].setOnClickListener(v -> {
                chosenBank[0] = bankNames[idx];

                // dim all chips then light up the tapped one
                for (View b : bankButtons) b.setAlpha(0.4f);
                bankButtons[idx].setAlpha(1.0f);

                // World Bank transfers are free internal transfers
                if (bankNames[idx].equals(Contact.BANK_WORLDBANK)) {
                    selectedTransferType = Transaction.TRANSFER_INTERNAL;
                } else if (bankNames[idx].equals(Contact.BANK_JAZZCASH)) {
                    selectedTransferType = Transaction.TRANSFER_JAZZCASH;
                } else {
                    selectedTransferType = Transaction.TRANSFER_IBFT;
                }
            });
        }

        // this is the ADD RECIPIENT button that confirms the entry
        btnConfirmRecipient.setText("Add Recipient");
        btnConfirmRecipient.setOnClickListener(v -> {
            String rawAccount = etRecipientAccount.getText().toString().trim();
            String name       = etRecipientName.getText().toString().trim();

            // name field must not be blank
            if (name.isEmpty()) {
                etRecipientName.setError("Please enter recipient name");
                etRecipientName.requestFocus();
                return;
            }

            // strip spaces to get the raw iban for length check
            String cleanAccount = rawAccount.replaceAll("\\s+", "").toUpperCase();

            // must have typed digits beyond the locked prefix
            if (cleanAccount.equals(IBAN_PREFIX) || cleanAccount.length() < 10) {
                etRecipientAccount.setError("Please enter a valid account number after " + IBAN_PREFIX);
                etRecipientAccount.requestFocus();
                return;
            }

            // pk ibans are exactly 24 characters total
            if (cleanAccount.length() != 24) {
                etRecipientAccount.setError(
                        "IBAN must be 24 characters. You have " + cleanAccount.length() + " so far.");
                etRecipientAccount.requestFocus();
                return;
            }

            // all good, store the recipient and close the dialog
            setRecipient(name, cleanAccount, chosenBank[0], "", "", selectedContactId);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Attaches a TextWatcher that keeps IBAN_PREFIX permanently locked at the start.
     * Works exactly like the phone watcher in SignUpActivity:
     * the prefix is always re-inserted if the user tries to delete it,
     * and the cursor is never allowed to move before the prefix end.
     */
    private void setupLockedIbanWatcher(EditText etAccount) {
        etAccount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingIban) return;
                isFormattingIban = true;

                String current = s.toString();

                // if the prefix was deleted or changed, restore it
                if (!current.startsWith(IBAN_PREFIX)) {
                    // strip everything and keep only the user typed digits after prefix
                    String userPart = current.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

                    // remove the prefix characters from the front if they are still partly there
                    String prefixClean = IBAN_PREFIX.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                    if (userPart.startsWith(prefixClean)) {
                        userPart = userPart.substring(prefixClean.length());
                    }

                    // cap total to 24 characters including the 8 char prefix
                    if (userPart.length() > 16) userPart = userPart.substring(0, 16);

                    String rebuilt = IBAN_PREFIX + userPart;
                    etAccount.setText(rebuilt);
                    etAccount.setSelection(rebuilt.length());

                } else {
                    // prefix is intact, just clean and cap the user typed suffix
                    String suffix = current.substring(IBAN_PREFIX.length());

                    // keep only alphanumeric in the suffix
                    String cleanSuffix = suffix.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

                    // suffix can be at most 16 characters so total stays at 24
                    if (cleanSuffix.length() > 16) cleanSuffix = cleanSuffix.substring(0, 16);

                    String rebuilt = IBAN_PREFIX + cleanSuffix;
                    etAccount.setText(rebuilt);
                    etAccount.setSelection(rebuilt.length());
                }

                isFormattingIban = false;
            }
        });

        // also guard the cursor so it cannot be placed inside the locked prefix
        etAccount.setOnClickListener(v -> {
            if (etAccount.getSelectionStart() < IBAN_PREFIX.length()) {
                etAccount.setSelection(IBAN_PREFIX.length());
            }
        });
    }

    // fetches saved contacts and hooks them up to the chip list in the dialog
    private void loadContactsIntoDialog(RecyclerView rv,
                                        EditText etAccount, EditText etName) {
        String uid = getCurrentUserId();
        if (uid.isEmpty() || rv == null) return;

        List<Contact> contacts = new ArrayList<>();

        ContactPickerAdapter adapter = new ContactPickerAdapter(contacts, contact -> {
            // tapping a chip fills both fields and sets the transfer type
            etAccount.setText(contact.getAccountNumber());
            etAccount.setSelection(etAccount.getText().length());
            etName.setText(contact.getName());
            selectedContactId    = contact.getContactId();
            selectedRecipientUid = contact.getOwnerUid();

            if (Contact.BANK_WORLDBANK.equals(contact.getBankName())) {
                selectedTransferType = Transaction.TRANSFER_INTERNAL;
            } else if (Contact.BANK_JAZZCASH.equals(contact.getBankName())) {
                selectedTransferType = Transaction.TRANSFER_JAZZCASH;
            } else {
                selectedTransferType = Transaction.TRANSFER_IBFT;
            }

            Toast.makeText(this, contact.getName() + " selected", Toast.LENGTH_SHORT).show();
        });

        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);

        repo.getContactsQuery(uid).get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Contact c = doc.toObject(Contact.class);
                        c.setContactId(doc.getId());
                        contacts.add(c);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // contacts list is optional so failure is silent
                });
    }

    // stores the recipient details and updates the labels on the main screen
    public void setRecipient(String name, String account, String bank,
                             String recipientUid, String recipientAccountId, String contactId) {
        selectedRecipientName      = name;
        selectedRecipientAccount   = account;
        selectedRecipientBank      = bank;
        selectedRecipientUid       = recipientUid;
        selectedRecipientAccountId = recipientAccountId;
        selectedContactId          = contactId;

        tvRecipientName.setText(name);
        tvRecipientAccount.setText(account);
    }

    // validates all fields then either simulates or executes the real transfer
    private void validateAndSend() {
        if (selectedRecipientAccount.isEmpty()) {
            Toast.makeText(this, "Please add a recipient first", Toast.LENGTH_SHORT).show();
            showRecipientPicker();
            return;
        }

        String amountStr = etAmount != null ? etAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            etAmount.setError("Please enter an amount");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount entered");
            return;
        }

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than zero");
            return;
        }

        btnSendMoney.setEnabled(false);
        btnSendMoney.setText("Processing...");

        double adminFee = TransactionRepository.getAdminFee(selectedTransferType);

        // bypass mode skips firestore and goes straight to the success screen
        if (SessionManager.DEV_BYPASS) {
            simulateTransferSuccess(amount, adminFee);
            return;
        }

        if (accountId.isEmpty()) {
            Toast.makeText(this, "Account not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            btnSendMoney.setEnabled(true);
            btnSendMoney.setText(getString(R.string.btn_send_money));
            return;
        }

        String uid = getCurrentUserId();

        repo.sendMoney(
                uid, accountId, cardId,
                selectedRecipientUid, selectedRecipientAccountId,
                selectedRecipientAccount, selectedRecipientName,
                selectedRecipientBank, amount,
                selectedTransferType, "", selectedContactId
        ).addOnSuccessListener(referenceNumber -> {
            openSuccessScreen(amount, adminFee, referenceNumber);

        }).addOnFailureListener(e -> {
            btnSendMoney.setEnabled(true);
            btnSendMoney.setText(getString(R.string.btn_send_money));
            String msg = e.getMessage() != null ? e.getMessage() : "Transfer failed";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    // generates a fake reference and opens success screen without touching Firestore
    private void simulateTransferSuccess(double amount, double adminFee) {
        String fakeReference = Transaction.generateReference();
        openSuccessScreen(amount, adminFee, fakeReference);
    }

    // packs all receipt fields into an intent and starts the success screen
    private void openSuccessScreen(double amount, double adminFee, String referenceNumber) {
        Intent intent = new Intent(this, TransferSuccessActivity.class);
        intent.putExtra("amount",          amount);
        intent.putExtra("adminFee",        adminFee);
        intent.putExtra("total",           amount + adminFee);
        intent.putExtra("referenceNumber", referenceNumber);
        intent.putExtra("recipientName",   selectedRecipientName);
        intent.putExtra("recipientBank",   selectedRecipientBank);
        startActivity(intent);
        finish();
    }

    // returns the uid for the current user respecting bypass mode
    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return sessionManager.getUserId();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }

    // inline adapter that shows saved contacts as small tappable avatar chips
    private static class ContactPickerAdapter
            extends RecyclerView.Adapter<ContactPickerAdapter.VH> {

        // fires when a contact chip is tapped
        interface OnContactClick {
            void onClick(Contact contact);
        }

        private final List<Contact> list;
        private final OnContactClick listener;

        ContactPickerAdapter(List<Contact> list, OnContactClick listener) {
            this.list     = list;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            // build the chip layout programmatically with a circle and a name
            android.widget.LinearLayout ll = new android.widget.LinearLayout(parent.getContext());
            ll.setOrientation(android.widget.LinearLayout.VERTICAL);
            ll.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            ll.setPadding(16, 8, 16, 8);

            // circle view shows two letter initials
            TextView avatar = new TextView(parent.getContext());
            avatar.setTag("avatar");
            int size = (int) (60 * parent.getContext().getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams ap =
                    new android.widget.LinearLayout.LayoutParams(size, size);
            avatar.setLayoutParams(ap);
            avatar.setGravity(android.view.Gravity.CENTER);
            avatar.setTextSize(16);
            avatar.setTextColor(0xFF7B2FBE);
            avatar.setTypeface(null, android.graphics.Typeface.BOLD);
            avatar.setBackground(parent.getContext()
                    .getResources().getDrawable(R.drawable.shape_avatar_circle, null));

            // first name label below the circle
            TextView name = new TextView(parent.getContext());
            name.setTag("name");
            name.setTextSize(11);
            name.setTextColor(0xFF1A1A1A);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            android.widget.LinearLayout.LayoutParams np =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            np.topMargin = 6;
            name.setLayoutParams(np);

            ll.addView(avatar);
            ll.addView(name);
            return new VH(ll);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Contact c       = list.get(position);
            TextView avatar = holder.itemView.findViewWithTag("avatar");
            TextView name   = holder.itemView.findViewWithTag("name");

            avatar.setText(c.getInitials());

            // show only the first word of the name to keep the chip compact
            String[] parts = c.getName().split(" ");
            name.setText(parts[0]);

            holder.itemView.setOnClickListener(v -> listener.onClick(c));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(android.view.View v) { super(v); }
        }
    }
}