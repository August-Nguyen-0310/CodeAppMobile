package com.example.businessmanagement.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.ProjectAdapter;
import com.example.businessmanagement.models.Project;
import com.example.businessmanagement.models.User;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ProjectAdapter.OnProjectActionListener {

    private TextView txtGreetingUser, txtBossCount, txtManagerCount, txtEmployeeCount, tvChatBadge;
    private View dashBoard, llBossDashboard, llManagerDashboard, llEmployeeDashboard, llLeftDashboard;
    private TabLayout tabLayout;
    private FirebaseFirestore db;
    private ImageView icAdd, icChat, imgAvatar;
    private RecyclerView rvProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectList = new ArrayList<>();
    private Map<String, Integer> projectTaskCounts = new HashMap<>();
    private Map<String, Integer> projectCompletedTaskCounts = new HashMap<>();
    private ListenerRegistration chatBadgeListener;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        handleRoleAndUI();
        fetchCounts();
        setupClickListeners();
        setupAddButton();
        setupProjectsRecyclerView();
        fetchProjects();
        loadUserAvatar();
        listenForUnreadChats();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        startService(new Intent(this, com.example.businessmanagement.services.NotificationService.class));
    }

    private void initViews() {
        txtGreetingUser = findViewById(R.id.txtGreetingUser);
        txtBossCount = findViewById(R.id.txtBossCount);
        txtManagerCount = findViewById(R.id.txtManagerCount);
        txtEmployeeCount = findViewById(R.id.txtEmployeeCount);
        tvChatBadge = findViewById(R.id.tvChatBadge);
        icAdd = findViewById(R.id.icAdd);
        icChat = findViewById(R.id.imageView4);
        imgAvatar = findViewById(R.id.imgAvatar);
        rvProjects = findViewById(R.id.rvProjects);
        tabLayout = findViewById(R.id.tabLayout);
        
        dashBoard = findViewById(R.id.llDashboard);
        llLeftDashboard = findViewById(R.id.llLeftDashboard);
        llBossDashboard = findViewById(R.id.llBossDashboard);
        llManagerDashboard = findViewById(R.id.llManagerDashboard);
        llEmployeeDashboard = findViewById(R.id.llEmployeeDashboard);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleRoleAndUI() {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserRole = pref.getString("user_role", "Employee");
        String username = pref.getString("user_name", "User");

        if (dashBoard != null) {
            if (currentUserRole.equalsIgnoreCase("Boss")) {
                txtGreetingUser.setText("Boss " + username);
                dashBoard.setVisibility(View.VISIBLE);
                if (llLeftDashboard != null) llLeftDashboard.setVisibility(View.VISIBLE);
                updateEmployeeMargin(5);
                tabLayout.setVisibility(View.VISIBLE);
            } else if (currentUserRole.equalsIgnoreCase("Manager")) {
                txtGreetingUser.setText("Manager " + username);
                dashBoard.setVisibility(View.VISIBLE);
                if (llLeftDashboard != null) llLeftDashboard.setVisibility(View.GONE);
                updateEmployeeMargin(0);
                tabLayout.setVisibility(View.VISIBLE);
            } else {
                txtGreetingUser.setText(username);
                dashBoard.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
            }
        }

        if(icAdd != null){
            if(currentUserRole.equalsIgnoreCase("Boss") || currentUserRole.equalsIgnoreCase("Manager")){
                icAdd.setVisibility(View.VISIBLE);
            } else{
                icAdd.setVisibility(View.GONE);
            }
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { fetchProjects(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateEmployeeMargin(int marginDp) {
        if (llEmployeeDashboard != null) {
            float scale = getResources().getDisplayMetrics().density;
            int pixels = (int) (marginDp * scale + 0.5f);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llEmployeeDashboard.getLayoutParams();
            params.leftMargin = pixels;
            llEmployeeDashboard.setLayoutParams(params);
        }
    }

    private void fetchCounts() {
        db.collection("Users").whereEqualTo("role", "Boss").get()
                .addOnSuccessListener(queryDocumentSnapshots -> txtBossCount.setText(String.valueOf(queryDocumentSnapshots.size())));

        db.collection("Users").whereEqualTo("role", "Manager").get()
                .addOnSuccessListener(queryDocumentSnapshots -> txtManagerCount.setText(String.valueOf(queryDocumentSnapshots.size())));

        db.collection("Users").whereEqualTo("role", "Employee").get()
                .addOnSuccessListener(queryDocumentSnapshots -> txtEmployeeCount.setText(String.valueOf(queryDocumentSnapshots.size())));
    }

    private void setupClickListeners() {
        View.OnClickListener listener = v -> {
            String targetRole = "";
            if (v.getId() == R.id.llBossDashboard) targetRole = "Boss";
            else if (v.getId() == R.id.llManagerDashboard) targetRole = "Manager";
            else if (v.getId() == R.id.llEmployeeDashboard) targetRole = "Employee";

            if (!targetRole.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, UserListActivity.class);
                intent.putExtra("TARGET_ROLE", targetRole);
                startActivity(intent);
            }
        };

        if (llBossDashboard != null) llBossDashboard.setOnClickListener(listener);
        if (llManagerDashboard != null) llManagerDashboard.setOnClickListener(listener);
        if (llEmployeeDashboard != null) llEmployeeDashboard.setOnClickListener(listener);

        if (icChat != null) {
            icChat.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
                startActivity(intent);
            });
        }

        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupAddButton(){
        if (icAdd != null) {
            icAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupProjectsRecyclerView() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            projectAdapter = new ProjectAdapter(projectList, currentUserRole, uid);
            projectAdapter.setOnProjectActionListener(this);
            projectAdapter.setTaskCounts(projectTaskCounts, projectCompletedTaskCounts);
            rvProjects.setAdapter(projectAdapter);
        }
    }

    private void fetchProjects() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        int tabPos = tabLayout.getSelectedTabPosition();
        String targetStatus = "active";
        if (tabPos == 1) targetStatus = "done";
        else if (tabPos == 2) targetStatus = "deleted";

        Query query;
        if (currentUserRole.equalsIgnoreCase("Boss")) {
            query = db.collection("Projects").whereEqualTo("status", targetStatus);
        } else {
            query = db.collection("Projects").whereArrayContains("memberIds", currentUid).whereEqualTo("status", targetStatus);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            projectList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Project project = doc.toObject(Project.class);
                if (project.getProjectId() == null || project.getProjectId().isEmpty()) {
                    project.setProjectId(doc.getId());
                }
                
                // Tự động xóa nếu quá 15 ngày
                if ("deleted".equals(project.getStatus()) && project.getDeleteTimestamp() > 0) {
                    long diff = System.currentTimeMillis() - project.getDeleteTimestamp();
                    if (diff > 15L * 24 * 60 * 60 * 1000) {
                        db.collection("Projects").document(project.getProjectId()).delete();
                        continue;
                    }
                }
                
                projectList.add(project);
            }
            projectAdapter.notifyDataSetChanged();
            fetchTaskCounts(currentUserRole, currentUid);
        });
    }

    @Override
    public void onProjectLongClick(Project project) {
        if ("active".equals(project.getStatus())) {
            showActionDialog(project);
        } else if ("deleted".equals(project.getStatus())) {
            showRestoreDialog(project);
        }
    }

    private void showActionDialog(Project project) {
        String[] options = {"Hoàn thành Project", "Xóa Project"};
        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn Project")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) confirmDone(project);
                    else confirmDelete(project);
                }).show();
    }

    private void confirmDone(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hoàn thành")
                .setMessage("Bạn có chắc chắn muốn chuyển Project này vào mục lưu trữ không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    project.setStatus("done");
                    updateProjectStatus(project);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void confirmDelete(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Project sẽ được chuyển vào mục xóa và tự động xóa vĩnh viễn sau 15 ngày. Tiếp tục?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    project.setStatus("deleted");
                    project.setDeleteTimestamp(System.currentTimeMillis());
                    updateProjectStatus(project);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showRestoreDialog(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Khôi phục Project")
                .setMessage("Bạn có muốn khôi phục Project này về trạng thái hoạt động không?")
                .setPositiveButton("Khôi phục", (dialog, which) -> {
                    project.setStatus("active");
                    project.setDeleteTimestamp(0);
                    updateProjectStatus(project);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void updateProjectStatus(Project project) {
        db.collection("Projects").document(project.getProjectId())
                .set(project)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái Project", Toast.LENGTH_SHORT).show();
                    fetchProjects();
                });
    }

    private void fetchTaskCounts(String role, String userId) {
        Query taskQuery;
        if (role.equalsIgnoreCase("Boss") || role.equalsIgnoreCase("Manager")) {
            taskQuery = db.collection("Tasks");
        } else {
            taskQuery = db.collection("Tasks").whereArrayContains("assignedMembers", userId);
        }

        taskQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            projectTaskCounts.clear();
            projectCompletedTaskCounts.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String projectId = doc.getString("projectId");
                String status = doc.getString("status");
                if (projectId != null) {
                    for (Project p : projectList) {
                        if (p.getProjectId().equals(projectId)) {
                            projectTaskCounts.put(projectId, projectTaskCounts.getOrDefault(projectId, 0) + 1);
                            if ("done".equalsIgnoreCase(status)) {
                                projectCompletedTaskCounts.put(projectId, projectCompletedTaskCounts.getOrDefault(projectId, 0) + 1);
                            }
                            break;
                        }
                    }
                }
            }
            projectAdapter.setTaskCounts(projectTaskCounts, projectCompletedTaskCounts);
        });
    }

    private void loadUserAvatar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_profile).into(imgAvatar);
            }
        });
    }

    private void listenForUnreadChats() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Ẩn các group chat của project đã hoàn thành hoặc xóa
        db.collection("Projects").whereIn("status", java.util.Arrays.asList("done", "deleted")).get().addOnSuccessListener(snapshots -> {
            List<String> inactiveProjectIds = new ArrayList<>();
            for (QueryDocumentSnapshot d : snapshots) inactiveProjectIds.add(d.getId());

            Query query;
            if (currentUserRole.equalsIgnoreCase("Boss")) query = db.collection("Chats");
            else query = db.collection("Chats").whereArrayContains("memberIds", currentUid);

            if (chatBadgeListener != null) chatBadgeListener.remove();
            chatBadgeListener = query.addSnapshotListener((snapshots2, e) -> {
                if (e != null || snapshots2 == null) return;
                int unreadCount = 0;
                for (QueryDocumentSnapshot doc : snapshots2) {
                    String pjId = doc.getString("projectId");
                    if (pjId != null && inactiveProjectIds.contains(pjId)) continue; // Bỏ qua nếu project không còn active

                    List<String> seenBy = (List<String>) doc.get("seenBy");
                    if (seenBy == null || !seenBy.contains(currentUid)) unreadCount++;
                }
                if (unreadCount > 0) {
                    tvChatBadge.setVisibility(View.VISIBLE);
                    tvChatBadge.setText(String.valueOf(unreadCount));
                } else tvChatBadge.setVisibility(View.GONE);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchProjects();
        fetchCounts();
        loadUserAvatar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatBadgeListener != null) chatBadgeListener.remove();
    }
}