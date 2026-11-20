package com.group4.warehouse.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.group4.warehouse.common.ApiException;

/** Business rules that the database alone does not enforce. */
class StaffServiceTest {

    private final StaffRepository repository = mock(StaffRepository.class);
    private final StaffService service = new StaffService(repository);

    @Test
    void samePhoneTwiceIsRejected() {
        CreateStaffRequest request = new CreateStaffRequest(
                "ST98", "Test", "1 Test St", null, null, List.of("555-1111", " 555-1111 "));

        ApiException e = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.status());
        verify(repository, never()).insert(any());
    }

    @Test
    void supervisionCycleIsRejected() {
        // ST02 is supervised by ST01. Making ST02 the supervisor of ST01 would be a loop.
        when(repository.findById("ST01")).thenReturn(Optional.of(
                new Staff("ST01", "Boss", "Addr", "W01", null, List.of("555-0001"))));
        when(repository.findSupervisorId("ST02")).thenReturn(Optional.of("ST01"));

        UpdateStaffRequest request = new UpdateStaffRequest(null, null, null, "ST02", null);
        ApiException e = assertThrows(ApiException.class, () -> service.update("ST01", request));

        assertEquals(HttpStatus.BAD_REQUEST, e.status());
        verify(repository, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void emptyPatchIsRejected() {
        UpdateStaffRequest request = new UpdateStaffRequest(null, null, null, null, null);
        assertThrows(ApiException.class, () -> service.update("ST01", request));
    }
}
