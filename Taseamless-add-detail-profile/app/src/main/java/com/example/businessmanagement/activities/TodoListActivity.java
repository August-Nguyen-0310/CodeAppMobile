package com.example.businessmanagement.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.TodoAdapter;
import com.example.businessmanagement.models.Todo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.widget.TextView;
import com.example.businessmanagement.models.Task;

public class TodoListActivity extends AppCompatActivity {

    private RecyclerView rvTodos;
    private TodoAdapter adapter;
    private List<Todo> todoList = new ArrayList<>();
    private String taskId, projectId;
    private FirebaseFirestore db;
    private String userRole, currentUserId;
    private ImageView ivBack, ivAddTodo;
    private TextView tvTaskDetailName, tvTaskDetailDesc, tvTaskDetailStatus;
    private Task currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = pref.getString("user_role", "Employee");

        taskId = getIntent().getStringExtra("TASK_ID");
        projectId = getIntent().getStringExtra("PROJECT_ID");

        initViews();
        setupRecyclerView();
        fetchTaskDetails();
        fetchTodos();
    }

    private void initViews() {
        rvTodos = findViewById(R.id.rvTodos);
        ivBack = findViewById(R.id.ivBack);
        ivAddTodo = findViewById(R.id.ivAddTodo);
        
        tvTaskDetailName = findViewById(R.id.tvTaskDetailName);
        tvTaskDetailDesc = findViewById(R.id.tvTaskDetailDesc);
        tvTaskDetailStatus = findViewById(R.id.tvTaskDetailStatus);

        ivBack.setOnClickListener(v -> finish());
        
        if (userRole.equalsIgnoreCase("Boss") || userRole.equalsIgnoreCase("Manager")) {
            ivAddTodo.setVisibility(View.VISIBLE);
        } else {
            ivAddTodo.setVisibility(View.GONE);
        }

        ivAddTodo.setOnClickListener(v -> showAddTodoDialog());
    }

    private void fetchTaskDetails() {
        if (taskId == null) return;
        db.collection("Tasks").document(taskId).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;
            currentTask = snapshot.toObject(Task.class);
            if (currentTask != null) {
                tvTaskDetailName.setText(currentTask.getTaskName() != null ? currentTask.getTaskName() : "Unknown Task");
                tvTaskDetailDesc.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "No description");
                String status = currentTask.getStatus();
                tvTaskDetailStatus.setText("Status: " + (status != null ? status.toUpperCase() : "UNKNOWN"));
            }
        });
    }

    private void showAddTodoDialog() {
        if (currentTask == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Thêm Todo mới");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final android.widget.EditText inputTitle = new android.widget.EditText(this);
        inputTitle.setHint("Tiêu đề Todo");
        layout.addView(inputTitle);

        final android.widget.EditText inputDesc = new android.widget.EditText(this);
        inputDesc.setHint("Mô tả Todo");
        layout.addView(inputDesc);

        final android.widget.Spinner spinnerAssignee = new android.widget.Spinner(this);
        List<String> memberIds = currentTask.getAssignedMembers();
        if (memberIds == null) memberIds = new ArrayList<>();
        
        final List<String> memberNames = new ArrayList<>();
        final List<String> finalMemberIds = new ArrayList<>();
        
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, memberNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignee.setAdapter(spinnerAdapter);

        memberNames.add("Không chọn (Giao cho tất cả)");
        finalMemberIds.add("");

        if (!memberIds.isEmpty()) {
            db.collection("Users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (String uid : currentTask.getAssignedMembers()) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals(uid)) {
                            memberNames.add(doc.getString("fullName"));
                            finalMemberIds.add(uid);
                            break;
                        }
                    }
                }
                spinnerAdapter.notifyDataSetChanged();
            });
        }
        
        layout.addView(spinnerAssignee);
        builder.setView(layout);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            int selectedPos = spinnerAssignee.getSelectedItemPosition();
            String assignedToId = (selectedPos > 0 && selectedPos < finalMemberIds.size()) ? finalMemberIds.get(selectedPos) : "";

            if (title.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập tiêu đề", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            Todo newTodo = new Todo();
            newTodo.setProjectId(projectId);
            newTodo.setTaskId(taskId);
            newTodo.setTitle(title);
            newTodo.setDescription(desc);
            newTodo.setStatus("active");
            newTodo.setAssignedTo(assignedToId);
            newTodo.setDeadline(System.currentTimeMillis() + 86400000L); // Default + 1 day

            db.collection("Todos").add(newTodo)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Đã thêm Todo", Toast.LENGTH_SHORT).show();
                    sendSystemNotification("Đã thêm Todo mới: " + title);
                });
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showEditTodoDialog(Todo todo) {
        if (currentTask == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chỉnh sửa Todo");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final android.widget.EditText inputTitle = new android.widget.EditText(this);
        inputTitle.setText(todo.getTitle());
        layout.addView(inputTitle);

        final android.widget.EditText inputDesc = new android.widget.EditText(this);
        inputDesc.setText(todo.getDescription());
        layout.addView(inputDesc);

        final android.widget.Spinner spinnerAssignee = new android.widget.Spinner(this);
        List<String> memberIds = currentTask.getAssignedMembers();
        if (memberIds == null) memberIds = new ArrayList<>();
        
        final List<String> memberNames = new ArrayList<>();
        final List<String> finalMemberIds = new ArrayList<>();
        
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, memberNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignee.setAdapter(spinnerAdapter);

        memberNames.add("Không chọn (Giao cho tất cả)");
        finalMemberIds.add("");

        if (!memberIds.isEmpty()) {
            db.collection("Users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int assignedIndex = 0;
                for (String uid : currentTask.getAssignedMembers()) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals(uid)) {
                            memberNames.add(doc.getString("fullName"));
                            finalMemberIds.add(uid);
                            if (uid.equals(todo.getAssignedTo())) {
                                assignedIndex = finalMemberIds.size() - 1;
                            }
                            break;
                        }
                    }
                }
                spinnerAdapter.notifyDataSetChanged();
                spinnerAssignee.setSelection(assignedIndex);
            });
        } else {
            spinnerAssignee.setSelection(0);
        }
        
        layout.addView(spinnerAssignee);
        builder.setView(layout);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            int selectedPos = spinnerAssignee.getSelectedItemPosition();
            String assignedToId = (selectedPos > 0 && selectedPos < finalMemberIds.size()) ? finalMemberIds.get(selectedPos) : "";

            if (title.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập tiêu đề", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Todos").document(todo.getId())
                .update("title", title, "description", desc, "assignedTo", assignedToId)
                .addOnSuccessListener(docRef -> {
                    android.widget.Toast.makeText(this, "Đã cập nhật Todo", android.widget.Toast.LENGTH_SHORT).show();
                    sendSystemNotification("Đã cập nhật thay đổi ở Todo: " + title);
                });
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void setupRecyclerView() {
        adapter = new TodoAdapter(todoList, userRole, currentUserId, new TodoAdapter.OnTodoActionListener() {
            @Override
            public void onCompleteTodo(Todo todo) {
                updateTodoStatus(todo, "completed", null);
            }

            @Override
            public void onEditTodo(Todo todo) {
                showEditTodoDialog(todo);
            }

            @Override
            public void onDeleteTodo(Todo todo) {
                db.collection("Todos").document(todo.getId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(TodoListActivity.this, "Đã xóa Todo", Toast.LENGTH_SHORT).show();
                        sendSystemNotification("Đã xóa Todo: " + todo.getTitle());
                    });
            }
        });
        rvTodos.setLayoutManager(new LinearLayoutManager(this));
        rvTodos.setAdapter(adapter);
    }

    private void fetchTodos() {
        if (taskId == null) return;
        db.collection("Todos")
                .whereEqualTo("taskId", taskId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        todoList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Todo todo = doc.toObject(Todo.class);
                            todo.setId(doc.getId());
                            todoList.add(todo);
                        }
                        adapter.notifyDataSetChanged();
                        checkAndUpdateTaskStatus();
                    }
                });
    }

    private void checkAndUpdateTaskStatus() {
        if (taskId == null || todoList.isEmpty()) return;
        boolean allCompleted = true;
        for (Todo t : todoList) {
            if (!"completed".equalsIgnoreCase(t.getStatus())) {
                allCompleted = false;
                break;
            }
        }
        
        String newTaskStatus = allCompleted ? "done" : "active";
        
        if (currentTask != null && !newTaskStatus.equals(currentTask.getStatus())) {
            db.collection("Tasks").document(taskId)
                .update("status", newTaskStatus)
                .addOnSuccessListener(aVoid -> {
                    if (newTaskStatus.equals("done")) {
                        Toast.makeText(this, "Tất cả Todo đã xong. Task cập nhật thành Completed!", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void updateTodoStatus(Todo todo, String status, String feedback) {
        db.collection("Todos").document(todo.getId())
                .update("status", status, "feedback", feedback)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated to " + status, Toast.LENGTH_SHORT).show();
                    if ("completed".equalsIgnoreCase(status)) {
                        sendSystemNotification("Todo '" + todo.getTitle() + "' đã được đánh dấu Hoàn thành!");
                    }
                });
    }

    private void sendSystemNotification(String content) {
        if (taskId == null) return;
        db.collection("Chats").whereEqualTo("taskId", taskId).limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            String targetChatId = taskId;
            if (!queryDocumentSnapshots.isEmpty()) {
                targetChatId = queryDocumentSnapshots.getDocuments().get(0).getId();
            }

            String msgId = UUID.randomUUID().toString();
            // Lấy tên user gửi thông báo
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String uName = pref.getString("user_name", "User");
            String role = pref.getString("user_role", "");
            String fullName = uName + (!role.isEmpty() ? " - " + role : "");

            com.example.businessmanagement.models.Message msg = new com.example.businessmanagement.models.Message(
                msgId, targetChatId, taskId,
                "system", "Hệ thống (bởi " + fullName + ")", null,
                content, System.currentTimeMillis()
            );

            db.collection("Chats").document(targetChatId)
                .collection("Messages").document(msgId)
                .set(msg);
                
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("lastMessage", "Thông báo: " + content);
            updates.put("lastTimestamp", System.currentTimeMillis());
            updates.put("lastSenderName", "Hệ thống");
            db.collection("Chats").document(targetChatId).update(updates);
        });
    }
}