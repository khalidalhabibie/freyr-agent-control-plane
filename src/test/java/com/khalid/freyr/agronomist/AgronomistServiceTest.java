package com.khalid.freyr.agronomist;

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
class AgronomistServiceTest {

    @Mock
    private AgronomistRepository agronomistRepository;

    @InjectMocks
    private AgronomistService agronomistService;

    @Test
    void createAgronomistSavesAndReturnsAgronomist() {
        CreateAgronomistRequest request = new CreateAgronomistRequest(
                "Dewi Lestari",
                "08123456780",
                "Subang",
                6,
                AvailabilityStatus.AVAILABLE
        );

        when(agronomistRepository.save(any(Agronomist.class))).thenAnswer(invocation -> {
            Agronomist agronomist = invocation.getArgument(0);
            agronomist.prePersist();
            return agronomist;
        });

        AgronomistResponse response = agronomistService.createAgronomist(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Dewi Lestari");
        assertThat(response.phoneNumber()).isEqualTo("08123456780");
        assertThat(response.assignedDistrict()).isEqualTo("Subang");
        assertThat(response.maxDailyVisit()).isEqualTo(6);
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(agronomistRepository).save(any(Agronomist.class));
    }

    @Test
    void getAgronomistsReturnsAllAgronomists() {
        Agronomist agronomist = persistedAgronomist(
                "Raka Pratama",
                "08234567891",
                "Cianjur",
                5,
                AvailabilityStatus.UNAVAILABLE
        );

        when(agronomistRepository.findAll()).thenReturn(List.of(agronomist));

        List<AgronomistResponse> responses = agronomistService.getAgronomists();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().name()).isEqualTo("Raka Pratama");
        assertThat(responses.getFirst().availabilityStatus()).isEqualTo(AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void getAgronomistReturnsAgronomistWhenFound() {
        Agronomist agronomist = persistedAgronomist(
                "Maya Sari",
                "08345678912",
                "Garut",
                4,
                AvailabilityStatus.ON_LEAVE
        );
        UUID id = agronomist.getId();

        when(agronomistRepository.findById(id)).thenReturn(Optional.of(agronomist));

        AgronomistResponse response = agronomistService.getAgronomist(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Maya Sari");
        assertThat(response.assignedDistrict()).isEqualTo("Garut");
    }

    @Test
    void getAgronomistThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(agronomistRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agronomistService.getAgronomist(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Agronomist not found");
    }

    @Test
    void updateAgronomistUpdatesAndReturnsAgronomist() {
        Agronomist agronomist = persistedAgronomist(
                "Old Name",
                "08000000000",
                "Old District",
                3,
                AvailabilityStatus.UNAVAILABLE
        );
        UUID id = agronomist.getId();
        UpdateAgronomistRequest request = new UpdateAgronomistRequest(
                "New Name",
                "08999999999",
                "New District",
                7,
                AvailabilityStatus.AVAILABLE
        );

        when(agronomistRepository.findById(id)).thenReturn(Optional.of(agronomist));
        when(agronomistRepository.save(agronomist)).thenAnswer(invocation -> {
            Agronomist savedAgronomist = invocation.getArgument(0);
            savedAgronomist.preUpdate();
            return savedAgronomist;
        });

        AgronomistResponse response = agronomistService.updateAgronomist(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.phoneNumber()).isEqualTo("08999999999");
        assertThat(response.assignedDistrict()).isEqualTo("New District");
        assertThat(response.maxDailyVisit()).isEqualTo(7);
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        verify(agronomistRepository).save(agronomist);
    }

    @Test
    void updateAgronomistThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        UpdateAgronomistRequest request = new UpdateAgronomistRequest(
                "New Name",
                "08999999999",
                "New District",
                7,
                AvailabilityStatus.AVAILABLE
        );

        when(agronomistRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agronomistService.updateAgronomist(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Agronomist not found");
    }

    private Agronomist persistedAgronomist(
            String name,
            String phoneNumber,
            String assignedDistrict,
            Integer maxDailyVisit,
            AvailabilityStatus availabilityStatus
    ) {
        Agronomist agronomist = new Agronomist(
                name,
                phoneNumber,
                assignedDistrict,
                maxDailyVisit,
                availabilityStatus
        );
        agronomist.prePersist();
        return agronomist;
    }
}
