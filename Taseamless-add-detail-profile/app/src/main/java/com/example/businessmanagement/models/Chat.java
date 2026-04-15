package com.example.businessmanagement.models;

import java.util.List;

public class Chat {
    private String chatId;
    private String projectId;
    private String taskId;
    private String taskName;
    private String projectName;
    private List<String> memberIds;
    private String lastMessage;
    private long lastTimestamp;
    private boolean active;
    private String lastSenderName;
    private String accessCode;

    public Chat() {}

    public Chat(String chatId, String projectId, String taskId, String taskName, String projectName, 
                List<String> memberIds, String lastMessage, long lastTimestamp, boolean active) {
        this.chatId = chatId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.taskName = taskName;
        this.projectName = projectName;
        this.memberIds = memberIds;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.active = active;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getLastSenderName() { return lastSenderName; }
    public void setLastSenderName(String lastSenderName) { this.lastSenderName = lastSenderName; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
}