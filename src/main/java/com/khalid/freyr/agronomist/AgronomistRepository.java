package com.khalid.freyr.agronomist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgronomistRepository extends JpaRepository<Agronomist, UUID> {

    List<Agronomist> findByAssignedDistrictAndAvailabilityStatus(
            String assignedDistrict,
            AvailabilityStatus availabilityStatus
    );
}
