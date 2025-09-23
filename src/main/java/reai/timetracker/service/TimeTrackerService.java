package reai.timetracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reai.timetracker.entity.Employee;
import reai.timetracker.entity.TimeEntry;
import reai.timetracker.repository.TimeEntryRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TimeTrackerService {

    @Autowired
    private TimeEntryRepository repository;

    @Autowired
    private ReaiApiService reaiApiService;

    public TimeEntry startTimer(String projectName, Long employeeId, Long tenantId) {
        Employee employee = reaiApiService.getEmployee(employeeId, tenantId);
        if (employee == null) {
            throw new SecurityException("Employee not found or access denied");
        }

        repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)
                .ifPresent(this::stopActiveTimer);

        TimeEntry entry = new TimeEntry(projectName, employeeId);
        entry.setTenantId(tenantId);
        return repository.save(entry);
    }

    public TimeEntry stopTimer(Long employeeId, Long tenantId) {
        Optional<TimeEntry> activeEntry =
                repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId);

        if (activeEntry.isPresent()) {
            TimeEntry entry = activeEntry.get();
            entry.stop();
            return repository.save(entry);
        }

        return null;
    }

    public Optional<TimeEntry> getCurrentTimer(Long employeeId, Long tenantId) {
        return repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId);
    }

    public List<TimeEntry> getTimeEntries(Long employeeId, Long tenantId) {
        Employee employee = reaiApiService.getEmployee(employeeId, tenantId);
        if (employee == null) {
            throw new SecurityException("Employee not found or access denied");
        }

        return repository.findByEmployeeIdAndTenantIdOrderByStartTimeDesc(employeeId, tenantId);
    }

    public TimeEntry updateEntry(Long id, String description, Boolean billable, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .map(entry -> {
                    if (description != null) entry.setDescription(description);
                    if (billable != null) entry.setBillable(billable);
                    entry.setSynced(false);
                    return repository.save(entry);
                })
                .orElse(null);
    }

    public int syncUnsyncedEntries(Long tenantId) {
        List<TimeEntry> unsyncedEntries = repository.findUnsyncedEntriesByTenantId(tenantId);
        int syncedCount = 0;

        for (TimeEntry entry : unsyncedEntries) {
            if (reaiApiService.syncTimeEntry(entry)) {
                entry.setSynced(true);
                repository.save(entry);
                syncedCount++;
            }
        }

        return syncedCount;
    }

    private void stopActiveTimer(TimeEntry entry) {
        entry.stop();
        repository.save(entry);
    }

    public List<TimeEntry> getAllTimeEntries(Long tenantId) {
        return repository.findByTenantIdOrderByStartTimeDesc(tenantId);
    }
}