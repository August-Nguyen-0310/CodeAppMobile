package com.example.businessmanagement.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.activities.ProjectDetailActivity;
import com.example.businessmanagement.models.Project;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectActionListener {
        void onProjectLongClick(Project project);
    }

    private List<Project> projectList;
    private String userRole;
    private String userId;
    private Map<String, Integer> taskCountMap = new HashMap<>();
    private Map<String, Integer> completedTaskCountMap = new HashMap<>();
    private OnProjectActionListener actionListener;

    public ProjectAdapter(List<Project> projectList, String userRole, String userId) {
        this.projectList = projectList;
        this.userRole = userRole;
        this.userId = userId;
    }

    public void setOnProjectActionListener(OnProjectActionListener listener) {
        this.actionListener = listener;
    }

    public void setTaskCounts(Map<String, Integer> counts, Map<String, Integer> completedCounts) {
        this.taskCountMap = counts;
        this.completedTaskCountMap = completedCounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project project = projectList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvProjectName.setText(project.getProjectName());

        holder.llTaskCount.setVisibility(View.VISIBLE);
        Integer count = taskCountMap.get(project.getProjectId());
        int finalCount = (count != null ? count : 0);
        
        Integer completedCountInt = completedTaskCountMap.get(project.getProjectId());
        int completedCount = (completedCountInt != null ? completedCountInt : 0);

        if (userRole.equalsIgnoreCase("Employee")) {
            holder.tvTaskLabel.setText("Your Task - ");
            holder.tvTaskCount.setText(String.valueOf(finalCount));
        } else {
            holder.tvTaskLabel.setText("Total: ");
            holder.tvTaskCount.setText(finalCount + " tasks");
        }

        float progressValue = 0f;
        if (finalCount > 0) {
            progressValue = (completedCount / (float) finalCount) * 100f;
        }
        holder.progressCircle.setProgressWithAnimation(progressValue, 1000L);
        holder.tvPercentage.setText((int) progressValue + "%");

        holder.btnViewTask.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProjectDetailActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            intent.putExtra("PROJECT_NAME", project.getProjectName());
            context.startActivity(intent);
        });

        holder.btnChat.setOnClickListener(v -> {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Chats")
                .whereEqualTo("projectId", project.getProjectId())
                .whereEqualTo("taskId", null)
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
                                        .setMessage("Đoạn chat Dự án này yêu cầu mã truy cập bảo mật. Vui lòng kiểm tra inbox.")
                                        .setView(etCode)
                                        .setPositiveButton("Xác nhận", (dialog, which) -> {
                                            String input = etCode.getText().toString().trim();
                                            if (input.equals(chatData.getAccessCode())) {
                                                prefs.edit().putBoolean(chatData.getChatId(), true).apply();
                                                android.widget.Toast.makeText(context, "Xác thực thành công!", android.widget.Toast.LENGTH_SHORT).show();
                                                navigateToChatProject(context, chatData.getChatId(), project.getProjectName());
                                            } else {
                                                android.widget.Toast.makeText(context, "Mã truy cập không chính xác!", android.widget.Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton("Hủy", null)
                                        .show();
                            } else {
                                navigateToChatProject(context, chatData.getChatId(), project.getProjectName());
                            }
                        }
                    } else {
                        navigateToChatProject(context, project.getProjectId(), project.getProjectName());
                    }
                });
        });

        // Setup Long Click for Manager/Boss
        if (userRole.equalsIgnoreCase("Boss") || userRole.equalsIgnoreCase("Manager")) {
            holder.itemView.setOnLongClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onProjectLongClick(project);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return (projectList != null) ? projectList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircularProgressBar progressCircle;
        TextView tvPercentage, tvProjectName, tvTaskCount, tvTaskLabel;
        LinearLayout llTaskCount;
        AppCompatButton btnViewTask, btnChat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            progressCircle = itemView.findViewById(R.id.cpb_project);
            tvPercentage = itemView.findViewById(R.id.tvProgressPercentage);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
            tvTaskLabel = itemView.findViewById(R.id.tvTaskLabel);
            llTaskCount = itemView.findViewById(R.id.llTaskCount);
            btnViewTask = itemView.findViewById(R.id.btnViewTask);
            btnChat = itemView.findViewById(R.id.btnChat);
        }
    }

    private static void navigateToChatProject(Context context, String chatId, String projectName) {
        Intent intent = new Intent(context, com.example.businessmanagement.activities.ChatActivity.class);
        intent.putExtra(com.example.businessmanagement.activities.ChatActivity.EXTRA_CHAT_ID, chatId);
        intent.putExtra(com.example.businessmanagement.activities.ChatActivity.EXTRA_PROJECT_NAME, projectName);
        context.startActivity(intent);
    }
}