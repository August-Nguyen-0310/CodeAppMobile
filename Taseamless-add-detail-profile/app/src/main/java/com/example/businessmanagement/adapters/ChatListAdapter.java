package com.example.businessmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.models.ChatList;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ChatList chat, int position);
    }

    private List<ChatList> chatList;
    private OnItemClickListener listener;
    private String currentUserId;

    public ChatListAdapter(List<ChatList> chatList, String currentUserId) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatList chat = chatList.get(position);

        // Project Name
        if (chat.getProjectName() != null && !chat.getProjectName().isEmpty()) {
            holder.txtProjectName.setText(chat.getProjectName());
            holder.txtProjectName.setVisibility(View.VISIBLE);
        } else {
            holder.txtProjectName.setVisibility(View.GONE);
        }

        // Task Name & Separator logic
        if (chat.getTaskName() != null && !chat.getTaskName().isEmpty()) {
            holder.txtYourTask.setText(chat.getTaskName());
            holder.txtYourTask.setVisibility(View.VISIBLE);
            holder.txtChatSeparator.setVisibility(holder.txtProjectName.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        } else {
            holder.txtYourTask.setVisibility(View.GONE);
            holder.txtChatSeparator.setVisibility(View.GONE);
        }

        // Last Message & Sender logic
        if (chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()) {
            holder.txtSenderName.setText(chat.getLastSenderName());
            holder.txtMessageContent.setText(chat.getLastMessage());
            holder.txtMessageTime.setText(chat.getMessageTime());
            holder.txtSenderName.setVisibility(View.VISIBLE);
            holder.txtSenderSeparator.setVisibility(View.VISIBLE);
        } else {
            holder.txtSenderName.setVisibility(View.GONE);
            holder.txtSenderSeparator.setVisibility(View.GONE);
            holder.txtMessageContent.setText("No messages yet");
            holder.txtMessageTime.setText("");
        }

        // Unread Indicator
        if (chat.isUnread(currentUserId)) {
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            holder.txtMessageContent.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtMessageContent.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black));
        } else {
            holder.viewUnreadIndicator.setVisibility(View.GONE);
            holder.txtMessageContent.setTypeface(null, android.graphics.Typeface.ITALIC);
            holder.txtMessageContent.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.grey));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(chat, position);
        });
    }

    @Override
    public int getItemCount() {
        return (chatList != null) ? chatList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProjectName, txtYourTask, txtSenderName, txtMessageContent, txtMessageTime;
        TextView txtChatSeparator, txtSenderSeparator;
        View viewUnreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProjectName   = itemView.findViewById(R.id.txtProjectName);
            txtYourTask      = itemView.findViewById(R.id.txtYourTask);
            txtSenderName    = itemView.findViewById(R.id.txtSenderName);
            txtMessageContent = itemView.findViewById(R.id.txtMessageContent);
            txtMessageTime   = itemView.findViewById(R.id.txtMessageTime);
            txtChatSeparator = itemView.findViewById(R.id.txtChatSeparator);
            txtSenderSeparator = itemView.findViewById(R.id.txtSenderSeparator);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}
