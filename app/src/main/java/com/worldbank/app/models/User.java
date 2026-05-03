package com.worldbank.app.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {

    String uid;
    String email;
    String displayName;
    String location;
    String profileImageUrl;
    String cnic;
    String phone;

    @ServerTimestamp
    Date createdAt;

    public User() {}

    public User(String uid, String email, String displayName, String cnic, String phone) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.cnic = cnic;
        this.phone = phone;
        this.location = "";
        this.profileImageUrl = "";
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}