package reai.timetracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public TimeEntry startTimer(String projectName, Long employeeId) {
        repository.findByEmployeeIdAndEndTimeIsNull(employeeId)
                .ifPresent(this::stopActiveTimer);

        TimeEntry entry = new TimeEntry(projectName, employeeId);
        return repository.save(entry);
    }

    public TimeEntry stopTimer(Long employeeId) {
        Optional<TimeEntry> activeEntry = repository.findByEmployeeIdAndEndTimeIsNull(employeeId);

        if (activeEntry.isPresent()) {
            TimeEntry entry = activeEntry.get();
            entry.stop();
            return repository.save(entry);
        }

        return null;
    }

    public Optional<TimeEntry> getCurrentTimer(Long employeeId) {
        return repository.findByEmployeeIdAndEndTimeIsNull(employeeId);
    }

    public List<TimeEntry> getTimeEntries(Long employeeId) {
        return repository.findByEmployeeIdOrderByStartTimeDesc(employeeId);
    }

    public TimeEntry updateEntry(Long id, String description, Boolean billable) {
        return repository.findById(id)
                .map(entry -> {
                    if (description != null) entry.setDescription(description);
                    if (billable != null) entry.setBillable(billable);
                    entry.setSynced(false); // Mark for re-sync
                    return repository.save(entry);
                })
                .orElse(null);
    }

    public int syncUnsyncedEntries() {
        List<TimeEntry> unsyncedEntries = repository.findUnsyncedEntries();
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
}