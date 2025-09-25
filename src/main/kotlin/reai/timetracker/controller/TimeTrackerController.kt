package reai.timetracker.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reai.timetracker.entity.TimeEntry
import reai.timetracker.service.TimeTrackerService

@RestController
@RequestMapping("/api/time")
@CrossOrigin(origins = ["*"])
class TimeTrackerController(
    private val timeTrackerService: TimeTrackerService
) {

    private val logger = LoggerFactory.getLogger(TimeTrackerController::class.java)

    @PostMapping("/start")
    fun startTimer(
        @RequestParam projectName: String,
        @RequestParam employeeId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<TimeEntry> {
        logger.debug("Starting timer for project: {}, employeeId: {}, tenantId: {}", projectName, employeeId, tenantId)
        return try {
            val entry = timeTrackerService.startTimer(projectName, employeeId, tenantId)
            ResponseEntity.ok(entry)
        } catch (e: Exception) {
            logger.error("Failed to start timer: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PostMapping("/stop")
    fun stopTimer(
        @RequestParam employeeId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<TimeEntry> {
        logger.debug("Stopping timer for employeeId: {}, tenantId: {}", employeeId, tenantId)
        return try {
            val entry = timeTrackerService.stopTimer(employeeId, tenantId)

            if (entry != null) {
                logger.info("Timer stopped successfully for employeeId: {}, tenantId: {}", employeeId, tenantId)

                try {
                    val syncedCount = timeTrackerService.syncTodayAggregated(tenantId, 1)
                    logger.info("Auto-sync completed: {} entries synced for tenantId: {}", syncedCount, tenantId)
                } catch (syncException: Exception) {
                    logger.warn("Auto-sync failed after stopping timer: {}", syncException.message)
                }

                ResponseEntity.ok(entry)
            } else {
                logger.warn("No active timer found for employeeId: {}, tenantId: {}", employeeId, tenantId)
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Failed to stop timer: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @GetMapping("/current")
    fun getCurrentTimer(
        @RequestParam employeeId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<TimeEntry> {
        logger.debug("Getting current timer for employeeId: {}, tenantId: {}", employeeId, tenantId)
        return try {
            timeTrackerService.getCurrentTimer(employeeId, tenantId)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
        } catch (e: Exception) {
            logger.error("Failed to get current timer: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @GetMapping("/entries")
    fun getTimeEntries(
        @RequestParam employeeId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<List<TimeEntry>> {
        logger.debug("Getting time entries for employeeId: {}, tenantId: {}", employeeId, tenantId)
        return try {
            val entries = timeTrackerService.getTimeEntries(employeeId, tenantId)
            ResponseEntity.ok(entries)
        } catch (e: Exception) {
            logger.error("Failed to get time entries: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PutMapping("/entries/{id}")
    fun updateEntry(
        @PathVariable id: Long,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) billable: Boolean?,
        @RequestParam tenantId: Long
    ): ResponseEntity<TimeEntry> {
        logger.debug("Updating time entry with id: {}, tenantId: {}", id, tenantId)
        return try {
            val updated = timeTrackerService.updateEntry(id, description, billable, tenantId)
            if (updated != null) ResponseEntity.ok(updated) else ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to update time entry: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PostMapping("/sync")
    fun syncToReai(@RequestParam tenantId: Long): ResponseEntity<String> {
        logger.debug("Syncing time entries to ReAI for tenantId: {}", tenantId)
        return try {
            val synced = timeTrackerService.syncTodayAggregated(tenantId, 1)
            ResponseEntity.ok("Synced $synced entries to ReAI")
        } catch (e: Exception) {
            logger.error("Failed to sync time entries: {}", e.message)
            ResponseEntity.status(500).body("Failed to sync time entries")
        }
    }

    @GetMapping("/entries/all")
    fun getAllTimeEntries(@RequestParam tenantId: Long): ResponseEntity<List<TimeEntry>> {
        logger.debug("Getting all time entries for tenantId: {}", tenantId)
        return try {
            val entries = timeTrackerService.getAllTimeEntries(tenantId)
            ResponseEntity.ok(entries)
        } catch (e: Exception) {
            logger.error("Failed to get all time entries: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    data class TenantDto(
        val id: Long,
        val name: String
    )
}