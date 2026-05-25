package com.example.demo.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "sites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    private String location;

    @Column(name = "active_status", nullable = false, columnDefinition = "boolean default true")
    private Boolean activeStatus = true;

    // 1. Add the field with a default of true
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // 2. Add the getter method
    public boolean isActive() {
        return active;
    }

    // 3. Add the setter method
    public void setActive(boolean active) {
        this.active = active;
    }
    public String getSiteName() {
        return this.siteName;
    }
}