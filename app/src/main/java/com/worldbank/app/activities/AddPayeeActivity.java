package com.worldbank.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.worldbank.app.R;
import com.worldbank.app.models.Contact;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

public class AddPayeeActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PICK_CONTACT_REQUEST = 101;

    private EditText etPayeeName, etPayeeAccount;
    private AutoCompleteTextView autoCompleteBanks;
    private Button btnSavePayee;
    private ImageButton ibBack, ibPickContact;
    
    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payee);

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupBankDropdown();
        setupClickListeners();
    }

    private void bindViews() {
        etPayeeName = findViewById(R.id.etPayeeName);
        etPayeeAccount = findViewById(R.id.etPayeeAccount);
        autoCompleteBanks = findViewById(R.id.autoCompleteBanks);
        btnSavePayee = findViewById(R.id.btnSavePayee);
        ibBack = findViewById(R.id.ibBack);
        ibPickContact = findViewById(R.id.ibPickContact);
    }

    private void setupBankDropdown() {
        String[] banks = {
                Contact.BANK_WORLDBANK, Contact.BANK_HBL, Contact.BANK_MEEZAN, 
                Contact.BANK_UBL, Contact.BANK_MCB, Contact.BANK_ALLIED, 
                Contact.BANK_ASKARI, Contact.BANK_JAZZCASH, Contact.BANK_EASYPAISA
        };

        // FIXED: Using custom item_dropdown_black for high contrast visibility
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                R.layout.item_dropdown_black, banks);
        autoCompleteBanks.setAdapter(adapter);
        
        autoCompleteBanks.setText(Contact.BANK_WORLDBANK, false);
    }

    private void setupClickListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
        ibPickContact.setOnClickListener(v -> checkContactsPermission());
        btnSavePayee.setOnClickListener(v -> savePayee());
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
                Toast.makeText(this, "Permission denied. Please enable in settings.", Toast.LENGTH_SHORT).show();
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

                    etPayeeAccount.setText(cleanNumber);
                    etPayeeName.setText(name);
                    autoCompleteBanks.setText(Contact.BANK_JAZZCASH, false);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load contact", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePayee() {
        String name = etPayeeName.getText().toString().trim();
        String account = etPayeeAccount.getText().toString().trim();
        String bank = autoCompleteBanks.getText().toString().trim();

        if (name.isEmpty() || account.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePayee.setEnabled(false);
        btnSavePayee.setText("Saving...");

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        Contact contact = new Contact(uid, name, account, bank);

        repo.saveContact(contact).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Payee saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            btnSavePayee.setEnabled(true);
            btnSavePayee.setText("Save Payee");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
