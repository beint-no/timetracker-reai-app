package reai.timetracker.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reai.timetracker.entity.EmployeesDto
import reai.timetracker.entity.TimeEntry
import reai.timetracker.repository.TimeEntryRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TimeTrackerService(
    private val repository: TimeEntryRepository,
    private val reaiApiService: ReaiApiService
) {

    private val logger = LoggerFactory.getLogger(TimeTrackerService::class.java)

    @Transactional
    fun startTimer(projectName: String, employeeId: Long, tenantId: Long): TimeEntry {
        if (projectName.isBlank()) {
            throw IllegalArgumentException("Project name cannot be empty")
        }
        val employee: EmployeesDto? = reaiApiService.getEmployee(employeeId, tenantId)
        if (employee == null) {
            throw SecurityException("Employee not found or access denied")
        }
        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)
        if (activeEntry.isPresent) {
            throw IllegalStateException("An active timer already exists for this employee")
        }
        val entry = TimeEntry(employeeId, projectName).apply {
            this.tenantId = tenantId
            this.startTime = LocalDateTime.now()
            this.entryDate = startTime.toLocalDate()
        }
        return repository.save(entry)
    }

    @Transactional
    fun stopTimer(employeeId: Long, tenantId: Long): TimeEntry? {
        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)
        if (activeEntry.isPresent) {
            val entry = activeEntry.get()
            entry.stop()
            entry.entryDate = entry.startTime.toLocalDate()
            return repository.save(entry)
        }
        return null
    }

    fun getCurrentTimer(employeeId: Long, tenantId: Long) =
        repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)

    fun getTimeEntries(employeeId: Long, tenantId: Long): List<TimeEntry> {
        val employee: EmployeesDto? = reaiApiService.getEmployee(employeeId, tenantId)
        if (employee == null) {
            throw SecurityException("Employee not found or access denied")
        }
        return repository.findByEmployeeIdAndTenantIdOrderByStartTimeDesc(employeeId, tenantId)
    }

    @Transactional
    fun updateEntry(id: Long, description: String?, billable: Boolean?, tenantId: Long): TimeEntry? {
        return repository.findByIdAndTenantId(id, tenantId)
            .map { entry ->
                description?.let { entry.description = it }
                billable?.let { entry.billable = it }
                entry.synced = false
                repository.save(entry)
            }
            .orElse(null)
    }

    @Transactional
    fun syncTodayAggregated(tenantId: Long, employeeId: Long): Boolean {
        val today = LocalDate.now()
        val entries = repository.findEntriesToday(tenantId, employeeId, today)
        if (entries.isEmpty()) return false

        val totalHours = entries.sumOf { e ->
            if (e.endTime != null) Duration.between(e.startTime, e.endTime).toMinutes() / 60.0 else 0.0
        }

        val mergedDescription = entries.mapNotNull { it.description }.joinToString(" | ")

        val earliestStart = entries.minOf { it.startTime }
        val latestEnd = entries.filter { it.endTime != null }
            .maxByOrNull { it.endTime!! }
            ?.endTime

        val aggregated = TimeEntry(
            projectName = entries.first().projectName,
            employeeId = employeeId,
            tenantId = tenantId
        ).apply {
            this.startTime = earliestStart
            this.endTime = latestEnd
            this.entryDate = today
            this.description = mergedDescription
            this.synced = false
        }

        return try {
            if (reaiApiService.syncTimeEntry(aggregated)) {
                entries.forEach { it.synced = true }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to sync aggregated entry for $employeeId: ${e.message}", e)
            false
        }
    }

    fun getAllTimeEntries(tenantId: Long): List<TimeEntry> =
        repository.findByTenantIdOrderByStartTimeDesc(tenantId)
}
