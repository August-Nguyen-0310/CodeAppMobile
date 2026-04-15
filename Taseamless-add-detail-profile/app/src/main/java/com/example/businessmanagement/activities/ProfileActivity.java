package com.example.businessmanagement.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.businessmanagement.R;
import com.example.businessmanagement.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfileAvatar, btnBackProfile;
    private TextView txtProfileName, txtProfileEmail, txtProfileRole, txtManagedBy, txtChangeAvatar, lblManagedBy;
    private TextView txtProjectCount, txtProfileHeaderTitle;
    private Button btnLogout, btnMessageUser;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUid;
    private String targetUserId; // The user we are viewing
    private User targetUser;
    private boolean isSelf = true;

    private static final String SUPABASE_URL = "https://gjvfpoaslaidwprhwpvh.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdqdmZwb2FzbGFpZHdwcmh3cHZoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU5ODM1NTYsImV4cCI6MjA5MTU1OTU1Nn0.QHpN5mpSdCggEm5ef43ZzuDLxOjsY7m-C_Qi1I8S2E4";
    private static final String BUCKET_NAME = "storage";
    private final OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadAvatar(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        targetUserId = getIntent().getStringExtra("USER_ID");
        if (targetUserId == null || targetUserId.equals(currentUid)) {
            targetUserId = currentUid;
            isSelf = true;
        } else {
            isSelf = false;
        }

        initViews();
        setupUI();
        loadUserProfile();
        fetchProjectCount();

        btnBackProfile.setOnClickListener(v -> finish());

        if (isSelf) {
            txtChangeAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                imagePicker.launch(intent);
            });
            btnLogout.setOnClickListener(v -> logout());
        } else {
            btnMessageUser.setOnClickListener(v -> startPrivateChat());
        }
    }

    private void initViews() {
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        txtProfileName = findViewById(R.id.txtProfileName);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        txtProfileRole = findViewById(R.id.txtProfileRole);
        txtManagedBy = findViewById(R.id.txtManagedBy);
        txtChangeAvatar = findViewById(R.id.txtChangeAvatar);
        btnLogout = findViewById(R.id.btnLogout);
        lblManagedBy = findViewById(R.id.lblManagedBy);
        txtProjectCount = findViewById(R.id.txtProjectCount);
        txtProfileHeaderTitle = findViewById(R.id.txtProfileHeaderTitle);
        btnMessageUser = findViewById(R.id.btnMessageUser);
    }

    private void setupUI() {
        if (isSelf) {
            txtProfileHeaderTitle.setText("Profile Detail");
            txtChangeAvatar.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnMessageUser.setVisibility(View.GONE);
        } else {
            txtProfileHeaderTitle.setText("User Info");
            txtChangeAvatar.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            btnMessageUser.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserProfile() {
        if (targetUserId == null) return;

        db.collection("Users").document(targetUserId).get().addOnSuccessListener(documentSnapshot -> {
            targetUser = documentSnapshot.toObject(User.class);
            if (targetUser != null) {
                txtProfileName.setText(targetUser.getFullName());
                txtProfileEmail.setText(targetUser.getEmail());
                txtProfileRole.setText(targetUser.getRole());

                if (targetUser.getProfileImageUrl() != null && !targetUser.getProfileImageUrl().isEmpty()) {
                    Glide.with(this).load(targetUser.getProfileImageUrl()).placeholder(R.drawable.ic_profile).into(imgProfileAvatar);
                }

                if (targetUser.getManagerId() != null && !targetUser.getManagerId().isEmpty()) {
                    lblManagedBy.setVisibility(View.VISIBLE);
                    txtManagedBy.setVisibility(View.VISIBLE);
                    fetchManagerName(targetUser.getManagerId());
                } else {
                    lblManagedBy.setVisibility(View.GONE);
                    txtManagedBy.setVisibility(View.GONE);
                }
            }
        });
    }

    private void fetchManagerName(String managerId) {
        db.collection("Users").document(managerId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                txtManagedBy.setText(doc.getString("fullName"));
            } else {
                txtManagedBy.setText("Unknown Manager");
            }
        });
    }

    private void fetchProjectCount() {
        db.collection("Projects").whereArrayContains("memberIds", targetUserId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (txtProjectCount != null) {
                        txtProjectCount.setText(String.valueOf(count));
                    }
                });
    }

    private void startPrivateChat() {
        if (targetUser == null) return;
        
        // Private chat logic: roomId is often combination of sorted UIDs
        String roomId;
        if (currentUid.compareTo(targetUserId) < 0) roomId = currentUid + "_" + targetUserId;
        else roomId = targetUserId + "_" + currentUid;

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, roomId);
        intent.putExtra(ChatActivity.EXTRA_PROJECT_NAME, "Private Chat with " + targetUser.getFullName());
        startActivity(intent);
    }

    private void uploadAvatar(Uri uri) {
        if (!isSelf) return;
        Toast.makeText(this, "Uploading avatar...", Toast.LENGTH_SHORT).show();
        String fileName = "avatar_" + currentUid + "_" + UUID.randomUUID() + ".jpg";
        String storagePath = "avatars/" + fileName;

        new Thread(() -> {
            try {
                RequestBody requestBody = new RequestBody() {
                    @Override public MediaType contentType() { return MediaType.parse("image/jpeg"); }
                    @Override public void writeTo(@NonNull BufferedSink sink) throws IOException {
                        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                            if (in == null) return;
                            Source source = Okio.source(in);
                            sink.writeAll(source);
                        }
                    }
                };

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + storagePath)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + storagePath;
                        updateProfileImageUrl(publicUrl);
                    } else {
                        throw new IOException("Failed to upload: " + response.message());
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void updateProfileImageUrl(String url) {
        db.collection("Users").document(currentUid).update("profileImageUrl", url)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        Glide.with(this).load(url).into(imgProfileAvatar);
                        Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(this, "Update Firestore failed", Toast.LENGTH_SHORT).show()));
    }

    private void logout() {
        mAuth.signOut();
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().clear().apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
