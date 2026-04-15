package com.example.businessmanagement.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.businessmanagement.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText login_email, login_password;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        login_email = findViewById(R.id.edtUsername);
        login_password = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // TODO: Xóa code cứng này sau khi test xong
        login_email.setText("hunh.hoilinh1993@gmail.com");
        login_password.setText("123456");

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = login_email.getText().toString();
                String password = login_password.getText().toString();

                if(email.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            String uid = authResult.getUser().getUid();

                            db.collection("Users").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if(documentSnapshot.exists()){
                                        String role = documentSnapshot.getString("role");
                                        String name = documentSnapshot.getString("fullName");

                                        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        pref.edit()
                                            .putString("user_role", role)
                                            .putString("user_name", name)
                                            .apply();

                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                        } else{
                                            Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                                    });

                    }).addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this, "Please check your email or password and try again!", Toast.LENGTH_SHORT).show();
                            login_email.setText("");
                            login_password.setText("");
                    });
            }
        });
    }
}
