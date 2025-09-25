package reai.timetracker.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reai.timetracker.entity.TimeEntry
import java.time.LocalDate
import java.util.*

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, Long> {

    fun findByEmployeeIdAndEndTimeIsNullAndTenantId(
        employeeId: Long,
        tenantId: Long
    ): Optional<TimeEntry>

    fun findByEmployeeIdAndTenantIdOrderByStartTimeDesc(
        employeeId: Long,
        tenantId: Long
    ): List<TimeEntry>

    fun findByIdAndTenantId(
        id: Long,
        tenantId: Long
    ): Optional<TimeEntry>

    fun findUnsyncedEntriesByTenantId(tenantId: Long): List<TimeEntry>

    fun findByTenantIdOrderByStartTimeDesc(tenantId: Long): List<TimeEntry>

    @Query("""
    SELECT e FROM TimeEntry e
    WHERE e.tenantId = :tenantId
      AND e.employeeId = :employeeId
      AND e.entryDate = :today
""")
    fun findEntriesToday(
        @Param("tenantId") tenantId: Long,
        @Param("employeeId") employeeId: Long,
        @Param("today") today: LocalDate
    ): List<TimeEntry>
}
