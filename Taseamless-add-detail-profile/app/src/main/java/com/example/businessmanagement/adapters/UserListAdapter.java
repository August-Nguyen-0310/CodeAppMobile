package com.example.businessmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.businessmanagement.R;
import com.example.businessmanagement.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onEditClick(User user);
        void onDeleteClick(User user);
        void onInboxClick(User user);
    }

    public UserListAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getFullName());
        holder.tvUserEmail.setText(user.getEmail());

        // Load user avatar using Glide
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_profile);
        }

        if (user.getManagerId() != null && !user.getManagerId().isEmpty()) {
            FirebaseFirestore.getInstance().collection("Users").document(user.getManagerId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.tvManagerInfo.setText("Manager: " + documentSnapshot.getString("fullName"));
                    }
                });
        } else {
            holder.tvManagerInfo.setText("Manager: None");
        }

        holder.ivEditUser.setOnClickListener(v -> listener.onEditClick(user));
        holder.ivDeleteUser.setOnClickListener(v -> listener.onDeleteClick(user));
        if (holder.ivInboxUser != null) {
            holder.ivInboxUser.setOnClickListener(v -> listener.onInboxClick(user));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvManagerInfo;
        ImageView ivEditUser, ivDeleteUser, ivInboxUser, ivUserAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvManagerInfo = itemView.findViewById(R.id.tvManagerInfo);
            ivEditUser = itemView.findViewById(R.id.ivEditUser);
            ivDeleteUser = itemView.findViewById(R.id.ivDeleteUser);
            ivInboxUser = itemView.findViewById(R.id.ivInboxUser);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
    }
}