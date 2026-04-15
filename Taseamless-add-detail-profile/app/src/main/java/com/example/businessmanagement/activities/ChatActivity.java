package com.example.businessmanagement.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.MessageAdapter;
import com.example.businessmanagement.models.Chat;
import com.example.businessmanagement.models.Message;
import com.example.businessmanagement.models.Project;
import com.example.businessmanagement.models.Task;
import com.example.businessmanagement.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    // ──────── Intent Keys ────────
    public static final String EXTRA_CHAT_ID      = "chatId";
    public static final String EXTRA_TASK_ID      = "taskId";
    public static final String EXTRA_TASK_NAME    = "taskName";
    public static final String EXTRA_PROJECT_NAME = "projectName";

    private static final int REQUEST_AUDIO_PERMISSION = 101;
    private static final long MAX_FILE_SIZE_BYTES     = 100L * 1024 * 1024; // 100 MB

    // ──────── Views ────────
    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private ImageView     btnSend, btnMic, btnAttach, btnBack;
    private LinearLayout  llRecording, llUploadProgress;
    private TextView      txtRecordTimer, txtUploadPercent, txtChatTaskName, txtChatProjectName;
    private ProgressBar   uploadProgressBar;
    private ImageView     imgUploadPreview;
    private LinearLayout  llChatHeaderInfo;

    private View          llReplyPreview;
    private TextView      txtReplyPreviewSender, txtReplyPreviewContent;
    private ImageView     btnCloseReply;
    private Message       replyingToMessage = null;

    private View          llPinnedBanner;
    private TextView      txtPinnedBannerContent;
    private ImageView     btnViewAllPinned;

    // ──────── Mentions Data ────────
    private RecyclerView rvMentions;
    private MentionAdapter mentionAdapter;
    private final List<User> availableUsers = new ArrayList<>();
    private final List<User> mentionResults = new ArrayList<>();
    private int mentionStartIndex = -1;

    // ──────── Data ────────
    private MessageAdapter          adapter;
    private final List<Message>     messageList = new ArrayList<>();
    private FirebaseFirestore       db;
    
    // ──────── Supabase Config ────────
    private static final String SUPABASE_URL = "https://gjvfpoaslaidwprhwpvh.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdqdmZwb2FzbGFpZHdwcmh3cHZoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU5ODM1NTYsImV4cCI6MjA5MTU1OTU1Nn0.QHpN5mpSdCggEm5ef43ZzuDLxOjsY7m-C_Qi1I8S2E4";
    private static final String BUCKET_NAME = "storage";
    
    private final OkHttpClient      httpClient = new OkHttpClient();
    private ListenerRegistration    listenerReg;
    private String currentUid, currentName, currentProfileImageUrl;
    private String chatId, taskId, taskName, projectName;

    // ──────── Recording ────────
    private MediaRecorder recorder;
    private File          audioFile;
    private long          recordStartTime;
    private boolean       isRecording = false;
    private Handler       recordTimer = new Handler(Looper.getMainLooper());
    private Runnable      recordTimerRunnable;

    // ──────── File Picker ────────
    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) handleFilePicked(fileUri);
                }
            });

    // ──────── onCreate ────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db      = FirebaseFirestore.getInstance();

        // Read extras
        chatId      = getIntent().getStringExtra(EXTRA_CHAT_ID);
        taskId      = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskName    = getIntent().getStringExtra(EXTRA_TASK_NAME);
        projectName = getIntent().getStringExtra(EXTRA_PROJECT_NAME);

        // If chatId not provided, use taskId as chatId
        if (chatId == null) chatId = taskId;

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUid  = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String savedRole = pref.getString("user_role", "");
        String savedName = pref.getString("user_name", "User");
        currentName = (savedRole.isEmpty()) ? savedName : savedName + " - " + savedRole;
        
        // Fetch current user details for avatar
        fetchCurrentUserInfo();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        listenForMessages();
    }

    private void fetchCurrentUserInfo() {
        if (currentUid.isEmpty()) return;
        db.collection("Users").document(currentUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");
            }
        });
    }

    private void initViews() {
        rvMessages          = findViewById(R.id.rvMessages);
        etMessage           = findViewById(R.id.etMessage);
        btnSend             = findViewById(R.id.btnSend);
        btnMic              = findViewById(R.id.btnMic);
        btnAttach           = findViewById(R.id.btnAttach);
        btnBack             = findViewById(R.id.btnBack);
        llRecording         = findViewById(R.id.llRecording);
        llUploadProgress    = findViewById(R.id.llUploadProgress);
        txtRecordTimer      = findViewById(R.id.txtRecordTimer);
        txtUploadPercent    = findViewById(R.id.txtUploadPercent);
        txtChatTaskName     = findViewById(R.id.txtChatTaskName);
        txtChatProjectName  = findViewById(R.id.txtChatProjectName);
        uploadProgressBar   = findViewById(R.id.uploadProgressBar);
        imgUploadPreview    = findViewById(R.id.imgUploadPreview);
        
        llChatHeaderInfo    = (LinearLayout) txtChatTaskName.getParent();

        llReplyPreview         = findViewById(R.id.llReplyPreview);
        txtReplyPreviewSender  = findViewById(R.id.txtReplyPreviewSender);
        txtReplyPreviewContent = findViewById(R.id.txtReplyPreviewContent);
        btnCloseReply          = findViewById(R.id.btnCloseReply);

        llPinnedBanner         = findViewById(R.id.llPinnedBanner);
        txtPinnedBannerContent = findViewById(R.id.txtPinnedBannerContent);
        btnViewAllPinned       = findViewById(R.id.btnViewAllPinned);

        if (btnCloseReply != null) {
            btnCloseReply.setOnClickListener(v -> closeReplyPreview());
        }

        rvMentions = findViewById(R.id.rvMentions);
        rvMentions.setLayoutManager(new LinearLayoutManager(this));
        mentionAdapter = new MentionAdapter();
        rvMentions.setAdapter(mentionAdapter);

        fetchAvailableUsers();
        setupMentionTextWatcher();

        if (!TextUtils.isEmpty(taskName)) {
            fetchTaskNumberAndSetTitle();
        } else if (!TextUtils.isEmpty(projectName)) {
            txtChatTaskName.setText(projectName);
            txtChatProjectName.setVisibility(View.GONE);
        }
    }

    private void fetchTaskNumberAndSetTitle() {
        if (taskId == null || projectName == null) {
            txtChatTaskName.setText(taskName);
            txtChatProjectName.setText(projectName);
            return;
        }

        db.collection("Tasks")
                .whereEqualTo("projectId", chatId.split("_")[0])
                .orderBy("deadline", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int taskIndex = 1;
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        if (queryDocumentSnapshots.getDocuments().get(i).getId().equals(taskId)) {
                            taskIndex = i + 1;
                            break;
                        }
                    }
                    txtChatTaskName.setText("Task " + taskIndex + ": " + taskName);
                    txtChatProjectName.setText(projectName);
                    txtChatProjectName.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    txtChatTaskName.setText(taskName);
                    txtChatProjectName.setText(projectName);
                });
    }

    private void setupRecyclerView() {
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        adapter = new MessageAdapter(this, messageList);
        adapter.setOnMessageActionClickListener(new MessageAdapter.OnMessageActionClickListener() {
            @Override
            public void onReplyClick(Message msg) {
                showReplyPreview(msg);
            }
            @Override
            public void onPinClick(Message msg) {
                togglePinMessage(msg);
            }
            @Override
            public void onDeleteClick(Message msg) {
                deleteMessage(msg);
            }
            @Override
            public void onLongClick(Message msg) {
                showMessageOptionsDialog(msg);
            }
        });
        rvMessages.setAdapter(adapter);
    }

    private void showReplyPreview(Message msg) {
        replyingToMessage = msg;
        llReplyPreview.setVisibility(View.VISIBLE);
        txtReplyPreviewSender.setText("Trả lời: " + msg.getSenderName());
        txtReplyPreviewContent.setText(Message.TYPE_TEXT.equals(msg.getMessageType()) ? msg.getContent() : "Đính kèm");
        etMessage.requestFocus();
    }

    private void showMessageOptionsDialog(Message msg) {
        boolean isMyMessage = msg.getSenderId() != null && msg.getSenderId().equals(currentUid);
        String pinOption = msg.isPinned() ? "📌 Bỏ ghim" : "📌 Ghim";

        // Build options: everyone can pin; only sender can delete
        java.util.List<String> optionList = new java.util.ArrayList<>();
        optionList.add("↩ Trả lời");
        optionList.add(pinOption);
        if (isMyMessage) {
            optionList.add("🗑 Xóa");
        }
        String[] options = optionList.toArray(new String[0]);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Tùy chọn tin nhắn")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (selected.equals("↩ Trả lời")) {
                        showReplyPreview(msg);
                    } else if (selected.equals("📌 Ghim") || selected.equals("📌 Bỏ ghim")) {
                        togglePinMessage(msg);
                    } else if (selected.equals("🗑 Xóa")) {
                        deleteMessage(msg);
                    }
                })
                .show();
    }

    private void deleteMessage(Message msg) {
        if (chatId == null || msg.getId() == null) return;

        String typeLabel = Message.TYPE_IMAGE.equals(msg.getMessageType())
                ? "hình ảnh" : "tin nhắn";

        new android.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + typeLabel + " này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("Chats").document(chatId)
                            .collection("Messages").document(msg.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                                    "Đã xóa " + typeLabel, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void closeReplyPreview() {
        replyingToMessage = null;
        llReplyPreview.setVisibility(View.GONE);
    }

    private void togglePinMessage(Message msg) {
        if (chatId == null || msg.getId() == null) return;
        boolean newPinStatus = !msg.isPinned();
        db.collection("Chats").document(chatId)
            .collection("Messages").document(msg.getId())
            .update("pinned", newPinStatus)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, newPinStatus ? "Đã ghim tin nhắn" : "Đã bỏ ghim", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (llChatHeaderInfo != null) {
            llChatHeaderInfo.setOnClickListener(v -> showChatDetailDialog());
        }

        btnSend.setOnClickListener(v -> sendTextMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendTextMessage();
            return true;
        });

        btnAttach.setOnClickListener(v -> openFilePicker());

        btnMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    requestAudioPermissionAndRecord();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isRecording) stopRecordingAndSend();
                    return true;
            }
            return false;
        });
    }

    private void showChatDetailDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_chat_detail, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvCode = view.findViewById(R.id.tvDetailAccessCode);
        RecyclerView rvMembers = view.findViewById(R.id.rvDetailMembers);
        Button btnClose = view.findViewById(R.id.btnCloseDetail);
        ImageView btnCopy = view.findViewById(R.id.btnCopyCode);

        tvTitle.setText(!TextUtils.isEmpty(taskName) ? taskName : projectName);

        db.collection("Chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Chat chat = doc.toObject(Chat.class);
                if (chat != null) {
                    String code = chat.getAccessCode();
                    tvCode.setText(TextUtils.isEmpty(code) ? "Chưa có mã" : code);
                    if (!TextUtils.isEmpty(code)) {
                        btnCopy.setVisibility(View.VISIBLE);
                        btnCopy.setOnClickListener(v -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Access Code", code);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, "Đã sao chép mã", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });

        // Tải danh sách thành viên linh hoạt cho Task hoặc Project
        if (!TextUtils.isEmpty(taskId)) {
            // Trường hợp 1: Nhóm chat của một Task cụ thể
            db.collection("Tasks").document(taskId).get().addOnSuccessListener(taskDoc -> {
                if (taskDoc.exists()) {
                    Task task = taskDoc.toObject(Task.class);
                    if (task != null && task.getAssignedMembers() != null) {
                        loadMembersList(task.getAssignedMembers(), rvMembers);
                    }
                }
            });
        } else {
            // Trường hợp 2: Nhóm chat chung của Project
            String pId = "";
            if (chatId != null && chatId.contains("_")) {
                pId = chatId.split("_")[0];
            } else if (chatId != null) {
                pId = chatId;
            }

            if (!pId.isEmpty()) {
                db.collection("Projects").document(pId).get().addOnSuccessListener(projectDoc -> {
                    if (projectDoc.exists()) {
                        Project project = projectDoc.toObject(Project.class);
                        if (project != null) {
                            Set<String> allMemberIds = new HashSet<>();
                            if (project.getLeadId() != null) allMemberIds.add(project.getLeadId());
                            if (project.getManagerIds() != null) allMemberIds.addAll(project.getManagerIds());
                            if (project.getMemberIds() != null) allMemberIds.addAll(project.getMemberIds());
                            
                            loadMembersList(new ArrayList<>(allMemberIds), rvMembers);
                        }
                    }
                });
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadMembersList(List<String> memberIds, RecyclerView rv) {
        List<User> members = new ArrayList<>();
        MemberAdapter memberAdapter = new MemberAdapter(members);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(memberAdapter);

        for (String uid : memberIds) {
            db.collection("Users").document(uid).get().addOnSuccessListener(uDoc -> {
                if (uDoc.exists()) {
                    User u = uDoc.toObject(User.class);
                    if (u != null) {
                        members.add(u);
                        memberAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
        private List<User> users;
        MemberAdapter(List<User> users) { this.users = users; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.item_member, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            User u = users.get(p);
            h.txtName.setText(u.getFullName());
            h.txtRole.setText(u.getRole());
            
            if (u.getProfileImageUrl() != null && !u.getProfileImageUrl().isEmpty()) {
                Glide.with(ChatActivity.this)
                        .load(u.getProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .into(h.imgMember);
            } else {
                h.imgMember.setImageResource(R.drawable.ic_profile);
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", u.getUid());
                startActivity(intent);
            });
        }
        @Override public int getItemCount() { return users.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtRole;
            ImageView imgMember;
            ViewHolder(View v) { 
                super(v); 
                txtName = v.findViewById(R.id.txtMemberName); 
                txtRole = v.findViewById(R.id.txtMemberRole);
                imgMember = v.findViewById(R.id.imgMember);
            }
        }
    }

    // ──────── TEXT MESSAGE ────────

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etMessage.setText("");

        String msgId = UUID.randomUUID().toString();
        Message msg = new Message(msgId, chatId, taskId, currentUid, currentName, currentProfileImageUrl, text,
                System.currentTimeMillis());

        if (replyingToMessage != null) {
            msg.setReplyToMessageId(replyingToMessage.getId());
            msg.setReplyToSenderName(replyingToMessage.getSenderName());
            msg.setReplyToContent(Message.TYPE_TEXT.equals(replyingToMessage.getMessageType()) ? replyingToMessage.getContent() : "Đính kèm");
            closeReplyPreview();
        }

        saveMessage(msg);
    }

    // ──────── VOICE RECORDING ────────

    private void requestAudioPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            Toast.makeText(this, "Cần quyền ghi âm để gửi tin nhắn thoại", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        try {
            audioFile = new File(getCacheDir(), "voice_" + System.currentTimeMillis() + ".3gp");
            recorder  = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recordStartTime = System.currentTimeMillis();
            isRecording     = true;

            llRecording.setVisibility(View.VISIBLE);
            startRecordTimer();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecordTimer() {
        recordTimerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - recordStartTime;
                long min = elapsed / 60000;
                long sec = (elapsed % 60000) / 1000;
                txtRecordTimer.setText(String.format("  %d:%02d", min, sec));
                recordTimer.postDelayed(this, 500);
            }
        };
        recordTimer.post(recordTimerRunnable);
    }

    private void stopRecordingAndSend() {
        if (!isRecording || recorder == null) return;
        isRecording = false;
        long duration = System.currentTimeMillis() - recordStartTime;

        try {
            recorder.stop();
        } catch (RuntimeException ignored) {
        }
        recorder.release();
        recorder = null;
        llRecording.setVisibility(View.GONE);
        if (recordTimerRunnable != null) recordTimer.removeCallbacks(recordTimerRunnable);

        if (duration < 500) {
            Toast.makeText(this, "Tin nhắn thoại quá ngắn", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadFile(Uri.fromFile(audioFile), audioFile.getName(),
                audioFile.length(), Message.TYPE_VOICE, duration,
                "voices/" + chatId + "/" + UUID.randomUUID() + ".3gp");
    }

    // ──────── FILE ATTACH ────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(Intent.createChooser(intent, "Chọn file"));
    }

    private void handleFilePicked(Uri uri) {
        String fileName = getFileName(uri);
        long   fileSize = getFileSize(uri);

        if (fileSize > MAX_FILE_SIZE_BYTES) {
            Toast.makeText(this, "File vượt quá 100MB, vui lòng chọn file nhỏ hơn", Toast.LENGTH_LONG).show();
            return;
        }

        String type = Message.TYPE_FILE;
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") 
                    || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
                type = Message.TYPE_IMAGE;
            }
        }

        String storagePath = "files/" + chatId + "/" + UUID.randomUUID() + "_" + fileName;
        uploadFile(uri, fileName, fileSize, type, 0, storagePath);
    }

    // ──────── UPLOAD ────────

    private void uploadFile(Uri localUri, String fileName, long fileSize,
                            String type, long audioDuration, String storagePath) {
        showUploadProgress(50, localUri, type); 
        
        new Thread(() -> {
            try {
                RequestBody requestBody = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("application/octet-stream");
                    }
                    @Override
                    public void writeTo(@NonNull BufferedSink sink) throws IOException {
                        try (java.io.InputStream in = getContentResolver().openInputStream(localUri)) {
                            if (in == null) throw new IOException("Cannot read file");
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
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Unexpected code " + response.code() + " " + response.message() + "\n" + errorBody);
                    }
                    
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + storagePath;
                    
                    runOnUiThread(() -> {
                        hideUploadProgress();
                        String msgId = UUID.randomUUID().toString();
                        Message msg  = new Message(msgId, chatId, taskId, currentUid, currentName, currentProfileImageUrl,
                                type, publicUrl, fileName,
                                fileSize, audioDuration, System.currentTimeMillis());
                        saveMessage(msg);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideUploadProgress();
                    Toast.makeText(ChatActivity.this, "Lỗi upload Supabase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showUploadProgress(int pct, Uri localUri, String type) {
        llUploadProgress.setVisibility(View.VISIBLE);
        uploadProgressBar.setProgress(pct);
        txtUploadPercent.setText(pct + "%");

        if (Message.TYPE_IMAGE.equals(type) && localUri != null) {
            imgUploadPreview.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(this).load(localUri).into(imgUploadPreview);
        } else {
            imgUploadPreview.setVisibility(View.GONE);
        }
    }

    private void hideUploadProgress() {
        llUploadProgress.setVisibility(View.GONE);
    }

    // ──────── FIRESTORE ────────

    private void saveMessage(Message msg) {
        if (chatId == null) {
            Toast.makeText(this, "Không xác định được phòng chat", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("Chats").document(chatId)
                .collection("Messages").document(msg.getId())
                .set(msg)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        String preview = msg.getMessageType().equals(Message.TYPE_TEXT)
                ? msg.getContent()
                : msg.getMessageType().equals(Message.TYPE_VOICE) ? "🎤 Tin nhắn thoại" : "📎 " + msg.getFileName();

        Map<String, Object> chatDoc = new HashMap<>();
        chatDoc.put("chatId",        chatId);
        chatDoc.put("taskId",        taskId);
        chatDoc.put("taskName",      taskName    != null ? taskName    : "");
        chatDoc.put("projectName",   projectName != null ? projectName : "");
        chatDoc.put("lastMessage",   preview);
        chatDoc.put("lastTimestamp", msg.getTimestamp());
        chatDoc.put("lastSenderName", currentName);
        chatDoc.put("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(currentUid));
        
        chatDoc.put("seenBy", Arrays.asList(currentUid));

        db.collection("Chats").document(chatId)
                .set(chatDoc, com.google.firebase.firestore.SetOptions.merge());
    }

    private void listenForMessages() {
        if (chatId == null) return;
        listenerReg = db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    messageList.clear();
                    messageList.addAll(snapshots.toObjects(Message.class));
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty())
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    
                    updatePinnedBanner();
                    
                    db.collection("Chats").document(chatId)
                        .update("seenBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUid));
                });
    }

    private void updatePinnedBanner() {
        if (llPinnedBanner == null) return;
        List<Message> pinnedList = new ArrayList<>();
        for (Message m : messageList) {
            if (m.isPinned()) pinnedList.add(m);
        }

        if (pinnedList.isEmpty()) {
            llPinnedBanner.setVisibility(View.GONE);
        } else {
            llPinnedBanner.setVisibility(View.VISIBLE);
            Message latestPin = pinnedList.get(pinnedList.size() - 1);
            String previewText = getMessagePreviewLabel(latestPin);
            if (pinnedList.size() > 1) {
                txtPinnedBannerContent.setText(pinnedList.size() + " tin nhắn ghim: " + previewText);
            } else {
                txtPinnedBannerContent.setText("📌 " + previewText);
            }

            llPinnedBanner.setOnClickListener(v -> {
                int pos = messageList.indexOf(latestPin);
                if (pos >= 0) rvMessages.smoothScrollToPosition(pos);
            });

            btnViewAllPinned.setOnClickListener(v -> {
                String[] items = new String[pinnedList.size()];
                for (int i = 0; i < pinnedList.size(); i++) {
                    Message pm = pinnedList.get(i);
                    items[i] = pm.getSenderName() + ": " + getMessagePreviewLabel(pm);
                }
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Tin nhắn đã ghim")
                        .setItems(items, (dialog, which) -> {
                            int pos = messageList.indexOf(pinnedList.get(which));
                            if (pos >= 0) rvMessages.smoothScrollToPosition(pos);
                        })
                        .show();
            });
        }
    }

    /** Returns a human-readable preview label for a message based on its type */
    private String getMessagePreviewLabel(Message msg) {
        if (Message.TYPE_TEXT.equals(msg.getMessageType())) {
            return msg.getContent() != null ? msg.getContent() : "";
        } else if (Message.TYPE_IMAGE.equals(msg.getMessageType())) {
            return "🖼 Hình ảnh";
        } else if (Message.TYPE_VOICE.equals(msg.getMessageType())) {
            return "🎤 Tin nhắn thoại";
        } else {
            return "📎 " + (msg.getFileName() != null ? msg.getFileName() : "Tệp đính kèm");
        }
    }

    // ──────── HELPERS ────────

    private String getFileName(Uri uri) {
        String name = "file_" + System.currentTimeMillis();
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return name;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) size = cursor.getLong(idx);
            }
        } catch (Exception ignored) {}
        return size;
    }

    // ──────── Lifecycle ────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
        if (adapter     != null) adapter.releasePlayer();
        if (recorder    != null) { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); }
        if (recordTimerRunnable != null) recordTimer.removeCallbacks(recordTimerRunnable);
    }

    // ──────── AUTO-COMPLETE MENTIONS ────────

    private void fetchAvailableUsers() {
        db.collection("Users").get().addOnSuccessListener(snapshots -> {
            availableUsers.clear();
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                User u = doc.toObject(User.class);
                if (u.getFullName() != null) availableUsers.add(u);
            }
        });
    }

    private void setupMentionTextWatcher() {
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int cursorPosition = etMessage.getSelectionStart();
                if (cursorPosition <= 0) {
                    hideMentions();
                    return;
                }

                String text = s.toString();
                int lastAtSymbol = text.lastIndexOf('@', cursorPosition - 1);
                
                if (lastAtSymbol != -1) {
                    if (lastAtSymbol == 0 || text.charAt(lastAtSymbol - 1) == ' ' || text.charAt(lastAtSymbol - 1) == '\n') {
                        String query = text.substring(lastAtSymbol + 1, cursorPosition);
                        if (!query.contains(" ") && !query.contains("\n")) {
                            mentionStartIndex = lastAtSymbol;
                            filterMentions(query);
                            return;
                        }
                    }
                }
                hideMentions();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterMentions(String query) {
        mentionResults.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (User u : availableUsers) {
            if (u.getFullName().toLowerCase().contains(lowerQuery) || u.getFullName().toLowerCase().replace(" ", "").contains(lowerQuery)) {
                mentionResults.add(u);
            }
        }

        if (mentionResults.isEmpty()) {
            hideMentions();
        } else {
            if (mentionResults.size() > 5) {
                mentionResults.subList(5, mentionResults.size()).clear();
            }
            rvMentions.setVisibility(View.VISIBLE);
            mentionAdapter.notifyDataSetChanged();
        }
    }

    private void hideMentions() {
        mentionStartIndex = -1;
        rvMentions.setVisibility(View.GONE);
    }

    private void insertMention(User user) {
        if (mentionStartIndex == -1) return;
        
        android.text.Editable editable = etMessage.getText();
        if (editable != null) {
            int currentPos = etMessage.getSelectionStart();
            String insertion = "@" + user.getFullName().replace(" ", "") + " ";
            editable.replace(mentionStartIndex, currentPos, insertion);
        }
        hideMentions();
    }

    private class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.ViewHolder> {
        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.item_mention, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User u = mentionResults.get(position);
            holder.txtName.setText(u.getFullName());
            holder.itemView.setOnClickListener(v -> insertMention(u));
        }

        @Override
        public int getItemCount() { return mentionResults.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName;
            ViewHolder(View v) {
                super(v);
                txtName = v.findViewById(R.id.txtMentionName);
            }
        }
    }
}