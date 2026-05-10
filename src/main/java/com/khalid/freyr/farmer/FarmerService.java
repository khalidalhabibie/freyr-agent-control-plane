package com.khalid.freyr.farmer;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;

    public FarmerService(FarmerRepository farmerRepository) {
        this.farmerRepository = farmerRepository;
    }

    @Transactional
    public FarmerResponse createFarmer(CreateFarmerRequest request) {
        Farmer farmer = new Farmer(
                request.name(),
                request.phoneNumber(),
                request.village(),
                request.district()
        );

        Farmer savedFarmer = farmerRepository.save(farmer);
        return toResponse(savedFarmer);
    }

    @Transactional(readOnly = true)
    public List<FarmerResponse> getFarmers() {
        return farmerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FarmerResponse getFarmer(UUID id) {
        return farmerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Farmer not found"));
    }

    @Transactional
    public FarmerResponse updateFarmer(UUID id, UpdateFarmerRequest request) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Farmer not found"));

        farmer.update(
                request.name(),
                request.phoneNumber(),
                request.village(),
                request.district()
        );

        Farmer savedFarmer = farmerRepository.save(farmer);
        return toResponse(savedFarmer);
    }

    private FarmerResponse toResponse(Farmer farmer) {
        return new FarmerResponse(
                farmer.getId(),
                farmer.getName(),
                farmer.getPhoneNumber(),
                farmer.getVillage(),
                farmer.getDistrict(),
                farmer.getCreatedAt(),
                farmer.getUpdatedAt()
        );
    }
}
