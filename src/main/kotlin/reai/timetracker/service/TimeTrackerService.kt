package reai.timetracker.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reai.timetracker.entity.TimeEntry
import reai.timetracker.repository.TimeEntryRepository
import java.math.BigDecimal
import java.math.RoundingMode
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
    fun startTimer(projectId: Long, employeeId: Long): TimeEntry {

        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNull(employeeId)
        require(!activeEntry.isPresent) { "Active timer already exists for employee $employeeId" }

        val currentTime = LocalDateTime.now()
        val currentTimeMillis = System.currentTimeMillis()

        return repository.save(
            TimeEntry(employeeId, projectId).apply {
                this.employeeId = employeeId
                this.projectId = projectId
                startTime = currentTime
                startTimeMillis = currentTimeMillis
                entryDate = currentTime.toLocalDate()
            }
        ).also {
            logger.info("Timer started - Employee: $employeeId, Timestamp: $currentTimeMillis")
        }
    }

    @Transactional
    fun stopTimer(employeeId: Long, accessToken: String?): TimeEntry? {
        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNull(employeeId)
            .orElse(null) ?: return null

        val currentTime = LocalDateTime.now()
        val currentTimeMillis = System.currentTimeMillis()

        val wasSynced = activeEntry.synced

        val stoppedEntry = activeEntry.apply {
            endTime = currentTime
            endTimeMillis = currentTimeMillis
            calculateDurationMetrics()
            if (!wasSynced) {
                synced = false
            }
        }.let(repository::save)

        val synced = if (!wasSynced) {
            val result = syncEntryWithReai(stoppedEntry, accessToken)
            if (!result) {
                logger.warn("Immediate sync failed for entry ${stoppedEntry.id}; it will remain marked as unsynced")
            }
            result
        } else {
            logger.debug("Entry ${stoppedEntry.id} already synced; skipping immediate sync")
            true
        }

        autoSyncProjectDaily(stoppedEntry, accessToken)

        return stoppedEntry.also {
            logger.info("Timer stopped - Employee: $employeeId, Duration: ${it.totalMilliseconds}ms, synced=$synced")
        }
    }

    @Transactional
    fun createInstantEntry(employeeId: Long, projectId: Long, accessToken: String?): TimeEntry {
        val now = LocalDateTime.now()
        val nowMillis = System.currentTimeMillis()

        val entry = TimeEntry(employeeId, projectId).apply {
            this.employeeId = employeeId
            this.projectId = projectId
            startTime = now
            endTime = now
            entryDate = now.toLocalDate()
            startTimeMillis = nowMillis
            endTimeMillis = nowMillis
            calculateDurationMetrics()
            synced = false
        }

        val persisted = repository.save(entry)
        val synced = syncEntryWithReai(persisted, accessToken)
        if (!synced) {
            logger.warn("Instant entry ${persisted.id} failed to sync immediately")
        }

        logger.info("Created instant entry for employee $employeeId on project $projectId (synced=$synced)")
        return persisted
    }

    @Transactional
    fun stopTimerWithoutSync(employeeId: Long): TimeEntry? {
        val activeEntry = repository.findByEmployeeIdAndEndTimeIsNull(employeeId)
            .orElse(null) ?: return null

        val currentTime = LocalDateTime.now()
        val currentTimeMillis = System.currentTimeMillis()

        return activeEntry.apply {
            endTime = currentTime
            endTimeMillis = currentTimeMillis
            calculateDurationMetrics()
        }.let(repository::save).also {
            logger.info("Timer stopped without sync - Employee: $employeeId, Duration: ${it.totalMilliseconds}ms")
        }
    }

    fun getCurrentTimer(employeeId: Long) =
        repository.findByEmployeeIdAndEndTimeIsNull(employeeId)

    fun getTimeEntries(employeeId: Long, accessToken: String?): List<TimeEntry> {
        validateEmployeeAccess(employeeId, accessToken)
        return repository.findByEmployeeIdOrderByStartTimeDesc(employeeId)
    }

    @Transactional
    fun updateEntry(id: Long): TimeEntry? =
        repository.findById(id).map { entry ->
            entry.synced = false
            repository.save(entry)
        }.orElse(null)

    @Transactional
    fun syncTodayAggregated(employeeId: Long, projectId: Long, accessToken: String?): Boolean {
        val today = LocalDate.now()
        val completedEntries = repository.findEntriesToday(employeeId, today)
            .filter { it.endTime != null && !it.synced }

        if (completedEntries.isEmpty()) {
            logger.warn("No completed entries to sync for employee $employeeId")
            return false
        }

        val projectEntries = completedEntries.filter { it.projectId == projectId }

        if (projectEntries.isEmpty()) {
            logger.warn("No completed entries to sync for employee $employeeId on project $projectId")
            return false
        }

        val totalHours = projectEntries.sumOf { it.totalHours ?: 0.0 }
        val syncData = mapOf(
            "employeeId" to employeeId,
            "projectId" to projectId,
            "date" to today,
            "totalHours" to totalHours,
            "entryCount" to projectEntries.size
        )

        val synced = syncDailyTotalToCore(syncData, accessToken)
        if (synced) {
            markEntriesAsSynced(projectEntries)
            logger.info("Synced daily total for project $projectId: ${totalHours}h (${projectEntries.size} entries)")
        } else {
            logger.error("Failed to sync daily total for project: $projectId")
        }

        return synced
    }

    @Transactional
    fun syncTodayAllDataAggregated(employeeId: Long, accessToken: String?): Boolean {
        val today = LocalDate.now()
        val completedEntries = repository.findEntriesToday(employeeId, today)
            .filter { it.endTime != null && !it.synced }

        if (completedEntries.isEmpty()) {
            logger.warn("No completed entries to sync for employee $employeeId")
            return false
        }

        val projectDailyTotals = completedEntries.groupBy { it.projectId }
            .mapValues { (_, entries) -> entries.sumOf { it.totalHours ?: 0.0 } }

        var allSynced = true

        projectDailyTotals.forEach { (projectId, dailyTotal) ->
            val syncData = mapOf(
                "employeeId" to employeeId,
                "projectId" to projectId,
                "date" to today,
                "totalHours" to dailyTotal,
                "entryCount" to completedEntries.count { it.projectId == projectId }
            )

            if (syncDailyTotalToCore(syncData, accessToken)) {
                val projectEntries = completedEntries.filter { it.projectId == projectId }
                markEntriesAsSynced(projectEntries)
                logger.info("Synced daily total for $projectId: ${dailyTotal}h (${projectEntries.size} entries)")
            } else {
                allSynced = false
                logger.error("Failed to sync daily total for project: $projectId")
            }
        }

        return allSynced
    }

    @Transactional
    fun syncSpecificProject(employeeId: Long, projectId: Long, date: LocalDate, accessToken: String): Boolean {
        val projectEntries = repository.findByEmployeeIdAndProjectIdAndEntryDateAndSyncedIsFalse(employeeId, projectId, date)
            .filter { it.endTime != null && !it.synced }

        if (projectEntries.isEmpty()) {
            logger.info("No unsynced entries for project $projectId on $date")
            return true
        }

        val totalHours = projectEntries.sumOf { it.totalHours ?: 0.0 }
        val syncData = mapOf(
            "employeeId" to employeeId,
            "projectId" to projectId,
            "date" to date,
            "totalHours" to totalHours,
            "entryCount" to projectEntries.size
        )

        return if (syncDailyTotalToCore(syncData, accessToken)) {
            markEntriesAsSynced(projectEntries)
            logger.info("Synced project $projectId on $date: ${totalHours}h")
            true
        } else {
            logger.error("Failed to sync project $projectId on $date")
            false
        }
    }

    fun getAllTimeEntries(employeeId: Long): List<TimeEntry> =
        repository.findByEmployeeIdOrderByStartTimeDesc(employeeId)


    private fun TimeEntry.calculateDurationMetrics() {
        val duration = when {
            startTimeMillis != null && endTimeMillis != null -> endTimeMillis!! - startTimeMillis!!
            startTime != null && endTime != null -> Duration.between(startTime, endTime).toMillis()
            else -> 0L
        }
        totalMilliseconds = duration
        totalHours = millisecondsToHours(duration)
    }

    private fun syncDailyTotalToCore(syncData: Map<String, Any>, accessToken: String?): Boolean =
        try {
            val aggregatedEntry = TimeEntry(
                employeeId = syncData["employeeId"] as Long,
                projectId = syncData["projectId"] as Long
            ).apply {
                entryDate = syncData["date"] as LocalDate
                totalHours = syncData["totalHours"] as Double
                totalMilliseconds = ((syncData["totalHours"] as Double) * 3600000).toLong()
                synced = false
                startTime = (syncData["date"] as LocalDate).atStartOfDay()
                endTime = (syncData["date"] as LocalDate).atTime(23, 59, 59)
            }
            reaiApiService.syncTimeEntry(aggregatedEntry, accessToken)
        } catch (e: Exception) {
            logger.error("Failed to sync daily total to core: ${e.message}", e)
            false
        }

    private fun syncEntryWithReai(entry: TimeEntry, accessToken: String?): Boolean {
        if (accessToken.isNullOrBlank()) {
            logger.warn("Access token is missing; skipping sync for entry ${entry.id}")
            return false
        }

        return try {
            val synced = reaiApiService.syncTimeEntry(entry, accessToken)
            if (synced) {
                entry.synced = true
                repository.save(entry)
                logger.info("Synced entry ${entry.id} to ReAI")
            } else {
                logger.warn("ReAI sync returned false for entry ${entry.id}")
            }
            synced
        } catch (e: Exception) {
            logger.error("Failed to sync entry ${entry.id}: ${e.message}", e)
            false
        }
    }

    private fun autoSyncProjectDaily(stoppedEntry: TimeEntry, accessToken: String?) {
        try {
            val employeeId = stoppedEntry.employeeId
            val date = stoppedEntry.entryDate

            val allProjectEntriesForDay = repository
                .findByEmployeeIdAndProjectIdAndEntryDateAndSyncedIsFalse(employeeId, stoppedEntry.projectId, date)
                .filter { it.endTime != null }

            if (allProjectEntriesForDay.isEmpty()) {
                logger.debug("No completed entries to auto-sync for employee $employeeId on ${stoppedEntry.projectId}")
                return
            }

            val totalHours = allProjectEntriesForDay.sumOf { it.totalHours ?: 0.0 }
            val syncData = mapOf(
                "employeeId" to employeeId,
                "projectId" to stoppedEntry.projectId,
                "date" to date,
                "totalHours" to totalHours,
                "entryCount" to allProjectEntriesForDay.size
            )

            if (syncDailyTotalToCore(syncData, accessToken)) {
                markEntriesAsSynced(allProjectEntriesForDay)
            }
        } catch (e: Exception) {
            logger.error("Auto-sync error: ${e.message}", e)
        }
    }

    private fun markEntriesAsSynced(entries: List<TimeEntry>) {
        entries.forEach { it.synced = true }
        repository.saveAll(entries)
    }

    private fun validateEmployeeAccess(employeeId: Long, accessToken: String?) {
        val employee = reaiApiService.getEmployee(employeeId, accessToken)
        checkNotNull(employee) { "Employee not found or access denied" }
    }

    private fun millisecondsToHours(milliseconds: Long): Double =
        BigDecimal(milliseconds)
            .divide(BigDecimal(3600000), 3, RoundingMode.HALF_UP)
            .toDouble()
}
