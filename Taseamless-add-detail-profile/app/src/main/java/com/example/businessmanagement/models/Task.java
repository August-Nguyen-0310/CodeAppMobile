package com.example.businessmanagement.models;

import java.util.List;
import java.util.Date;

public class Task {
    private String taskId;
    private String projectId;
    private String taskName;
    private String description;
    private Date deadline;
    private List<String> assignedMembers;
    private String status;
    private String fileUrl;
    private String approvedBy;

    public Task() {}

    public Task(String taskId, String projectId, String taskName, String description, Date deadline, List<String> assignedMembers, String status) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.taskName = taskName;
        this.description = description;
        this.deadline = deadline;
        this.assignedMembers = assignedMembers;
        this.status = status;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public List<String> getAssignedMembers() { return assignedMembers; }
    public void setAssignedMembers(List<String> assignedMembers) { this.assignedMembers = assignedMembers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
}