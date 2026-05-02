package com.worldbank.app.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.QuickPayAdapter;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.models.User;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class SendMoneyActivity extends AppCompatActivity implements QuickPayAdapter.OnContactClickListener {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PICK_CONTACT_REQUEST = 101;
    private static final String TAG = "SendMoneyActivity";

    private ImageButton ibBack;
    private TextView tvCardNumber, tvCardHolder, tvCardExpiry;
    private TextView tvRecipientName, tvRecipientAccount, tvTransferAmount;
    private TextView btnChange;
    private Button btnSendMoney;
    private EditText etAmount;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private String cardId = "";
    private String accountId = "";

    private String selectedRecipientName = "";
    private String selectedRecipientAccount = "";
    private String selectedRecipientBank = "";
    private String selectedRecipientUid = "";
    private String selectedRecipientAccountId = "";
    private String selectedTransferType = Transaction.TRANSFER_IBFT;

    static final String IBAN_PREFIX = "PK36WBNK";

    // Dialog views — kept as fields so all methods can access them
    private EditText currentDialogAccEdit;
    private EditText currentDialogNameEdit;
    private AutoCompleteTextView currentDialogBankSpinner;
    private TextView currentDialogStatus; // NEW: shows "Verifying…" / "✓ Verified" / "✗ Not found"

    // ── Verification state ──────────────────────────────────────────
    // recipientVerified is set true ONLY when Firestore confirms the account exists.
    // It is cleared ONLY when the account field content changes meaningfully.
    private boolean recipientVerified = false;

    // This flag suppresses the TextWatcher when WE are programmatically filling fields.
    // It does NOT affect recipientVerified — that is managed separately.
    private boolean isProgrammaticSet = false;

    // Snapshot of what was in the account field when the last lookup was launched.
    // Used to detect whether the user actually changed the value.
    private String lastLookedUpValue = "";

    // Debounce
    private final android.os.Handler lookupHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingLookup = null;

    // ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);
// 🔴 --- TEMPORARY DATABASE TEST --- 🔴
        TransactionRepository testRepo = new TransactionRepository();
        testRepo.findAccountByIban("PK36WBNK9001560255952")
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String name = snapshots.getDocuments().get(0).getString("accountTitle");
                        Log.d("DB_TEST", "SUCCESS! Found name: " + name);
                        Toast.makeText(this, "TEST SUCCESS: " + name, Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("DB_TEST", "FAILED! Query returned 0 documents.");
                        Toast.makeText(this, "TEST FAILED: 0 documents", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_TEST", "PERMISSION DENIED OR ERROR", e);
                    Toast.makeText(this, "TEST ERROR: Check Logcat", Toast.LENGTH_LONG).show();
                });
        // 🔴 ------------------------------- 🔴
        Intent intent = getIntent();
        cardId    = intent.getStringExtra("cardId");
        accountId = intent.getStringExtra("accountId");

        repo           = new TransactionRepository();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadCardData();
        setupClickListeners();
        setupAmountWatcher();

        if (intent.hasExtra("recipientAccount")) {
            setRecipient(
                    intent.getStringExtra("recipientName"),
                    intent.getStringExtra("recipientAccount"),
                    intent.getStringExtra("recipientBank"),
                    intent.getStringExtra("recipientUid"),
                    intent.getStringExtra("recipientAccountId"),
                    ""
            );
        }

        promptRecipientIfEmpty();
    }

    private void bindViews() {
        ibBack           = findViewById(R.id.ibBack);
        tvCardNumber     = findViewById(R.id.tvCardNumber);
        tvCardHolder     = findViewById(R.id.tvCardHolder);
        tvCardExpiry     = findViewById(R.id.tvCardExpiry);
        tvRecipientName  = findViewById(R.id.tvRecipientName);
        tvRecipientAccount = findViewById(R.id.tvRecipientAccount);
        tvTransferAmount = findViewById(R.id.tvTransferAmount);
        btnChange        = findViewById(R.id.btnChange);
        btnSendMoney     = findViewById(R.id.btnSendMoney);
        etAmount         = findViewById(R.id.etAmount);
    }

    private void loadCardData() {
        if (cardId == null || cardId.isEmpty()) return;
        repo.getCard(cardId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Card card = doc.toObject(Card.class);
                if (card != null) {
                    tvCardNumber.setText(card.getMaskedNumber());
                    tvCardHolder.setText(card.getHolderName());
                    tvCardExpiry.setText("Exp " + card.getExpiry());
                }
            }
        });
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
        btnChange.setOnClickListener(v -> showRecipientPicker());
        btnSendMoney.setOnClickListener(v -> goToReview());
    }

    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {
                updateAmountDisplay(s.toString());
            }
        });
    }

    private void updateAmountDisplay(String raw) {
        try {
            double amount = Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
            tvTransferAmount.setText(String.format("Rs. %,.0f", amount));
        } catch (Exception e) {
            tvTransferAmount.setText("Rs. 0");
        }
    }

    private void promptRecipientIfEmpty() {
        if (selectedRecipientAccount == null || selectedRecipientAccount.isEmpty()) {
            showRecipientPicker();
        }
    }

    // ── Recipient picker dialog ─────────────────────────────────────

    private void showRecipientPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recipient_picker);
        dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        currentDialogAccEdit     = dialog.findViewById(R.id.etRecipientAccount);
        currentDialogNameEdit    = dialog.findViewById(R.id.etRecipientName);
        currentDialogBankSpinner = dialog.findViewById(R.id.autoCompleteBanks);
        // NOTE: add a TextView with id tvVerificationStatus to dialog_recipient_picker.xml
        // showing e.g. "Verifying…" / "✓ Account verified" / "✗ Account not found"
        // If you haven't added it yet, this reference will just be null and we null-check it.
        currentDialogStatus      = dialog.findViewById(R.id.tvVerificationStatus);

        Button      btnConfirm     = dialog.findViewById(R.id.btnConfirmRecipient);
        ImageButton btnClose       = dialog.findViewById(R.id.btnCloseDialog);
        ImageButton ibPickContact  = dialog.findViewById(R.id.ibPickContact);
        RecyclerView rvContacts    = dialog.findViewById(R.id.rvContacts);

        setupDialogBankDropdown(currentDialogBankSpinner);
        loadContactsIntoDialog(rvContacts);

        // Reset state fresh every time the dialog opens
        resetVerificationState();

        // Pre-fill the IBAN prefix programmatically
        isProgrammaticSet = true;
        currentDialogAccEdit.setText(IBAN_PREFIX);
        currentDialogAccEdit.setSelection(IBAN_PREFIX.length());
        isProgrammaticSet = false;

        currentDialogAccEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // If WE are filling the field, skip — but DON'T touch recipientVerified here.
                // recipientVerified is only changed by lookup results or resetVerificationState().
                if (isProgrammaticSet) return;

                String input = s.toString().trim();

                // Only invalidate if the value actually changed from what we last verified.
                // This prevents the watcher from wiping a successful lookup the moment
                // Firestore returns and we auto-fill the name field (which can re-trigger this).
                if (!input.equals(lastLookedUpValue)) {
                    recipientVerified      = false;
                    selectedRecipientUid       = "";
                    selectedRecipientAccountId = "";
                    showStatus(null); // clear status
                }

                // Cancel previous debounce
                if (pendingLookup != null) {
                    lookupHandler.removeCallbacks(pendingLookup);
                }

                // Fire lookup once user types past the prefix (IBAN_PREFIX.length = 8)
                // This is intentionally low so lookup starts the moment real digits arrive.
                if (input.length() > IBAN_PREFIX.length()) {
                    showStatus("Verifying…");
                    pendingLookup = () -> performRecipientLookup(input, currentDialogNameEdit);
                    lookupHandler.postDelayed(pendingLookup, 600); // 600ms debounce
                } else if (!input.startsWith(IBAN_PREFIX) && input.length() >= 10) {
                    // Phone number path
                    showStatus("Verifying…");
                    pendingLookup = () -> performRecipientLookup(input, currentDialogNameEdit);
                    lookupHandler.postDelayed(pendingLookup, 600);
                }
            }
        });

        ibPickContact.setOnClickListener(v -> checkContactsPermission());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = currentDialogNameEdit.getText().toString().trim();
            String acc  = currentDialogAccEdit.getText().toString().trim();
            String bank = currentDialogBankSpinner.getText().toString().trim();

            if (acc.isEmpty()) {
                Toast.makeText(this, "Enter account number or IBAN", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "Recipient name missing — please wait for verification", Toast.LENGTH_SHORT).show();
                return;
            }

            // For World Bank (internal) transfers, we MUST have verified the account.
            // For other banks (IBFT/JazzCash/EasyPaisa), no in-app lookup is possible —
            // we trust the user-entered details, as real banks do.
            if (Contact.BANK_WORLDBANK.equals(bank) && !recipientVerified) {
                Toast.makeText(this,
                        "Account not verified yet — please wait a moment or re-enter",
                        Toast.LENGTH_LONG).show();
                return;
            }

            setRecipient(name, acc, bank, selectedRecipientUid, selectedRecipientAccountId, "");
            dialog.dismiss();
        });

        dialog.show();
    }

    // ── Core lookup ─────────────────────────────────────────────────

    /**
     * Performs the Firestore lookup for a given input string.
     * IMPORTANT: This method records `input` as `lastLookedUpValue` BEFORE
     * the async call. That way, if the user hasn't typed anything new by the
     * time Firestore returns, we know the result is still valid and we can
     * safely set recipientVerified = true WITHOUT the TextWatcher immediately
     * clearing it again.
     */
    private void performRecipientLookup(String input, EditText nameEdit) {
        // 1. Force uppercase instantly so "pk" matches "PK"
        // 1. Strip all spaces anywhere in the string and force uppercase
        String cleanInput = input.replaceAll("\\s+", "").toUpperCase();

        // 🔴 --- STRING COMPARISON DEBUGGER --- 🔴
        // We only run this debug if you've typed at least 15 characters to avoid spam
        if (cleanInput.startsWith("PK") && cleanInput.length() >= 15) {
            String expected = "PK36WBNK9001560255952";

            Log.d("DEBUG_MATCH", "========================================");
            Log.d("DEBUG_MATCH", "Expected IBAN : [" + expected + "] (Length: " + expected.length() + ")");
            Log.d("DEBUG_MATCH", "Your UI IBAN  : [" + cleanInput + "] (Length: " + cleanInput.length() + ")");
            Log.d("DEBUG_MATCH", "Are they a 100% perfect match? : " + expected.equals(cleanInput));

            if (!expected.equals(cleanInput)) {
                // Find exactly where it went wrong
                int minLen = Math.min(expected.length(), cleanInput.length());
                boolean foundDiff = false;
                for (int i = 0; i < minLen; i++) {
                    if (expected.charAt(i) != cleanInput.charAt(i)) {
                        Log.e("DEBUG_MATCH", "❌ ERROR AT POSITION " + (i+1) + ": Expected '" + expected.charAt(i) + "' but your EditText gave '" + cleanInput.charAt(i) + "'");
                        foundDiff = true;
                        break;
                    }
                }
                if (!foundDiff && expected.length() != cleanInput.length()) {
                    Log.e("DEBUG_MATCH", "❌ ERROR: The characters match, but your UI text is too " + (cleanInput.length() > expected.length() ? "LONG" : "SHORT") + "!");
                }
            } else {
                Log.d("DEBUG_MATCH", "✅ SUCCESS: The strings are absolutely identical!");
            }
            Log.d("DEBUG_MATCH", "========================================");
        }
        // 🔴 ---------------------------------- 🔴

        // Record this as the value we're currently looking up.
        // The TextWatcher will only reset recipientVerified if the value CHANGES from this.
        lastLookedUpValue = cleanInput;

        // Clear status if they delete everything
        if (cleanInput.isEmpty()) {
            showStatus("");
            return;
        }

        if (cleanInput.startsWith(IBAN_PREFIX)) {
            // ── IBAN lookup ─────────────────────────────────────

            // WAIT until they type the full 21-character IBAN before asking Firestore!
            // (Change this to 24 if you use real Pakistani IBANs later)
            if (cleanInput.length() < 21) {
                showStatus("Typing IBAN...");
                recipientVerified = false;
                return; // Stop here! Don't waste a Firebase read.
            }

            repo.findAccountByIban(cleanInput).addOnSuccessListener(snapshots -> {
                // Guard: user may have typed more since we launched. Only apply if still current.
                if (!cleanInput.equals(lastLookedUpValue)) return;

                if (!snapshots.isEmpty()) {
                    Account account = snapshots.getDocuments().get(0).toObject(Account.class);
                    if (account != null && account.isActive()) {
                        selectedRecipientUid       = account.getUid();
                        selectedRecipientAccountId = snapshots.getDocuments().get(0).getId();
                        recipientVerified          = true;

                        // Fill name field programmatically (guard so watcher ignores it)
                        isProgrammaticSet = true;
                        if (nameEdit != null) nameEdit.setText(account.getAccountTitle());
                        if (currentDialogBankSpinner != null)
                            currentDialogBankSpinner.setText(Contact.BANK_WORLDBANK, false);
                        isProgrammaticSet = false;

                        showStatus("✓ Account verified: " + account.getAccountTitle());
                    } else {
                        recipientVerified = false;
                        showStatus("✗ Account inactive or not found");
                    }
                } else {
                    recipientVerified = false;
                    showStatus("✗ No account found for this IBAN");
                }
            }).addOnFailureListener(e -> {
                if (!cleanInput.equals(lastLookedUpValue)) return;
                recipientVerified = false;
                showStatus("✗ Lookup failed — check your connection");
                Log.e(TAG, "IBAN lookup failed", e);
            });

        } else {
            // ── Phone number lookup ─────────────────────────────

            // Wait for them to type at least 11 digits (e.g. 03001234567)
            if (cleanInput.length() < 11) {
                showStatus("Typing Phone Number...");
                recipientVerified = false;
                return; // Wait for them to finish typing!
            }

            repo.findUserByPhone(cleanInput).addOnSuccessListener(userSnaps -> {
                if (!cleanInput.equals(lastLookedUpValue)) return;

                if (!userSnaps.isEmpty()) {
                    String uid = userSnaps.getDocuments().get(0).getId();
                    repo.getAccountQuery(uid).get().addOnSuccessListener(accSnaps -> {
                        if (!cleanInput.equals(lastLookedUpValue)) return;

                        if (!accSnaps.isEmpty()) {
                            Account account = accSnaps.getDocuments().get(0).toObject(Account.class);
                            if (account != null && account.isActive()) {
                                selectedRecipientUid       = uid;
                                selectedRecipientAccountId = accSnaps.getDocuments().get(0).getId();
                                recipientVerified          = true;

                                isProgrammaticSet = true;
                                if (nameEdit != null) nameEdit.setText(account.getAccountTitle());
                                if (currentDialogBankSpinner != null)
                                    currentDialogBankSpinner.setText(Contact.BANK_WORLDBANK, false);
                                isProgrammaticSet = false;

                                showStatus("✓ Account verified: " + account.getAccountTitle());
                            } else {
                                recipientVerified = false;
                                showStatus("✗ Account inactive");
                            }
                        } else {
                            recipientVerified = false;
                            showStatus("✗ No World Bank account linked to this number");
                        }
                    }).addOnFailureListener(e -> {
                        if (!cleanInput.equals(lastLookedUpValue)) return;
                        recipientVerified = false;
                        showStatus("✗ Lookup failed");
                        Log.e(TAG, "Account query failed", e);
                    });
                } else {
                    recipientVerified = false;
                    showStatus("✗ No user found with this phone number");
                }
            }).addOnFailureListener(e -> {
                if (!cleanInput.equals(lastLookedUpValue)) return;
                recipientVerified = false;
                showStatus("✗ Lookup failed — check your connection");
                Log.e(TAG, "Phone lookup failed", e);
            });
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    /** Resets all verification-related state. Call when dialog opens. */
    private void resetVerificationState() {
        recipientVerified      = false;
        selectedRecipientUid       = "";
        selectedRecipientAccountId = "";
        lastLookedUpValue          = "";
        if (pendingLookup != null) {
            lookupHandler.removeCallbacks(pendingLookup);
            pendingLookup = null;
        }
    }

    /** Shows a status message in the dialog (null clears it). Null-safe. */
    private void showStatus(String message) {
        if (currentDialogStatus == null) return;
        if (message == null || message.isEmpty()) {
            currentDialogStatus.setVisibility(View.GONE);
        } else {
            currentDialogStatus.setVisibility(View.VISIBLE);
            currentDialogStatus.setText(message);
        }
    }

    private void setupDialogBankDropdown(AutoCompleteTextView spinner) {
        String[] banks = {
                Contact.BANK_WORLDBANK, Contact.BANK_HBL, Contact.BANK_MEEZAN,
                Contact.BANK_UBL, Contact.BANK_MCB, Contact.BANK_JAZZCASH, Contact.BANK_EASYPAISA
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_black, banks);
        spinner.setAdapter(adapter);
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            pickContact();
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int numIdx  = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    if (numIdx >= 0 && nameIdx >= 0) {
                        String number = cursor.getString(numIdx);
                        String name   = cursor.getString(nameIdx);
                        String clean  = number.replaceAll("[^0-9]", "");
                        if (clean.startsWith("92")) clean = "0" + clean.substring(2);

                        isProgrammaticSet = true;
                        if (currentDialogAccEdit  != null) currentDialogAccEdit.setText(clean);
                        if (currentDialogNameEdit != null) currentDialogNameEdit.setText(name);
                        isProgrammaticSet = false;

                        // Trigger lookup manually (watcher was suppressed)
                        if (clean.length() >= 10) {
                            showStatus("Verifying…");
                            performRecipientLookup(clean, currentDialogNameEdit);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Contact pick failed", e);
            }
        }
    }

    private void loadContactsIntoDialog(RecyclerView rv) {
        String uid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : sessionManager.getUserId();
        if (uid == null || uid.isEmpty() || rv == null) return;

        List<Contact> contacts = new ArrayList<>();
        QuickPayAdapter adapter = new QuickPayAdapter(contacts, this);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);

        repo.getContactsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            contacts.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Contact c = doc.toObject(Contact.class);
                c.setContactId(doc.getId());
                contacts.add(c);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        isProgrammaticSet = true;
        if (currentDialogAccEdit != null) {
            currentDialogAccEdit.setText(contact.getAccountNumber());
            currentDialogAccEdit.setSelection(currentDialogAccEdit.getText().length());
        }
        if (currentDialogNameEdit  != null) currentDialogNameEdit.setText(contact.getName());
        if (currentDialogBankSpinner != null)
            currentDialogBankSpinner.setText(contact.getBankName(), false);
        isProgrammaticSet = false;

        // Always re-verify even for saved contacts — account may have become inactive
        showStatus("Verifying…");
        performRecipientLookup(contact.getAccountNumber(), currentDialogNameEdit);
    }

    public void setRecipient(String name, String account, String bank,
                             String uid, String accId, String contactId) {
        selectedRecipientName      = name      != null ? name    : "";
        selectedRecipientAccount   = account   != null ? account : "";
        selectedRecipientBank      = bank      != null ? bank    : "";
        selectedRecipientUid       = uid       != null ? uid     : "";
        selectedRecipientAccountId = accId     != null ? accId   : "";

        tvRecipientName.setText(selectedRecipientName);
        tvRecipientAccount.setText(selectedRecipientAccount);

        if      (Contact.BANK_WORLDBANK.equals(bank))   selectedTransferType = Transaction.TRANSFER_INTERNAL;
        else if (Contact.BANK_JAZZCASH.equals(bank))    selectedTransferType = Transaction.TRANSFER_JAZZCASH;
        else if (Contact.BANK_EASYPAISA.equals(bank))   selectedTransferType = Transaction.TRANSFER_EASYPAISA;
        else                                             selectedTransferType = Transaction.TRANSFER_IBFT;
    }

    private void goToReview() {
        if (selectedRecipientAccount.isEmpty()) {
            Toast.makeText(this, "Please add a recipient first", Toast.LENGTH_SHORT).show();
            return;
        }
        String amtStr = etAmount.getText().toString().trim();
        if (amtStr.isEmpty()) {
            etAmount.setError("Enter amount");
            return;
        }

        double amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        double fee = TransactionRepository.getAdminFee(selectedTransferType);

        Intent intent = new Intent(this, ReviewPaymentActivity.class);
        intent.putExtra("recipientName",      selectedRecipientName);
        intent.putExtra("recipientAccount",   selectedRecipientAccount);
        intent.putExtra("recipientBank",      selectedRecipientBank);
        intent.putExtra("recipientUid",       selectedRecipientUid);
        intent.putExtra("recipientAccountId", selectedRecipientAccountId);
        intent.putExtra("amount",             amount);
        intent.putExtra("fee",                fee);
        intent.putExtra("transferType",       selectedTransferType);
        intent.putExtra("cardId",             cardId);
        intent.putExtra("accountId",          accountId);
        startActivity(intent);
    }
}