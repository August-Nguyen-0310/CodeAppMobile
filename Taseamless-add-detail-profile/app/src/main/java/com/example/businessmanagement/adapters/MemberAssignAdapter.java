package com.example.businessmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.models.User;

import java.util.List;

public class MemberAssignAdapter extends RecyclerView.Adapter<MemberAssignAdapter.ViewHolder> {

    private List<User> employeeList;
    private List<String> selectedMemberIds;

    public MemberAssignAdapter(List<User> employeeList, List<String> selectedMemberIds) {
        this.employeeList = employeeList;
        this.selectedMemberIds = selectedMemberIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = employeeList.get(position);
        holder.tvMemberName.setText(user.getFullName());
        
        // Gỡ bỏ listener cũ trước khi set trạng thái để tránh lỗi khi recycle view
        holder.cbAssign.setOnCheckedChangeListener(null);
        holder.cbAssign.setChecked(selectedMemberIds.contains(user.getUid()));
        
        holder.cbAssign.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedMemberIds.contains(user.getUid())) {
                    selectedMemberIds.add(user.getUid());
                }
            } else {
                selectedMemberIds.remove(user.getUid());
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbAssign;
        TextView tvMemberName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbAssign = itemView.findViewById(R.id.cbAssign);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
        }
    }
}