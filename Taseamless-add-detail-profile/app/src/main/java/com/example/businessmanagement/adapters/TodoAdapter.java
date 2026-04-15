package com.example.businessmanagement.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.models.Todo;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {

    private List<Todo> todoList;
    private String userRole;
    private String currentUserId;
    private OnTodoActionListener listener;

    public interface OnTodoActionListener {
        void onCompleteTodo(Todo todo);
        void onEditTodo(Todo todo);
        void onDeleteTodo(Todo todo);
    }

    public TodoAdapter(List<Todo> todoList, String userRole, String currentUserId, OnTodoActionListener listener) {
        this.todoList = todoList;
        this.userRole = userRole;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.tvTitle.setText(todo.getTitle());
        String statusStr = todo.getStatus() != null ? todo.getStatus() : "pending";
        holder.tvStatus.setText("Status: " + statusStr.toUpperCase());
        
        // Cài đặt màu sắc cho status
        switch (statusStr.toLowerCase()) {
            case "pending": holder.tvStatus.setTextColor(Color.YELLOW); break;
            case "completed": holder.tvStatus.setTextColor(Color.GREEN); break;
            case "rejected": holder.tvStatus.setTextColor(Color.RED); break;
            default: holder.tvStatus.setTextColor(Color.WHITE);
        }

        boolean isMyTodo = todo.getAssignedTo() != null && todo.getAssignedTo().equals(currentUserId);
        boolean isBoss = userRole.equalsIgnoreCase("Boss") || userRole.equalsIgnoreCase("Manager");

        // Logic hiển thị nút
        holder.btnSubmit.setVisibility(View.GONE);
        holder.btnRecall.setVisibility(View.GONE);
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        
        holder.btnSubmit.setText("Hoàn thành");

        if (isBoss || isMyTodo) {
            if (!statusStr.equals("completed")) {
                holder.btnSubmit.setVisibility(View.VISIBLE);
            }
        }

        holder.btnSubmit.setOnClickListener(v -> listener.onCompleteTodo(todo));

        if (isBoss) {
            holder.itemView.setOnLongClickListener(v -> {
                String[] options = {"Chỉnh sửa Todo", "Xóa Todo"};
                new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Tùy chọn Todo")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            listener.onEditTodo(todo);
                        } else if (which == 1) {
                            new android.app.AlertDialog.Builder(v.getContext())
                                .setTitle("Xác nhận xóa")
                                .setMessage("Bạn có chắc chắn muốn xóa Todo này không?")
                                .setPositiveButton("Xóa", (d, w) -> listener.onDeleteTodo(todo))
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
        return todoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus;
        Button btnSubmit, btnRecall, btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTodoTitle);
            tvStatus = itemView.findViewById(R.id.tvTodoStatus);
            btnSubmit = itemView.findViewById(R.id.btnSubmit);
            btnRecall = itemView.findViewById(R.id.btnRecall);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}