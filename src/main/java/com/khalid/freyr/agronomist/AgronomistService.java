package com.khalid.freyr.agronomist;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgronomistService {

    private final AgronomistRepository agronomistRepository;

    public AgronomistService(AgronomistRepository agronomistRepository) {
        this.agronomistRepository = agronomistRepository;
    }

    @Transactional
    public AgronomistResponse createAgronomist(CreateAgronomistRequest request) {
        Agronomist agronomist = new Agronomist(
                request.name(),
                request.phoneNumber(),
                request.assignedDistrict(),
                request.maxDailyVisit(),
                request.availabilityStatus()
        );

        Agronomist savedAgronomist = agronomistRepository.save(agronomist);
        return toResponse(savedAgronomist);
    }

    @Transactional(readOnly = true)
    public List<AgronomistResponse> getAgronomists() {
        return agronomistRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgronomistResponse getAgronomist(UUID id) {
        return agronomistRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Agronomist not found"));
    }

    @Transactional
    public AgronomistResponse updateAgronomist(UUID id, UpdateAgronomistRequest request) {
        Agronomist agronomist = agronomistRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agronomist not found"));

        agronomist.update(
                request.name(),
                request.phoneNumber(),
                request.assignedDistrict(),
                request.maxDailyVisit(),
                request.availabilityStatus()
        );

        Agronomist savedAgronomist = agronomistRepository.save(agronomist);
        return toResponse(savedAgronomist);
    }

    private AgronomistResponse toResponse(Agronomist agronomist) {
        return new AgronomistResponse(
                agronomist.getId(),
                agronomist.getName(),
                agronomist.getPhoneNumber(),
                agronomist.getAssignedDistrict(),
                agronomist.getMaxDailyVisit(),
                agronomist.getAvailabilityStatus(),
                agronomist.getCreatedAt(),
                agronomist.getUpdatedAt()
        );
    }
}
