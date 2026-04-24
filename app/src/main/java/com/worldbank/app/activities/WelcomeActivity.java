package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

/**
 * WelcomeActivity
 * TODO: Implement UI and logic based on project documentation.
 */
public class WelcomeActivity extends AppCompatActivity {
    Button btn_login;
    Button btn_signUp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        init();
    }
    public void init(){
        btn_login=findViewById(R.id.btn_login);
        btn_signUp=findViewById(R.id.btn_signup);
        btn_login.setOnClickListener(v->{
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);

        });
        btn_signUp.setOnClickListener(v->
                {
            Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
            startActivity(intent);
        });


    }
}
