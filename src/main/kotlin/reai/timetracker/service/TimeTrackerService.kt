package reai.timetracker.service

import org.springframework.stereotype.Service
import reai.timetracker.entity.Employee
import reai.timetracker.entity.TimeEntry
import reai.timetracker.repository.TimeEntryRepository

@Service
class TimeTrackerService(
    private val repository: TimeEntryRepository,
    private val reaiApiService: ReaiApiService
) {

    fun startTimer(projectName: String, employeeId: Long, tenantId: Long): TimeEntry {
        val employee: Employee? = reaiApiService.getEmployee(employeeId, tenantId)
        if (employee == null) {
            throw SecurityException("Employee not found or access denied")
        }

        repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)
                .ifPresent { stopActiveTimer(it) }

        val entry = TimeEntry(employeeId, projectName).apply {
            this.tenantId = tenantId
        }
        return repository.save(entry)
    }

    fun stopTimer(employeeId: Long, tenantId: Long): TimeEntry? {
        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)
        return if (activeEntry.isPresent) {
            val entry = activeEntry.get()
            entry.stop()
            repository.save(entry)
        } else {
            null
        }
    }

    fun getCurrentTimer(employeeId: Long, tenantId: Long) =
            repository.findByEmployeeIdAndEndTimeIsNullAndTenantId(employeeId, tenantId)

    fun getTimeEntries(employeeId: Long, tenantId: Long): List<TimeEntry> {
        val employee: Employee? = reaiApiService.getEmployee(employeeId, tenantId)
        if (employee == null) {
            throw SecurityException("Employee not found or access denied")
        }
        return repository.findByEmployeeIdAndTenantIdOrderByStartTimeDesc(employeeId, tenantId)
    }

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

    fun syncUnsyncedEntries(tenantId: Long): Int {
        val unsyncedEntries = repository.findUnsyncedEntriesByTenantId(tenantId)
        var syncedCount = 0
        for (entry in unsyncedEntries) {
            if (reaiApiService.syncTimeEntry(entry)) {
                entry.synced = true
                repository.save(entry)
                syncedCount++
            }
        }
        return syncedCount
    }

    private fun stopActiveTimer(entry: TimeEntry) {
        entry.stop()
        repository.save(entry)
    }

    fun getAllTimeEntries(tenantId: Long): List<TimeEntry> =
            repository.findByTenantIdOrderByStartTimeDesc(tenantId)
}
