package com.example.businessmanagement.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    @DocumentId
    private String uid;
    private String email;
    private String fullName;
    private String role;
    private String managerId;
    private String password;
    private String profileImageUrl;

    public User() {}

    public User(String uid, String email, String fullName, String role, String managerId) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.managerId = managerId;
    }

    public User(String uid, String email, String fullName, String role, String managerId, String password) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.managerId = managerId;
        this.password = password;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}