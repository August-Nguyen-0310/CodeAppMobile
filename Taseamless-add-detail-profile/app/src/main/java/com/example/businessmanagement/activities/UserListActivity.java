package com.example.businessmanagement.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.adapters.UserListAdapter;
import com.example.businessmanagement.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity implements UserListAdapter.OnUserClickListener {

    private RecyclerView rvUsers;
    private UserListAdapter adapter;
    private List<User> userList;
    private String targetRole;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView tvTitle;
    private ImageView ivBack, ivAddUser;

    private List<User> managerList = new ArrayList<>();
    private List<String> managerNames = new ArrayList<>();
    private String currentUserRole;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserRole = pref.getString("user_role", "Employee");
        
        targetRole = getIntent().getStringExtra("TARGET_ROLE");

        initViews();
        setupRecyclerView();
        fetchUsers();
        loadManagersForSelection();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        tvTitle = findViewById(R.id.tvTitle);
        ivBack = findViewById(R.id.ivBack);
        ivAddUser = findViewById(R.id.ivAddUser);

        tvTitle.setText(targetRole + " List");
        ivBack.setOnClickListener(v -> finish());
        
        // Manager chỉ được phép thêm Employee. Nếu targetRole không phải Employee, ẩn nút thêm.
        if (currentUserRole.equalsIgnoreCase("Manager") && !targetRole.equalsIgnoreCase("Employee")) {
            ivAddUser.setVisibility(View.GONE);
        } else {
            ivAddUser.setVisibility(View.VISIBLE);
            ivAddUser.setOnClickListener(v -> showUserCrudDialog(null));
        }
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new UserListAdapter(userList, this);
        rvUsers.setAdapter(adapter);
    }

    private void fetchUsers() {
        db.collection("Users")
                .whereEqualTo("role", targetRole)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        userList.add(user);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadManagersForSelection() {
        db.collection("Users").whereEqualTo("role", "Manager").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    managerList.clear();
                    managerNames.clear();
                    managerNames.add("No Manager");
                    managerList.add(null);
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User u = doc.toObject(User.class);
                        u.setUid(doc.getId());
                        managerList.add(u);
                        managerNames.add(u.getFullName());
                    }
                });
    }

    private void showUserCrudDialog(User userToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_crud, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etFullName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        Spinner spRole = view.findViewById(R.id.spRole);
        Spinner spManager = view.findViewById(R.id.spManager);
        TextView tvRoleLabel = view.findViewById(R.id.tvRoleLabel);
        TextView tvManagerLabel = view.findViewById(R.id.tvManagerLabel);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        String[] roles = {"Boss", "Manager", "Employee"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(roleAdapter);

        ArrayAdapter<String> mgrAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, managerNames);
        mgrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spManager.setAdapter(mgrAdapter);

        // --- Logic Phân Quyền UI ---
        if (currentUserRole.equalsIgnoreCase("Manager")) {
            // Manager mặc định tạo Employee, không cho phép đổi Role
            if (tvRoleLabel != null) tvRoleLabel.setVisibility(View.GONE);
            spRole.setVisibility(View.GONE);
            
            // Hiện phần Assign Manager cho Manager (để Manager có thể phân chính mình hoặc manager khác)
            if (tvManagerLabel != null) tvManagerLabel.setVisibility(View.VISIBLE);
            spManager.setVisibility(View.VISIBLE);
        } else {
            // Boss có quyền xem và chỉnh sửa tất cả
            if (tvRoleLabel != null) tvRoleLabel.setVisibility(View.VISIBLE);
            spRole.setVisibility(View.VISIBLE);
            if (tvManagerLabel != null) tvManagerLabel.setVisibility(View.VISIBLE);
            spManager.setVisibility(View.VISIBLE);
        }

        if (userToEdit != null) {
            etName.setText(userToEdit.getFullName());
            etEmail.setText(userToEdit.getEmail());
            etPassword.setHint("New password (leave blank to keep)");
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(userToEdit.getRole())) {
                    spRole.setSelection(i);
                    break;
                }
            }
            if (userToEdit.getManagerId() != null) {
                for (int i = 0; i < managerList.size(); i++) {
                    if (managerList.get(i) != null && userToEdit.getManagerId().equals(managerList.get(i).getUid())) {
                        spManager.setSelection(i);
                        break;
                    }
                }
            }
        }

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            String role;
            String managerId;

            if (currentUserRole.equalsIgnoreCase("Manager")) {
                role = "Employee";
                // Lấy managerId từ spinner (Manager tự chọn chính mình hoặc manager khác)
                if (spManager.getSelectedItemPosition() > 0) {
                    managerId = managerList.get(spManager.getSelectedItemPosition()).getUid();
                } else {
                    managerId = currentUid; // Default là Manager đang tạo
                }
            } else {
                role = spRole.getSelectedItem().toString();
                String tempManagerId = null;
                if (spManager.getSelectedItemPosition() > 0) {
                    tempManagerId = managerList.get(spManager.getSelectedItemPosition()).getUid();
                }
                managerId = tempManagerId;
            }

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill name and email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userToEdit == null) {
                if (password.length() < 6) {
                    Toast.makeText(this, "Password must be >= 6 chars", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            String newUid = authResult.getUser().getUid();
                            User newUser = new User(newUid, email, name, role, managerId);
                            newUser.setPassword(password);
                            
                            db.collection("Users").document(newUid).set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        fetchUsers();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Auth creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                userToEdit.setFullName(name);
                userToEdit.setEmail(email);
                userToEdit.setRole(role);
                userToEdit.setManagerId(managerId);
                if (!password.isEmpty()) {
                    userToEdit.setPassword(password);
                }
                db.collection("Users").document(userToEdit.getUid()).set(userToEdit).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchUsers();
                });
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditClick(User user) {
        showUserCrudDialog(user);
    }

    @Override
    public void onDeleteClick(User user) {
        if (user.getUid() == null) return;
        
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getFullName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("Users").document(user.getUid()).delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Deleted from Firestore", Toast.LENGTH_SHORT).show();
                        fetchUsers();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onInboxClick(User user) {
        if (user.getUid() == null || auth.getCurrentUser() == null) return;
        String myUid = auth.getCurrentUser().getUid();
        String targetUid = user.getUid();
        
        if (myUid.equals(targetUid)) {
            Toast.makeText(this, "Không thể nhắn tin cho chính mình!", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = (myUid.compareTo(targetUid) < 0) ? "dm_" + myUid + "_" + targetUid : "dm_" + targetUid + "_" + myUid;

        java.util.Map<String, Object> chatDoc = new java.util.HashMap<>();
        chatDoc.put("chatId", chatId);
        chatDoc.put("taskName", user.getFullName());
        chatDoc.put("projectName", "Tin nhắn riêng");
        chatDoc.put("memberIds", java.util.Arrays.asList(myUid, targetUid));
        
        db.collection("Chats").document(chatId).set(chatDoc, com.google.firebase.firestore.SetOptions.merge());

        android.content.Intent intent = new android.content.Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
        intent.putExtra(ChatActivity.EXTRA_TASK_NAME, user.getFullName());
        intent.putExtra(ChatActivity.EXTRA_PROJECT_NAME, "Tin nhắn riêng");
        startActivity(intent);
    }
}