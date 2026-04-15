package com.example.businessmanagement.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.activities.ChatActivity;
import com.example.businessmanagement.activities.TodoListActivity;
import com.example.businessmanagement.models.Member;
import com.example.businessmanagement.models.Message;
import com.example.businessmanagement.models.Task;
import com.example.businessmanagement.models.Todo;
import com.example.businessmanagement.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectDetailAdapter extends RecyclerView.Adapter<ProjectDetailAdapter.ViewHolder> {

    private List<Task> taskList;
    private String userRole;
    private String currentUserId;
    private String projectName; // Added projectName
    private Set<Integer> expandedPositions = new HashSet<>();
    private OnTaskActionListener actionListener;
    private java.util.Map<String, Integer> totalTodosMap = new java.util.HashMap<>();
    private java.util.Map<String, Integer> completedTodosMap = new java.util.HashMap<>();

    public interface OnTaskActionListener {
        void onEditTask(Task task);
        void onDeleteTask(Task task);
    }

    public ProjectDetailAdapter(List<Task> taskList, String userRole, String currentUserId, String projectName, OnTaskActionListener listener) {
        this.taskList = taskList;
        this.userRole = userRole;
        this.currentUserId = currentUserId;
        this.projectName = projectName;
        this.actionListener = listener;
    }

    public void setTodoProgress(java.util.Map<String, Integer> total, java.util.Map<String, Integer> completed) {
        this.totalTodosMap = total;
        this.completedTodosMap = completed;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_detail_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvTaskName.setText(task.getTaskName());
        holder.tvDescription.setText(task.getDescription() != null && !task.getDescription().isEmpty() 
                ? task.getDescription() : "No description provided.");

        int total = totalTodosMap.containsKey(task.getTaskId()) ? totalTodosMap.get(task.getTaskId()) : 0;
        int completed = completedTodosMap.containsKey(task.getTaskId()) ? completedTodosMap.get(task.getTaskId()) : 0;
        
        if (total > 0) {
            int progress = (int) ((completed / (float) total) * 100);
            holder.pbTaskProgress.setProgress(progress);
            holder.tvTaskProgressText.setText(progress + "%");
        } else {
            if ("done".equalsIgnoreCase(task.getStatus())) {
                holder.pbTaskProgress.setProgress(100);
                holder.tvTaskProgressText.setText("100%");
            } else {
                holder.pbTaskProgress.setProgress(0);
                holder.tvTaskProgressText.setText("0%");
            }
        }

        boolean isMyTask = task.getAssignedMembers() != null && task.getAssignedMembers().contains(currentUserId);
        boolean isBoss = userRole.equalsIgnoreCase("Boss") || userRole.equalsIgnoreCase("Manager");

        boolean isExpanded = expandedPositions.contains(position) || (userRole.equalsIgnoreCase("Employee") && isMyTask);

        if (isExpanded) {
            holder.detailsSection.setVisibility(View.VISIBLE);
            holder.setupMembers(task.getAssignedMembers());
            
            if (isBoss || isMyTask) {
                holder.rightContentLayout.setVisibility(View.VISIBLE);
                holder.setupChatPreview(task.getTaskId());

                holder.chatPreviewSection.setOnClickListener(v -> {
                    FirebaseFirestore.getInstance().collection("Chats")
                        .whereEqualTo("taskId", task.getTaskId())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                com.example.businessmanagement.models.Chat chatData = queryDocumentSnapshots.getDocuments().get(0).toObject(com.example.businessmanagement.models.Chat.class);
                                if (chatData != null) {
                                    android.content.SharedPreferences prefs = context.getSharedPreferences("ChatAccess", Context.MODE_PRIVATE);
                                    boolean isUnlocked = prefs.getBoolean(chatData.getChatId(), false);

                                    if (chatData.getAccessCode() != null && !chatData.getAccessCode().isEmpty() && !isUnlocked) {
                                        final android.widget.EditText etCode = new android.widget.EditText(context);
                                        etCode.setHint("Nhập mã truy cập 6 số");
                                        new android.app.AlertDialog.Builder(context)
                                                .setTitle("Nhập Mã Truy Cập")
                                                .setMessage("Đoạn chat Task này yêu cầu mã truy cập bảo mật. Vui lòng kiểm tra inbox.")
                                                .setView(etCode)
                                                .setPositiveButton("Xác nhận", (dialog, which) -> {
                                                    String input = etCode.getText().toString().trim();
                                                    if (input.equals(chatData.getAccessCode())) {
                                                        prefs.edit().putBoolean(chatData.getChatId(), true).apply();
                                                        android.widget.Toast.makeText(context, "Xác thực thành công!", android.widget.Toast.LENGTH_SHORT).show();
                                                        navigateToChat(context, chatData.getChatId(), task.getTaskId(), task.getTaskName(), projectName);
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Mã truy cập không chính xác!", android.widget.Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .setNegativeButton("Hủy", null)
                                                .show();
                                    } else {
                                        navigateToChat(context, chatData.getChatId(), task.getTaskId(), task.getTaskName(), projectName);
                                    }
                                }
                            } else {
                                navigateToChat(context, task.getTaskId(), task.getTaskId(), task.getTaskName(), projectName);
                            }
                        });
                });

                holder.todoSection.setOnClickListener(v -> {
                    Intent intent = new Intent(context, TodoListActivity.class);
                    intent.putExtra("TASK_ID", task.getTaskId());
                    intent.putExtra("PROJECT_ID", task.getProjectId());
                    context.startActivity(intent);
                });
            } else {
                holder.rightContentLayout.setVisibility(View.GONE);
            }
        } else {
            holder.detailsSection.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (expandedPositions.contains(pos)) {
                expandedPositions.remove(pos);
            } else {
                expandedPositions.add(pos);
            }
            notifyItemChanged(pos);
        });

        if (isBoss) {
            holder.itemView.setOnLongClickListener(v -> {
                String[] options = {"Chỉnh sửa Task", "Xóa Task"};
                new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Tùy chọn Task")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (actionListener != null) actionListener.onEditTask(task);
                        } else if (which == 1) {
                            new android.app.AlertDialog.Builder(v.getContext())
                                .setTitle("Xóa Task")
                                .setMessage("Chắc chắn xóa Task này?")
                                .setPositiveButton("Xóa", (d, w) -> {
                                    if (actionListener != null) actionListener.onDeleteTask(task);
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                        }
                    })
                    .show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvDescription, tvChatPlaceholder, tvTodoPlaceholder, tvTaskProgressText;
        android.widget.ProgressBar pbTaskProgress;
        LinearLayout detailsSection, rightContentLayout;
        FrameLayout chatPreviewSection, todoSection;
        RecyclerView rvMembers, rvChatPreview, rvTodoPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.txtYourTask);
            tvDescription = itemView.findViewById(R.id.textView2);
            tvTaskProgressText = itemView.findViewById(R.id.tvTaskProgressText);
            pbTaskProgress = itemView.findViewById(R.id.pbTaskProgress);
            detailsSection = itemView.findViewById(R.id.detailsSection);
            rightContentLayout = itemView.findViewById(R.id.rightContentLayout);
            rvMembers = itemView.findViewById(R.id.rvMembers);
            chatPreviewSection = itemView.findViewById(R.id.chatPreviewSection);
            todoSection = itemView.findViewById(R.id.todoSection);
            
            rvChatPreview = itemView.findViewById(R.id.rvChatPreview);
            
            tvChatPlaceholder = itemView.findViewById(R.id.tvChatPlaceholder);
            tvTodoPlaceholder = itemView.findViewById(R.id.tvTodoPlaceholder);
        }

        void setupMembers(List<String> memberIds) {
            if (memberIds == null || memberIds.isEmpty()) {
                rvMembers.setAdapter(null);
                return;
            }
            List<Member> members = new ArrayList<>();
            FirebaseFirestore.getInstance().collection("Users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (String id : memberIds) {
                    queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> doc.getId().equals(id))
                            .findFirst()
                            .ifPresent(doc -> {
                                User u = doc.toObject(User.class);
                                if (u != null) {
                                    members.add(new Member(id, u.getFullName(), u.getRole(), u.getProfileImageUrl()));
                                }
                            });
                }
                MemberAdapter adapter = new MemberAdapter(members);
                rvMembers.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvMembers.setAdapter(adapter);
            });
        }

        void setupChatPreview(String taskId) {
            FirebaseFirestore.getInstance().collection("Messages")
                    .whereEqualTo("taskId", taskId)
                    .limit(10)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Message> messages = queryDocumentSnapshots.toObjects(Message.class);
                        messages.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        if (messages.size() > 3) messages = messages.subList(0, 3);

                        if (messages.isEmpty()) {
                            tvChatPlaceholder.setVisibility(View.VISIBLE);
                            rvChatPreview.setVisibility(View.GONE);
                        } else {
                            tvChatPlaceholder.setVisibility(View.GONE);
                            rvChatPreview.setVisibility(View.VISIBLE);
                            MessagePreviewAdapter adapter = new MessagePreviewAdapter(messages);
                            rvChatPreview.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                            rvChatPreview.setAdapter(adapter);
                        }
                    });
        }

        // setupTodoPreview removed - viewing Todo List is now only through TodoListActivity
    }

    private static void navigateToChat(Context context, String chatId, String taskId, String taskName, String projectName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
        intent.putExtra(ChatActivity.EXTRA_TASK_ID, taskId);
        intent.putExtra(ChatActivity.EXTRA_TASK_NAME, taskName);
        intent.putExtra(ChatActivity.EXTRA_PROJECT_NAME, projectName); // Pass projectName
        context.startActivity(intent);
    }
}