package com.khalid.freyr.farmfield;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farm_fields")
public class FarmField {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Column(name = "area_size", nullable = false, precision = 12, scale = 2)
    private BigDecimal areaSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "crop_stage", nullable = false, length = 50)
    private CropStage cropStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_status", nullable = false, length = 50)
    private WaterStatus waterStatus;

    @Column(name = "pest_reported", nullable = false)
    private boolean pestReported;

    @Column(name = "last_visit_at")
    private Instant lastVisitAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FarmField() {
    }

    public FarmField(
            UUID farmerId,
            String areaName,
            BigDecimal areaSize,
            CropStage cropStage,
            WaterStatus waterStatus,
            boolean pestReported,
            Instant lastVisitAt
    ) {
        this.farmerId = farmerId;
        this.areaName = areaName;
        this.areaSize = areaSize;
        this.cropStage = cropStage;
        this.waterStatus = waterStatus;
        this.pestReported = pestReported;
        this.lastVisitAt = lastVisitAt;
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
            UUID farmerId,
            String areaName,
            BigDecimal areaSize,
            CropStage cropStage,
            WaterStatus waterStatus,
            boolean pestReported,
            Instant lastVisitAt
    ) {
        this.farmerId = farmerId;
        this.areaName = areaName;
        this.areaSize = areaSize;
        this.cropStage = cropStage;
        this.waterStatus = waterStatus;
        this.pestReported = pestReported;
        this.lastVisitAt = lastVisitAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public String getAreaName() {
        return areaName;
    }

    public BigDecimal getAreaSize() {
        return areaSize;
    }

    public CropStage getCropStage() {
        return cropStage;
    }

    public WaterStatus getWaterStatus() {
        return waterStatus;
    }

    public boolean isPestReported() {
        return pestReported;
    }

    public Instant getLastVisitAt() {
        return lastVisitAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
