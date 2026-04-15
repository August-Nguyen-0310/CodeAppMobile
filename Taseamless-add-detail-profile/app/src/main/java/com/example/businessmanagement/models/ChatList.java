package com.example.businessmanagement.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class ChatList {

    // Firestore document fields
    private String chatId;
    private String taskId;
    private String projectId;
    private String projectName;
    private String taskName;
    private String lastMessage;
    private String lastSenderName;
    private long   lastTimestamp;
    private List<String> memberIds;
    private String accessCode;
    private List<String> seenBy = new ArrayList<>(); // Users who saw the latest message

    public ChatList() {}

    public ChatList(String chatId, String taskId, String projectId, String projectName, String taskName,
                    String lastSenderName, String lastMessage, long lastTimestamp,
                    List<String> memberIds) {
        this.chatId        = chatId;
        this.taskId        = taskId;
        this.projectId     = projectId;
        this.projectName   = projectName;
        this.taskName      = taskName;
        this.lastSenderName = lastSenderName;
        this.lastMessage   = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.memberIds     = memberIds;
        this.seenBy        = new ArrayList<>();
    }

    public String getChatId()        { return chatId; }
    public String getTaskId()        { return taskId; }
    public String getProjectId()     { return projectId; }
    public String getProjectName()   { return projectName; }
    public String getTaskName()      { return taskName; }
    public String getLastSenderName(){ return lastSenderName; }
    public String getLastMessage()   { return lastMessage; }
    public long   getLastTimestamp() { return lastTimestamp; }
    public List<String> getMemberIds(){ return memberIds; }
    public void   setAccessCode(String c) { this.accessCode = c; }
    public String getAccessCode()    { return accessCode; }
    
    public List<String> getSeenBy() { return seenBy; }
    public void setSeenBy(List<String> seenBy) { this.seenBy = seenBy; }

    public String getYourTask()    { return taskName; }
    public String getSenderName()  { return lastSenderName; }

    public boolean isUnread(String currentUserId) {
        if (seenBy == null) return true;
        return !seenBy.contains(currentUserId);
    }

    public String getMessageTime() {
        if (lastTimestamp <= 0) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(lastTimestamp));
    }
}
