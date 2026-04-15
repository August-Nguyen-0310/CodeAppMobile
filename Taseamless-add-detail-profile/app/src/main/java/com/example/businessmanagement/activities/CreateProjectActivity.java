package com.example.businessmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.TaskCreateAdapter;
import com.example.businessmanagement.models.Project;
import com.example.businessmanagement.models.Task;
import com.example.businessmanagement.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;

public class CreateProjectActivity extends AppCompatActivity {

    private EditText etProjectName, etDescription;
    private Spinner spLead;
    private RecyclerView rvTasksCreate;
    private Button btnCreateProject, btnAddTask;
    private ImageView ivBack;

    private FirebaseFirestore db;
    private List<User> managerList = new ArrayList<>();
    private List<Task> taskList = new ArrayList<>();
    private TaskCreateAdapter taskAdapter;
    private List<String> managerNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupTaskRecyclerView();
        loadManagers();

        btnAddTask.setOnClickListener(v -> addNewTask());
        btnCreateProject.setOnClickListener(v -> validateAndCreateProject());
        ivBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etProjectName = findViewById(R.id.etProjectName);
        etDescription = findViewById(R.id.etDescription);
        spLead = findViewById(R.id.spLead);
        rvTasksCreate = findViewById(R.id.rvTasksCreate);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnAddTask = findViewById(R.id.btnAddIteration);
        ivBack = findViewById(R.id.ivBack);
    }

    private void setupTaskRecyclerView() {
        taskAdapter = new TaskCreateAdapter(taskList, this);
        rvTasksCreate.setLayoutManager(new LinearLayoutManager(this));
        rvTasksCreate.setAdapter(taskAdapter);
    }

    private void loadManagers() {
        db.collection("Users").whereEqualTo("role", "Manager").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    managerList.clear();
                    managerNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        managerList.add(user);
                        managerNames.add(user.getFullName());
                    }
                    
                    if (managerNames.isEmpty()) {
                        managerNames.add("No Manager found");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, managerNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spLead.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load managers", Toast.LENGTH_SHORT).show();
                });
    }

    private void addNewTask() {
        Task newTask = new Task();
        newTask.setTaskName("");
        newTask.setDescription("");
        newTask.setStatus("pending");
        newTask.setAssignedMembers(new ArrayList<>());
        taskList.add(newTask);
        taskAdapter.notifyItemInserted(taskList.size() - 1);
    }

    private void validateAndCreateProject() {
        String name = etProjectName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (name.isEmpty() || taskList.isEmpty()) {
            Toast.makeText(this, "Please enter project name and add at least one task", Toast.LENGTH_SHORT).show();
            return;
        }

        if (managerList.isEmpty() || spLead.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Please select a Lead Manager", Toast.LENGTH_SHORT).show();
            return;
        }

        User selectedLead = managerList.get(spLead.getSelectedItemPosition());
        String bossUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<String> managerIds = new ArrayList<>();
        managerIds.add(selectedLead.getUid()); 
        
        List<String> memberIds = new ArrayList<>();
        memberIds.add(bossUid);
        memberIds.add(selectedLead.getUid());
        
        for (Task t : taskList) {
            if (t.getAssignedMembers() != null) {
                for (String mId : t.getAssignedMembers()) {
                    if (!memberIds.contains(mId)) memberIds.add(mId);
                }
            }
        }

        Project project = new Project(null, name, desc, bossUid, selectedLead.getUid(), managerIds, memberIds, "active");
        
        saveProjectToFirestore(project, taskList);
    }

    private void saveProjectToFirestore(Project project, List<Task> tasks) {
        WriteBatch batch = db.batch();
        DocumentReference projectRef = db.collection("Projects").document();
        project.setProjectId(projectRef.getId());
        batch.set(projectRef, project);

        long timestamp = System.currentTimeMillis();

        for (Task task : tasks) {
            DocumentReference taskRef = db.collection("Tasks").document();
            task.setTaskId(taskRef.getId());
            task.setProjectId(project.getProjectId());
            batch.set(taskRef, task);

            // Create Chat document with full initial data
            DocumentReference chatRef = db.collection("Chats").document();
            List<String> chatMembers = new ArrayList<>();
            if (task.getAssignedMembers() != null) chatMembers.addAll(task.getAssignedMembers());
            if (!chatMembers.contains(project.getCreatedBy())) chatMembers.add(project.getCreatedBy());
            if (!chatMembers.contains(project.getLeadId())) chatMembers.add(project.getLeadId());
            
            com.example.businessmanagement.models.Chat chat = new com.example.businessmanagement.models.Chat(
                chatRef.getId(), 
                project.getProjectId(), 
                task.getTaskId(), 
                task.getTaskName(),
                project.getProjectName(),
                chatMembers, 
                "No messages yet", 
                timestamp,
                true
            );
            
            batch.set(chatRef, chat);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Project Created!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
