package com.khalid.freyr.farmfield;

import com.khalid.freyr.farmer.FarmerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FarmFieldService {

    private final FarmFieldRepository farmFieldRepository;
    private final FarmerRepository farmerRepository;

    public FarmFieldService(FarmFieldRepository farmFieldRepository, FarmerRepository farmerRepository) {
        this.farmFieldRepository = farmFieldRepository;
        this.farmerRepository = farmerRepository;
    }

    @Transactional
    public FarmFieldResponse createFarmField(CreateFarmFieldRequest request) {
        validateFarmerExists(request.farmerId());

        FarmField farmField = new FarmField(
                request.farmerId(),
                request.areaName(),
                request.areaSize(),
                request.cropStage(),
                request.waterStatus(),
                request.pestReported(),
                request.lastVisitAt()
        );

        FarmField savedFarmField = farmFieldRepository.save(farmField);
        return toResponse(savedFarmField);
    }

    @Transactional(readOnly = true)
    public List<FarmFieldResponse> getFarmFields() {
        return farmFieldRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FarmFieldResponse getFarmField(UUID id) {
        return farmFieldRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Farm field not found"));
    }

    @Transactional
    public FarmFieldResponse updateFarmField(UUID id, UpdateFarmFieldRequest request) {
        FarmField farmField = farmFieldRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Farm field not found"));

        validateFarmerExists(request.farmerId());

        farmField.update(
                request.farmerId(),
                request.areaName(),
                request.areaSize(),
                request.cropStage(),
                request.waterStatus(),
                request.pestReported(),
                request.lastVisitAt()
        );

        FarmField savedFarmField = farmFieldRepository.save(farmField);
        return toResponse(savedFarmField);
    }

    private void validateFarmerExists(UUID farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new EntityNotFoundException("Farmer not found");
        }
    }

    private FarmFieldResponse toResponse(FarmField farmField) {
        return new FarmFieldResponse(
                farmField.getId(),
                farmField.getFarmerId(),
                farmField.getAreaName(),
                farmField.getAreaSize(),
                farmField.getCropStage(),
                farmField.getWaterStatus(),
                farmField.isPestReported(),
                farmField.getLastVisitAt(),
                farmField.getCreatedAt(),
                farmField.getUpdatedAt()
        );
    }
}
