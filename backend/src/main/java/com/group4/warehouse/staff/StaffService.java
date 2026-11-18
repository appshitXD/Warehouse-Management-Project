package com.group4.warehouse.staff;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/**
 * Business rules for staff:
 * - at least one phone, no phone listed twice,
 * - warehouse and supervisor are optional,
 * - no supervision cycle (the database only blocks supervising yourself:
 *   CHECK CHK_STAFF_SUPERVISOR; a longer loop A -> B -> A is checked here).
 */
@Service
public class StaffService {

    private final StaffRepository repository;

    public StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    public List<Staff> list(String q, String warehouseId) {
        return repository.findAll(Text.trimToNull(q), Text.trimToNull(warehouseId));
    }

    public Staff get(String id) {
        return repository.findById(Text.trim(id))
                .orElseThrow(() -> ApiException.notFound("Staff member " + id + " was not found."));
    }

    public Staff create(CreateStaffRequest request) {
        String id = Text.trim(request.id());
        String supervisorId = Text.trimToNull(request.supervisorId());
        checkNoCycle(id, supervisorId);
        Staff staff = new Staff(
                id,
                Text.trim(request.name()),
                Text.trim(request.address()),
                Text.trimToNull(request.warehouseId()),
                supervisorId,
                cleanPhones(request.phones()));
        repository.insert(staff);
        return get(id);
    }

    public Staff update(String id, UpdateStaffRequest request) {
        if (request.name() == null && request.address() == null && request.warehouseId() == null
                && request.supervisorId() == null && request.phones() == null) {
            throw ApiException.badRequest(
                    "Nothing to update: send name, address, warehouseId, supervisorId and/or phones.");
        }
        Staff current = get(id);                                     // 404 if unknown
        String supervisorId = Text.trim(request.supervisorId());     // "" = remove supervisor
        if (supervisorId != null && !supervisorId.isEmpty()) {
            checkNoCycle(current.id(), supervisorId);
        }
        repository.update(current.id(),
                Text.trim(request.name()),
                Text.trim(request.address()),
                Text.trim(request.warehouseId()),
                supervisorId,
                request.phones() == null ? null : cleanPhones(request.phones()));
        return get(current.id());
    }

    public void delete(String id) {
        if (!repository.delete(Text.trim(id))) {
            throw ApiException.notFound("Staff member " + id + " was not found.");
        }
    }

    /** Trims each phone and rejects duplicates (the key of STAFF_PHONE is Staff_ID + Phone). */
    private static List<String> cleanPhones(List<String> phones) {
        Set<String> seen = new HashSet<>();
        List<String> result = phones.stream().map(String::trim).toList();
        for (String phone : result) {
            if (!seen.add(phone)) {
                throw ApiException.badRequest("Phone " + phone + " is listed twice.");
            }
        }
        return result;
    }

    /** Walks up the supervisor chain from the new supervisor; reaching staffId means a loop. */
    private void checkNoCycle(String staffId, String supervisorId) {
        Set<String> visited = new HashSet<>();
        String current = supervisorId;
        while (current != null) {
            if (current.equals(staffId)) {
                throw ApiException.badRequest(
                        "A staff member cannot supervise themselves, directly or through others.");
            }
            if (!visited.add(current)) {
                return;   // a loop in existing data that does not involve staffId
            }
            current = repository.findSupervisorId(current).orElse(null);
        }
    }
}
