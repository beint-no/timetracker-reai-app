package reai.timetracker.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import reai.timetracker.entity.TimeEntry
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
}
