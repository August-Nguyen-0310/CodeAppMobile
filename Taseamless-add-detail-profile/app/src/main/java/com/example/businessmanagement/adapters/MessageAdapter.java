package com.example.businessmanagement.adapters;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.businessmanagement.R;
import com.example.businessmanagement.activities.ProfileActivity;
import com.example.businessmanagement.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private Context context;
    private List<Message> messageList;
    private OnMessageActionClickListener listener;
    private MediaPlayer mediaPlayer;
    private String currentUid;

    public interface OnMessageActionClickListener {
        void onReplyClick(Message msg);
        void onPinClick(Message msg);
        void onDeleteClick(Message msg);
        void onLongClick(Message msg);
    }

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUid = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    public void setOnMessageActionClickListener(OnMessageActionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messageList.get(position);
        boolean isMe = msg.getSenderId() != null && msg.getSenderId().equals(currentUid);

        // Adjust alignment and background based on sender
        RelativeLayout.LayoutParams paramsContainer = (RelativeLayout.LayoutParams) holder.llMessageContainer.getLayoutParams();
        
        if (isMe) {
            // Align to right for current user
            paramsContainer.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
            paramsContainer.removeRule(RelativeLayout.RIGHT_OF);
            paramsContainer.setMarginEnd(8);
            
            holder.imgSender.setVisibility(View.GONE);
            holder.txtSenderName.setVisibility(View.GONE);
            holder.llMessageBackground.setBackgroundResource(R.drawable.bg_deep_blue);
            holder.llMessageBubble.setGravity(Gravity.END);
        } else {
            // Align to left for other users
            paramsContainer.removeRule(RelativeLayout.ALIGN_PARENT_END);
            paramsContainer.addRule(RelativeLayout.RIGHT_OF, R.id.imgSender);
            
            holder.imgSender.setVisibility(View.VISIBLE);
            holder.txtSenderName.setVisibility(View.VISIBLE);
            holder.txtSenderName.setText(msg.getSenderName());
            holder.llMessageBackground.setBackgroundResource(R.drawable.bg_deep_green);
            holder.llMessageBubble.setGravity(Gravity.START);

            // Load avatar for other users
            if (msg.getSenderProfileImageUrl() != null && !msg.getSenderProfileImageUrl().isEmpty()) {
                Glide.with(context).load(msg.getSenderProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .into(holder.imgSender);
            } else {
                holder.imgSender.setImageResource(R.drawable.ic_profile);
            }

            // Click on other user's avatar
            holder.imgSender.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("USER_ID", msg.getSenderId());
                context.startActivity(intent);
            });
        }
        holder.llMessageContainer.setLayoutParams(paramsContainer);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.txtMessageTime.setText(sdf.format(new Date(msg.getTimestamp())));

        if (Message.TYPE_TEXT.equals(msg.getMessageType())) {
            holder.txtMessageContent.setVisibility(View.VISIBLE);
            holder.imgMessageContent.setVisibility(View.GONE);
            holder.txtMessageContent.setText(msg.getContent());
        } else if (Message.TYPE_IMAGE.equals(msg.getMessageType())) {
            holder.txtMessageContent.setVisibility(View.GONE);
            holder.imgMessageContent.setVisibility(View.VISIBLE);
            Glide.with(context).load(msg.getFileUrl()).into(holder.imgMessageContent);
        } else if (Message.TYPE_VOICE.equals(msg.getMessageType())) {
            holder.txtMessageContent.setVisibility(View.VISIBLE);
            holder.imgMessageContent.setVisibility(View.GONE);
            holder.txtMessageContent.setText("🎤 Tin nhắn thoại (" + (msg.getAudioDuration() / 1000) + "s)");
        } else {
            holder.txtMessageContent.setVisibility(View.VISIBLE);
            holder.imgMessageContent.setVisibility(View.GONE);
            holder.txtMessageContent.setText("📎 " + msg.getFileName());
        }

        // Show replied message if available
        if (msg.getReplyToMessageId() != null && !msg.getReplyToMessageId().isEmpty()) {
            holder.llReplyContainer.setVisibility(View.VISIBLE);
            holder.txtRepliedSender.setText(msg.getReplyToSenderName());
            holder.txtRepliedContent.setText(msg.getReplyToContent());
        } else {
            holder.llReplyContainer.setVisibility(View.GONE);
        }

        // Long-click on the whole row
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(msg);
            return true;
        });

        // Long-click on the message bubble background (text/voice/file)
        holder.llMessageBackground.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(msg);
            return true;
        });

        // Long-click directly on the text content
        holder.txtMessageContent.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(msg);
            return true;
        });

        holder.txtMessageContent.setOnClickListener(v -> {
            if (Message.TYPE_VOICE.equals(msg.getMessageType())) {
                playVoice(msg.getFileUrl(), position);
            }
        });

        // Long-click on the image thumbnail → show pin/delete menu
        holder.imgMessageContent.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(msg);
            return true;
        });

        holder.imgMessageContent.setOnClickListener(v -> {
            if (Message.TYPE_IMAGE.equals(msg.getMessageType()) && msg.getFileUrl() != null) {
                showImageExpanded(msg.getFileUrl());
            }
        });
    }

    private void showImageExpanded(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_viewer);
        ImageView imgExpanded = dialog.findViewById(R.id.imgExpanded);
        ImageView btnCloseExpanded = dialog.findViewById(R.id.btnCloseExpanded);
        ImageView btnBackExpanded = dialog.findViewById(R.id.btnBackExpanded);

        Glide.with(context).load(imageUrl).into(imgExpanded);

        btnCloseExpanded.setOnClickListener(v -> dialog.dismiss());
        btnBackExpanded.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void playVoice(String url, int position) {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> notifyItemChanged(position));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSenderName, txtMessageTime, txtMessageContent;
        ImageView imgSender, imgMessageContent;
        LinearLayout llMessageContainer, llMessageBubble, llMessageBackground, llReplyContainer;
        TextView txtRepliedSender, txtRepliedContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSenderName = itemView.findViewById(R.id.txtSenderName);
            txtMessageTime = itemView.findViewById(R.id.txtMessageTime);
            txtMessageContent = itemView.findViewById(R.id.txtMessageContent);
            imgSender = itemView.findViewById(R.id.imgSender);
            imgMessageContent = itemView.findViewById(R.id.imgMessageContent);
            llMessageContainer = itemView.findViewById(R.id.llMessageContainer);
            llMessageBubble = itemView.findViewById(R.id.llMessageBubble);
            llMessageBackground = itemView.findViewById(R.id.llMessageBackground);
            llReplyContainer = itemView.findViewById(R.id.llReplyContainer);
            txtRepliedSender = itemView.findViewById(R.id.txtRepliedSender);
            txtRepliedContent = itemView.findViewById(R.id.txtRepliedContent);
        }
    }
}
