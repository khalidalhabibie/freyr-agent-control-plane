package com.khalid.freyr.farmer;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FarmerServiceTest {

    @Mock
    private FarmerRepository farmerRepository;

    @InjectMocks
    private FarmerService farmerService;

    @Test
    void createFarmerSavesAndReturnsFarmer() {
        CreateFarmerRequest request = new CreateFarmerRequest(
                "Budi Santoso",
                "08123456789",
                "Sukamaju",
                "Cibadak"
        );

        when(farmerRepository.save(any(Farmer.class))).thenAnswer(invocation -> {
            Farmer farmer = invocation.getArgument(0);
            farmer.prePersist();
            return farmer;
        });

        FarmerResponse response = farmerService.createFarmer(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Budi Santoso");
        assertThat(response.phoneNumber()).isEqualTo("08123456789");
        assertThat(response.village()).isEqualTo("Sukamaju");
        assertThat(response.district()).isEqualTo("Cibadak");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(farmerRepository).save(any(Farmer.class));
    }

    @Test
    void getFarmersReturnsAllFarmers() {
        Farmer farmer = persistedFarmer("Siti Aminah", "08234567890", "Wanasari", "Cikarang");

        when(farmerRepository.findAll()).thenReturn(List.of(farmer));

        List<FarmerResponse> responses = farmerService.getFarmers();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().name()).isEqualTo("Siti Aminah");
        assertThat(responses.getFirst().phoneNumber()).isEqualTo("08234567890");
    }

    @Test
    void getFarmerReturnsFarmerWhenFound() {
        Farmer farmer = persistedFarmer("Agus Rahman", "08345678901", "Mekarsari", "Subang");
        UUID id = farmer.getId();

        when(farmerRepository.findById(id)).thenReturn(Optional.of(farmer));

        FarmerResponse response = farmerService.getFarmer(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Agus Rahman");
    }

    @Test
    void getFarmerThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(farmerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmerService.getFarmer(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farmer not found");
    }

    @Test
    void updateFarmerUpdatesAndReturnsFarmer() {
        Farmer farmer = persistedFarmer("Old Name", "08000000000", "Old Village", "Old District");
        UUID id = farmer.getId();

        UpdateFarmerRequest request = new UpdateFarmerRequest(
                "New Name",
                "08999999999",
                "New Village",
                "New District"
        );

        when(farmerRepository.findById(id)).thenReturn(Optional.of(farmer));
        when(farmerRepository.save(farmer)).thenAnswer(invocation -> {
            Farmer savedFarmer = invocation.getArgument(0);
            savedFarmer.preUpdate();
            return savedFarmer;
        });

        FarmerResponse response = farmerService.updateFarmer(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.phoneNumber()).isEqualTo("08999999999");
        assertThat(response.village()).isEqualTo("New Village");
        assertThat(response.district()).isEqualTo("New District");
        verify(farmerRepository).save(farmer);
    }

    @Test
    void updateFarmerThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        UpdateFarmerRequest request = new UpdateFarmerRequest(
                "New Name",
                "08999999999",
                "New Village",
                "New District"
        );

        when(farmerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmerService.updateFarmer(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farmer not found");
    }

    private Farmer persistedFarmer(String name, String phoneNumber, String village, String district) {
        Farmer farmer = new Farmer(name, phoneNumber, village, district);
        farmer.prePersist();
        return farmer;
    }
}
