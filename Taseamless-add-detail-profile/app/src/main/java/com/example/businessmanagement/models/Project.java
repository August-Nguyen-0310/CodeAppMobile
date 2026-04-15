package com.example.businessmanagement.models;

import java.util.List;

public class Project {
    private String projectId;
    private String projectName;
    private String description;
    private String createdBy;
    private String leadId;
    private List<String> managerIds;
    private List<String> memberIds;
    private String status; // active, done, deleted
    private long deleteTimestamp;

    public Project() {}

    public Project(String projectId, String projectName, String description, String createdBy, String leadId, List<String> managerIds, List<String> memberIds, String status) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.description = description;
        this.createdBy = createdBy;
        this.leadId = leadId;
        this.managerIds = managerIds;
        this.memberIds = memberIds;
        this.status = status;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public List<String> getManagerIds() { return managerIds; }
    public void setManagerIds(List<String> managerIds) { this.managerIds = managerIds; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDeleteTimestamp() { return deleteTimestamp; }
    public void setDeleteTimestamp(long deleteTimestamp) { this.deleteTimestamp = deleteTimestamp; }
}