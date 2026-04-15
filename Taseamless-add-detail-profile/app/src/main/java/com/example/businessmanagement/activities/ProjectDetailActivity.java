package com.example.businessmanagement.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.ProjectDetailAdapter;
import com.example.businessmanagement.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {

    private RecyclerView rvProjectTasks;
    private TextView tvProjectTitle;
    private ImageView ivBack, ivAddTask;
    private ProjectDetailAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private String projectId, projectName;
    private FirebaseFirestore db;
    private String currentUserId, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = pref.getString("user_role", "Employee");

        projectId = getIntent().getStringExtra("PROJECT_ID");
        projectName = getIntent().getStringExtra("PROJECT_NAME");

        initViews();
        setupRecyclerView();
        fetchTasks();
        fetchTodoProgress();
    }

    private void initViews() {
        rvProjectTasks = findViewById(R.id.rvProjectTasks);
        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        ivBack = findViewById(R.id.ivBack);
        ivAddTask = findViewById(R.id.ivAddTask);

        tvProjectTitle.setText(projectName != null ? projectName : "Project Details");
        ivBack.setOnClickListener(v -> finish());
        
        if (userRole.equalsIgnoreCase("Boss") || userRole.equalsIgnoreCase("Manager")) {
            ivAddTask.setVisibility(View.VISIBLE);
        }
        ivAddTask.setOnClickListener(v -> showTaskDialog(null));
    }

    private void setupRecyclerView() {
        adapter = new ProjectDetailAdapter(taskList, userRole, currentUserId, projectName, new ProjectDetailAdapter.OnTaskActionListener() {
            @Override
            public void onEditTask(Task task) {
                showTaskDialog(task);
            }

            @Override
            public void onDeleteTask(Task task) {
                db.collection("Tasks").document(task.getTaskId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProjectDetailActivity.this, "Đã xóa Task", Toast.LENGTH_SHORT).show();
                        fetchTasks();
                    });
            }
        });
        rvProjectTasks.setLayoutManager(new LinearLayoutManager(this));
        rvProjectTasks.setAdapter(adapter);
    }

    private void fetchTasks() {
        if (projectId == null) return;

        db.collection("Tasks")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    List<Task> otherTasks = new ArrayList<>();
                    List<Task> myTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());
                        
                        if (userRole.equalsIgnoreCase("Employee") && 
                            task.getAssignedMembers() != null && 
                            task.getAssignedMembers().contains(currentUserId)) {
                            myTasks.add(task);
                        } else {
                            otherTasks.add(task);
                        }
                    }

                    taskList.addAll(myTasks);
                    taskList.addAll(otherTasks);
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchTodoProgress() {
        if (projectId == null) return;
        db.collection("Todos")
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                
                java.util.Map<String, Integer> totalTodos = new java.util.HashMap<>();
                java.util.Map<String, Integer> completedTodos = new java.util.HashMap<>();

                for (QueryDocumentSnapshot doc : snapshots) {
                    String tid = doc.getString("taskId");
                    String status = doc.getString("status");
                    if (tid != null) {
                        totalTodos.put(tid, totalTodos.getOrDefault(tid, 0) + 1);
                        if ("completed".equalsIgnoreCase(status)) {
                            completedTodos.put(tid, completedTodos.getOrDefault(tid, 0) + 1);
                        }
                    }
                }
                if (adapter != null) {
                    adapter.setTodoProgress(totalTodos, completedTodos);
                }
            });
    }

    private void showTaskDialog(Task existingTask) {
        db.collection("Projects").document(projectId).get().addOnSuccessListener(documentSnapshot -> {
            com.example.businessmanagement.models.Project project = documentSnapshot.toObject(com.example.businessmanagement.models.Project.class);
            if (project == null || project.getMemberIds() == null) return;
            
            db.collection("Users").get().addOnSuccessListener(usersSnap -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle(existingTask == null ? "Thêm Task mới" : "Chỉnh sửa Task");

                android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
                layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                layout.setPadding(48, 24, 48, 24);

                final android.widget.EditText inputName = new android.widget.EditText(this);
                inputName.setHint("Tên Task (Bắt buộc)");
                if (existingTask != null) inputName.setText(existingTask.getTaskName());
                layout.addView(inputName);

                final android.widget.EditText inputDesc = new android.widget.EditText(this);
                inputDesc.setHint("Mô tả Task");
                if (existingTask != null) inputDesc.setText(existingTask.getDescription());
                layout.addView(inputDesc);

                android.widget.TextView tvTitle = new android.widget.TextView(this);
                tvTitle.setText("Chọn người thực hiện:");
                tvTitle.setPadding(0, 48, 0, 16);
                tvTitle.setTextSize(16);
                layout.addView(tvTitle);

                List<android.widget.CheckBox> checkBoxes = new ArrayList<>();
                List<String> userIds = new ArrayList<>();

                for (QueryDocumentSnapshot doc : usersSnap) {
                    String uid = doc.getId();
                    if (project.getMemberIds().contains(uid)) {
                        android.widget.CheckBox cb = new android.widget.CheckBox(this);
                        cb.setText(doc.getString("fullName") + " (" + doc.getString("role") + ")");
                        if (existingTask != null && existingTask.getAssignedMembers() != null && existingTask.getAssignedMembers().contains(uid)) {
                            cb.setChecked(true);
                        }
                        layout.addView(cb);
                        checkBoxes.add(cb);
                        userIds.add(uid);
                    }
                }
                
                scrollView.addView(layout);
                builder.setView(scrollView);

                builder.setPositiveButton("Lưu", (d, which) -> {
                    String name = inputName.getText().toString().trim();
                    String desc = inputDesc.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên Task không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> selectedUids = new ArrayList<>();
                    for(int i = 0; i < checkBoxes.size(); i++) {
                        if (checkBoxes.get(i).isChecked()) {
                            selectedUids.add(userIds.get(i));
                        }
                    }

                    if (existingTask == null) {
                        Task newTask = new Task();
                        newTask.setProjectId(projectId);
                        newTask.setTaskName(name);
                        newTask.setDescription(desc);
                        newTask.setStatus("active");
                        newTask.setAssignedMembers(selectedUids);
                        
                        db.collection("Tasks").add(newTask).addOnSuccessListener(ref -> {
                            Toast.makeText(this, "Đã thêm Task mới", Toast.LENGTH_SHORT).show();
                            fetchTasks(); // Reload danh sách
                        });
                    } else {
                        db.collection("Tasks").document(existingTask.getTaskId())
                            .update("taskName", name, "description", desc, "assignedMembers", selectedUids)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã cập nhật Task", Toast.LENGTH_SHORT).show();
                                fetchTasks();
                            });
                    }
                });
                builder.setNegativeButton("Hủy", null);
                builder.show();
            });
        });
    }
}