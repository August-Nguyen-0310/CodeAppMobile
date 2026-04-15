package com.example.businessmanagement.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.businessmanagement.activities.ProfileActivity;
import com.example.businessmanagement.models.Member;
import com.example.businessmanagement.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> memberList;

    public MemberAdapter(List<Member> memberList) {
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberList.get(position);
        holder.txtMemberName.setText(member.getName());
        holder.txtMemberRole.setText(member.getRole());

        if (member.getAvatar() != null && !member.getAvatar().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(member.getAvatar())
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.imgMember);
        } else {
            holder.imgMember.setImageResource(R.drawable.ic_profile);
        }

        holder.imgMember.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
            intent.putExtra("USER_ID", member.getId());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgMember;
        TextView txtMemberName;
        TextView txtMemberRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMember = itemView.findViewById(R.id.imgMember);
            txtMemberName = itemView.findViewById(R.id.txtMemberName);
            txtMemberRole = itemView.findViewById(R.id.txtMemberRole);
        }
    }
}
