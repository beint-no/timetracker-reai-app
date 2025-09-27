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
        @RequestParam projectId: Long,
        @RequestParam employeeId: Long,
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<TimeEntry> {
        return try {
            val token = extractToken(access_token, authHeader)

            val entry = timeTrackerService.startTimer(projectId, employeeId)
            ResponseEntity.ok(entry)
        } catch (e: Exception) {
            logger.error("Failed to start timer: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PostMapping("/stop")
    fun stopTimer(
        @RequestParam employeeId: Long,
        @RequestParam projectId: Long,
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<TimeEntry> {
        return try {
            val token = extractToken(access_token, authHeader)
            val tenantId = extractTenantIdFromToken(token)

            val entry = timeTrackerService.stopTimer(employeeId, token)

            if (entry != null) {
                val syncedCount = timeTrackerService.syncTodayAggregated(employeeId, projectId, token)
                logger.info("Auto-sync completed: {} entries synced for tenantId: {}", syncedCount, tenantId)
                ResponseEntity.ok(entry)
            } else {
                val newEntry = timeTrackerService.createInstantEntry(employeeId, projectId, token)
                logger.warn("No active timer to stop; created instant entry {} for tenantId: {}", newEntry.id, tenantId)
                ResponseEntity.ok(newEntry)
            }
        } catch (e: Exception) {
            logger.error("Failed to stop timer: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @GetMapping("/current")
    fun getCurrentTimer(
        @RequestParam employeeId: Long,
    ): ResponseEntity<TimeEntry> {
        logger.debug("Getting current timer for employeeId: {}", employeeId)
        return try {
            timeTrackerService.getCurrentTimer(employeeId)
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
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?

    ): ResponseEntity<List<TimeEntry>> {
        return try {
            val token = extractToken(access_token, authHeader)
            val entries = timeTrackerService.getTimeEntries(employeeId, token)
            ResponseEntity.ok(entries)
        } catch (e: Exception) {
            logger.error("Failed to get time entries: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PutMapping("/entries/{id}")
    fun updateEntry(
        @PathVariable id: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<TimeEntry> {
        logger.debug("Updating time entry with id: {}", id)
        return try {
            val updated = timeTrackerService.updateEntry(id)
            if (updated != null) ResponseEntity.ok(updated) else ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to update time entry: {}", e.message)
            ResponseEntity.status(500).body(null)
        }
    }

    @PostMapping("/sync")
    fun syncToReai(
        @RequestParam employeeId: Long,
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<String> {
        return try {
            val token = extractToken(access_token, authHeader)

            val synced = timeTrackerService.syncTodayAllDataAggregated(employeeId, token)
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

    private fun extractToken(queryToken: String?, authHeader: String?): String? {
        return when {
            authHeader?.startsWith("Bearer ") == true ->
                return authHeader
            else -> null
        }
    }

    private fun extractTenantIdFromToken(token: String?): Long {
        try {
            val payloadJson = String(
                java.util.Base64.getUrlDecoder().decode(token?.split(".")[1]),
                Charsets.UTF_8
            )
            val payload = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readTree(payloadJson)
            return payload["tenantId"]?.asLong()
                ?: throw IllegalStateException("tenantId not found in token")
        } catch (e: Exception) {
            throw IllegalStateException("Invalid token format: ${e.message}")
        }
    }

}
