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
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class SendMoneyActivity extends AppCompatActivity implements QuickPayAdapter.OnContactClickListener {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PICK_CONTACT_REQUEST = 101;

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
    
    private EditText currentDialogAccEdit, currentDialogNameEdit;
    private AutoCompleteTextView currentDialogBankSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        Intent intent = getIntent();
        cardId = intent.getStringExtra("cardId");
        accountId = intent.getStringExtra("accountId");

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadCardData();
        setupClickListeners();
        setupAmountWatcher();

        // Handle pre-filled recipient from Quick Pay
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
        ibBack = findViewById(R.id.ibBack);
        tvCardNumber = findViewById(R.id.tvCardNumber);
        tvCardHolder = findViewById(R.id.tvCardHolder);
        tvCardExpiry = findViewById(R.id.tvCardExpiry);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvRecipientAccount = findViewById(R.id.tvRecipientAccount);
        tvTransferAmount = findViewById(R.id.tvTransferAmount);
        btnChange = findViewById(R.id.btnChange);
        btnSendMoney = findViewById(R.id.btnSendMoney);
        etAmount = findViewById(R.id.etAmount);
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
            @Override
            public void afterTextChanged(Editable s) {
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
        if (selectedRecipientAccount.isEmpty()) showRecipientPicker();
    }

    private void showRecipientPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recipient_picker);
        dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        currentDialogAccEdit = dialog.findViewById(R.id.etRecipientAccount);
        currentDialogNameEdit = dialog.findViewById(R.id.etRecipientName);
        currentDialogBankSpinner = dialog.findViewById(R.id.autoCompleteBanks);
        
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmRecipient);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseDialog);
        ImageButton ibPickContact = dialog.findViewById(R.id.ibPickContact);
        RecyclerView rvContacts = dialog.findViewById(R.id.rvContacts);

        setupDialogBankDropdown(currentDialogBankSpinner);
        loadContactsIntoDialog(rvContacts);

        currentDialogAccEdit.setText(IBAN_PREFIX);
        currentDialogAccEdit.setSelection(IBAN_PREFIX.length());

        currentDialogAccEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                String iban = s.toString().trim();
                if (iban.length() == 24 && iban.startsWith(IBAN_PREFIX)) {
                    currentDialogNameEdit.setHint("Searching...");
                    repo.findAccountByIban(iban).addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            String name = snapshots.getDocuments().get(0).getString("accountTitle");
                            selectedRecipientUid = snapshots.getDocuments().get(0).getString("uid");
                            selectedRecipientAccountId = snapshots.getDocuments().get(0).getId();
                            currentDialogNameEdit.setText(name);
                            currentDialogBankSpinner.setText(Contact.BANK_WORLDBANK, false);
                        }
                    });
                }
            }
        });

        ibPickContact.setOnClickListener(v -> checkContactsPermission());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = currentDialogNameEdit.getText().toString().trim();
            String acc = currentDialogAccEdit.getText().toString().trim();
            String bank = currentDialogBankSpinner.getText().toString().trim();
            
            if (name.isEmpty() || acc.isEmpty()) {
                Toast.makeText(this, "Please enter recipient details", Toast.LENGTH_SHORT).show();
                return;
            }
            
            setRecipient(name, acc, bank, selectedRecipientUid, selectedRecipientAccountId, "");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupDialogBankDropdown(AutoCompleteTextView spinner) {
        String[] banks = {
                Contact.BANK_WORLDBANK, Contact.BANK_HBL, Contact.BANK_MEEZAN, 
                Contact.BANK_UBL, Contact.BANK_MCB, Contact.BANK_JAZZCASH, Contact.BANK_EASYPAISA
        };
        // FIXED: Using custom high-contrast dropdown layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_black, banks);
        spinner.setAdapter(adapter);
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            pickContact();
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact();
            } else {
                Toast.makeText(this, "Permission denied to access contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};

            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    String number = cursor.getString(numberIndex);
                    String name = cursor.getString(nameIndex);

                    String cleanNumber = number.replaceAll("[^0-9]", "");
                    if (cleanNumber.startsWith("92")) cleanNumber = "0" + cleanNumber.substring(2);
                    else if (!cleanNumber.startsWith("0")) cleanNumber = "0" + cleanNumber;

                    if (currentDialogAccEdit != null) currentDialogAccEdit.setText(cleanNumber);
                    if (currentDialogNameEdit != null) currentDialogNameEdit.setText(name);
                    if (currentDialogBankSpinner != null) currentDialogBankSpinner.setText(Contact.BANK_JAZZCASH, false);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void loadContactsIntoDialog(RecyclerView rv) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty() || rv == null) return;

        List<Contact> contacts = new ArrayList<>();
        QuickPayAdapter adapter = new QuickPayAdapter(contacts, this);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);

        repo.getContactsQuery(uid).get().addOnSuccessListener(snapshots -> {
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
        if (currentDialogAccEdit != null) {
            currentDialogAccEdit.setText(contact.getAccountNumber());
            currentDialogAccEdit.setSelection(currentDialogAccEdit.getText().length());
        }
        if (currentDialogNameEdit != null) {
            currentDialogNameEdit.setText(contact.getName());
        }
        if (currentDialogBankSpinner != null) {
            currentDialogBankSpinner.setText(contact.getBankName(), false);
        }
    }

    public void setRecipient(String name, String account, String bank, String uid, String accId, String contactId) {
        selectedRecipientName = name;
        selectedRecipientAccount = account;
        selectedRecipientBank = bank;
        selectedRecipientUid = uid;
        selectedRecipientAccountId = accId;
        
        tvRecipientName.setText(name);
        tvRecipientAccount.setText(account);

        if (Contact.BANK_WORLDBANK.equals(bank)) selectedTransferType = Transaction.TRANSFER_INTERNAL;
        else if (Contact.BANK_JAZZCASH.equals(bank)) selectedTransferType = Transaction.TRANSFER_JAZZCASH;
        else if (Contact.BANK_EASYPAISA.equals(bank)) selectedTransferType = Transaction.TRANSFER_EASYPAISA;
        else selectedTransferType = Transaction.TRANSFER_IBFT;
    }

    private void goToReview() {
        String amtStr = etAmount.getText().toString().trim();
        if (amtStr.isEmpty() || selectedRecipientAccount.isEmpty()) {
            Toast.makeText(this, "Enter amount and recipient", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
        double fee = TransactionRepository.getAdminFee(selectedTransferType);

        Intent intent = new Intent(this, ReviewPaymentActivity.class);
        intent.putExtra("recipientName", selectedRecipientName);
        intent.putExtra("recipientAccount", selectedRecipientAccount);
        intent.putExtra("recipientBank", selectedRecipientBank);
        intent.putExtra("recipientUid", selectedRecipientUid);
        intent.putExtra("recipientAccountId", selectedRecipientAccountId);
        intent.putExtra("amount", amount);
        intent.putExtra("fee", fee);
        intent.putExtra("transferType", selectedTransferType);
        intent.putExtra("cardId", cardId);
        intent.putExtra("accountId", accountId);
        startActivity(intent);
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
