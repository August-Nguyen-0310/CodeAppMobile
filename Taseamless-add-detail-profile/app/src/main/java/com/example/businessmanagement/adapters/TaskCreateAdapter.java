package com.example.businessmanagement.adapters;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.models.Task;
import com.example.businessmanagement.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskCreateAdapter extends RecyclerView.Adapter<TaskCreateAdapter.ViewHolder> {

    private List<Task> taskList;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private List<User> allEmployees = new ArrayList<>();

    public TaskCreateAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
        fetchAllEmployees();
    }

    private void fetchAllEmployees() {
        FirebaseFirestore.getInstance().collection("Users").whereEqualTo("role", "Employee").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEmployees.clear();
                    allEmployees.addAll(queryDocumentSnapshots.toObjects(User.class));
                    notifyDataSetChanged();
                });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_create, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.etTaskName.setText(task.getTaskName());
        holder.etTaskDescription.setText(task.getDescription());

        holder.etTaskName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.setTaskName(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        holder.etTaskDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.setDescription(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (task.getDeadline() != null) {
            holder.tvDeadline.setText(dateFormat.format(task.getDeadline()));
        } else {
            holder.tvDeadline.setText("Set Deadline");
        }

        holder.tvDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (task.getDeadline() != null) calendar.setTime(task.getDeadline());
            new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                task.setDeadline(calendar.getTime());
                holder.tvDeadline.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        holder.ivDeleteTask.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                taskList.remove(currentPos);
                notifyItemRemoved(currentPos);
            }
        });

        MemberAssignAdapter memberAdapter = new MemberAssignAdapter(allEmployees, task.getAssignedMembers());
        holder.rvAssignMembers.setLayoutManager(new LinearLayoutManager(context));
        holder.rvAssignMembers.setAdapter(memberAdapter);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText etTaskName, etTaskDescription;
        TextView tvDeadline;
        ImageView ivDeleteTask;
        RecyclerView rvAssignMembers;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            etTaskName = itemView.findViewById(R.id.etTaskName);
            etTaskDescription = itemView.findViewById(R.id.etTaskDescription);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            ivDeleteTask = itemView.findViewById(R.id.ivDeleteTask);
            rvAssignMembers = itemView.findViewById(R.id.rvAssignMembers);
        }
    }
}