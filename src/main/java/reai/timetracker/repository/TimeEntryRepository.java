package reai.timetracker.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import reai.timetracker.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByEmployeeIdOrderByStartTimeDesc(Long employeeId);

    Optional<TimeEntry> findByEmployeeIdAndEndTimeIsNull(Long employeeId);

    List<TimeEntry> findByEmployeeIdAndStartTimeBetween(
            Long employeeId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT te FROM TimeEntry te WHERE te.synced = false")
    List<TimeEntry> findUnsyncedEntries();

    List<TimeEntry> findByProjectNameContainingIgnoreCase(String projectName);
}
