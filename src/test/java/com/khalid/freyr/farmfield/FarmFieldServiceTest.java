package com.khalid.freyr.farmfield;

import com.khalid.freyr.farmer.FarmerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FarmFieldServiceTest {

    @Mock
    private FarmFieldRepository farmFieldRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @InjectMocks
    private FarmFieldService farmFieldService;

    @Test
    void createFarmFieldSavesAndReturnsFieldWhenFarmerExists() {
        UUID farmerId = UUID.randomUUID();
        Instant lastVisitAt = Instant.parse("2026-05-01T10:15:30Z");
        CreateFarmFieldRequest request = new CreateFarmFieldRequest(
                farmerId,
                "North Block",
                new BigDecimal("2.50"),
                CropStage.VEGETATIVE,
                WaterStatus.WET,
                false,
                lastVisitAt
        );

        when(farmerRepository.existsById(farmerId)).thenReturn(true);
        when(farmFieldRepository.save(any(FarmField.class))).thenAnswer(invocation -> {
            FarmField farmField = invocation.getArgument(0);
            farmField.prePersist();
            return farmField;
        });

        FarmFieldResponse response = farmFieldService.createFarmField(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.farmerId()).isEqualTo(farmerId);
        assertThat(response.areaName()).isEqualTo("North Block");
        assertThat(response.areaSize()).isEqualByComparingTo("2.50");
        assertThat(response.cropStage()).isEqualTo(CropStage.VEGETATIVE);
        assertThat(response.waterStatus()).isEqualTo(WaterStatus.WET);
        assertThat(response.pestReported()).isFalse();
        assertThat(response.lastVisitAt()).isEqualTo(lastVisitAt);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(farmFieldRepository).save(any(FarmField.class));
    }

    @Test
    void createFarmFieldThrowsWhenFarmerDoesNotExist() {
        UUID farmerId = UUID.randomUUID();
        CreateFarmFieldRequest request = new CreateFarmFieldRequest(
                farmerId,
                "North Block",
                new BigDecimal("2.50"),
                CropStage.VEGETATIVE,
                WaterStatus.WET,
                false,
                null
        );

        when(farmerRepository.existsById(farmerId)).thenReturn(false);

        assertThatThrownBy(() -> farmFieldService.createFarmField(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farmer not found");

        verify(farmFieldRepository, never()).save(any(FarmField.class));
    }

    @Test
    void getFarmFieldsReturnsAllFields() {
        UUID farmerId = UUID.randomUUID();
        FarmField farmField = persistedFarmField(
                farmerId,
                "East Plot",
                new BigDecimal("1.75"),
                CropStage.SEEDING,
                WaterStatus.UNKNOWN,
                false,
                null
        );

        when(farmFieldRepository.findAll()).thenReturn(List.of(farmField));

        List<FarmFieldResponse> responses = farmFieldService.getFarmFields();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().farmerId()).isEqualTo(farmerId);
        assertThat(responses.getFirst().areaName()).isEqualTo("East Plot");
    }

    @Test
    void getFarmFieldReturnsFieldWhenFound() {
        FarmField farmField = persistedFarmField(
                UUID.randomUUID(),
                "West Plot",
                new BigDecimal("3.00"),
                CropStage.REPRODUCTIVE,
                WaterStatus.DRY,
                true,
                Instant.parse("2026-05-02T08:00:00Z")
        );
        UUID id = farmField.getId();

        when(farmFieldRepository.findById(id)).thenReturn(Optional.of(farmField));

        FarmFieldResponse response = farmFieldService.getFarmField(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.areaName()).isEqualTo("West Plot");
        assertThat(response.pestReported()).isTrue();
    }

    @Test
    void getFarmFieldThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(farmFieldRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmFieldService.getFarmField(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farm field not found");
    }

    @Test
    void updateFarmFieldUpdatesAndReturnsFieldWhenFarmerExists() {
        UUID originalFarmerId = UUID.randomUUID();
        UUID updatedFarmerId = UUID.randomUUID();
        FarmField farmField = persistedFarmField(
                originalFarmerId,
                "Old Area",
                new BigDecimal("1.00"),
                CropStage.SEEDING,
                WaterStatus.UNKNOWN,
                false,
                null
        );
        UUID id = farmField.getId();
        Instant lastVisitAt = Instant.parse("2026-05-03T09:30:00Z");
        UpdateFarmFieldRequest request = new UpdateFarmFieldRequest(
                updatedFarmerId,
                "Updated Area",
                new BigDecimal("4.25"),
                CropStage.HARVEST_READY,
                WaterStatus.FLOODED,
                true,
                lastVisitAt
        );

        when(farmFieldRepository.findById(id)).thenReturn(Optional.of(farmField));
        when(farmerRepository.existsById(updatedFarmerId)).thenReturn(true);
        when(farmFieldRepository.save(farmField)).thenAnswer(invocation -> {
            FarmField savedFarmField = invocation.getArgument(0);
            savedFarmField.preUpdate();
            return savedFarmField;
        });

        FarmFieldResponse response = farmFieldService.updateFarmField(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.farmerId()).isEqualTo(updatedFarmerId);
        assertThat(response.areaName()).isEqualTo("Updated Area");
        assertThat(response.areaSize()).isEqualByComparingTo("4.25");
        assertThat(response.cropStage()).isEqualTo(CropStage.HARVEST_READY);
        assertThat(response.waterStatus()).isEqualTo(WaterStatus.FLOODED);
        assertThat(response.pestReported()).isTrue();
        assertThat(response.lastVisitAt()).isEqualTo(lastVisitAt);
        verify(farmFieldRepository).save(farmField);
    }

    @Test
    void updateFarmFieldThrowsWhenFieldNotFound() {
        UUID id = UUID.randomUUID();
        UpdateFarmFieldRequest request = updateRequest(UUID.randomUUID());

        when(farmFieldRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmFieldService.updateFarmField(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farm field not found");

        verify(farmerRepository, never()).existsById(any(UUID.class));
        verify(farmFieldRepository, never()).save(any(FarmField.class));
    }

    @Test
    void updateFarmFieldThrowsWhenFarmerDoesNotExist() {
        UUID farmerId = UUID.randomUUID();
        FarmField farmField = persistedFarmField(
                UUID.randomUUID(),
                "Old Area",
                new BigDecimal("1.00"),
                CropStage.SEEDING,
                WaterStatus.UNKNOWN,
                false,
                null
        );
        UUID id = farmField.getId();
        UpdateFarmFieldRequest request = updateRequest(farmerId);

        when(farmFieldRepository.findById(id)).thenReturn(Optional.of(farmField));
        when(farmerRepository.existsById(farmerId)).thenReturn(false);

        assertThatThrownBy(() -> farmFieldService.updateFarmField(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farmer not found");

        verify(farmFieldRepository, never()).save(any(FarmField.class));
    }

    private UpdateFarmFieldRequest updateRequest(UUID farmerId) {
        return new UpdateFarmFieldRequest(
                farmerId,
                "Updated Area",
                new BigDecimal("4.25"),
                CropStage.HARVEST_READY,
                WaterStatus.FLOODED,
                true,
                Instant.parse("2026-05-03T09:30:00Z")
        );
    }

    private FarmField persistedFarmField(
            UUID farmerId,
            String areaName,
            BigDecimal areaSize,
            CropStage cropStage,
            WaterStatus waterStatus,
            boolean pestReported,
            Instant lastVisitAt
    ) {
        FarmField farmField = new FarmField(
                farmerId,
                areaName,
                areaSize,
                cropStage,
                waterStatus,
                pestReported,
                lastVisitAt
        );
        farmField.prePersist();
        return farmField;
    }
}
