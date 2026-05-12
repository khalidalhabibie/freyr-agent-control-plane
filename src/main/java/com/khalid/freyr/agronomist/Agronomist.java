package com.khalid.freyr.agronomist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agronomists")
public class Agronomist {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "assigned_district", nullable = false)
    private String assignedDistrict;

    @Column(name = "max_daily_visit", nullable = false)
    private Integer maxDailyVisit;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 50)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Agronomist() {
    }

    public Agronomist(
            String name,
            String phoneNumber,
            String assignedDistrict,
            Integer maxDailyVisit,
            AvailabilityStatus availabilityStatus
    ) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.assignedDistrict = assignedDistrict;
        this.maxDailyVisit = maxDailyVisit;
        this.availabilityStatus = availabilityStatus;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            String name,
            String phoneNumber,
            String assignedDistrict,
            Integer maxDailyVisit,
            AvailabilityStatus availabilityStatus
    ) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.assignedDistrict = assignedDistrict;
        this.maxDailyVisit = maxDailyVisit;
        this.availabilityStatus = availabilityStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAssignedDistrict() {
        return assignedDistrict;
    }

    public Integer getMaxDailyVisit() {
        return maxDailyVisit;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
