// ─── User.java ──────────────────────────────────────────────────────────────
// package com.worldbank.app.models;
// Save each class in its own file under: app/src/main/java/com/worldbank/app/models/

/*
 * FILE 1: User.java
 */
package com.worldbank.app.models;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String location;
    private String profileImageUrl;

    public User() {} // Required for Firestore

    public User(String uid, String email, String displayName, String location) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.location = location;
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
}
