package reai.timetracker.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reai.timetracker.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByEmployeeIdAndTenantIdOrderByStartTimeDesc(Long employeeId, Long tenantId);

    Optional<TimeEntry> findByEmployeeIdAndEndTimeIsNullAndTenantId(Long employeeId, Long tenantId);

    List<TimeEntry> findByEmployeeIdAndTenantIdAndStartTimeBetween(
            Long employeeId,
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<TimeEntry> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT te FROM TimeEntry te WHERE te.synced = false AND te.tenantId = :tenantId")
    List<TimeEntry> findUnsyncedEntriesByTenantId(@Param("tenantId") Long tenantId);

    List<TimeEntry> findByProjectNameContainingIgnoreCaseAndTenantId(String projectName, Long tenantId);

    List<TimeEntry> findByTenantIdOrderByStartTimeDesc(Long tenantId);

    @Deprecated
    List<TimeEntry> findByEmployeeIdOrderByStartTimeDesc(Long employeeId);

    @Deprecated
    Optional<TimeEntry> findByEmployeeIdAndEndTimeIsNull(Long employeeId);

    @Deprecated
    @Query("SELECT te FROM TimeEntry te WHERE te.synced = false")
    List<TimeEntry> findUnsyncedEntries();
}