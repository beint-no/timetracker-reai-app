package reai.timetracker.controller

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reai.timetracker.service.ReaiApiService
import reai.timetracker.service.TimeTrackerService
import java.time.Duration
import java.time.LocalDate

@Controller
@RequestMapping("/htmx")
class TimeTrackerViewController(
    private val timeTrackerService: TimeTrackerService,
    private val reaiApiService: ReaiApiService
) {
    private val logger = LoggerFactory.getLogger(TimeTrackerViewController::class.java)

    @GetMapping("/employees")
    fun getEmployeesList(
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        session: jakarta.servlet.http.HttpSession,
        model: Model
    ): String {
        val employees = reaiApiService.getEmployees(authHeader)
        val selectedEmployeeId = session.getAttribute("selectedEmployeeId") as? Long

        model.addAttribute("employees", employees)
        model.addAttribute("selectedEmployeeId", selectedEmployeeId)
        return "fragments/employee-list"
    }

    @GetMapping("/projects")
    fun getProjectsList(
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        @RequestParam(required = false) search: String?,
        session: jakarta.servlet.http.HttpSession,
        model: Model
    ): String {
        val projects = reaiApiService.getProjects(search, authHeader)
        val selectedProjectId = session.getAttribute("selectedProjectId") as? String

        model.addAttribute("projects", projects)
        model.addAttribute("selectedProjectId", selectedProjectId)
        return "fragments/project-list"
    }

    @GetMapping("/timer/current")
    fun getCurrentTimer(
        @RequestParam(required = false) employeeId: Long?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        model: Model
    ): String {
        if (employeeId == null) {
            model.addAttribute("hasTimer", false)
            return "fragments/timer-display"
        }

        val timer = timeTrackerService.getCurrentTimer(employeeId)
        if (timer.isPresent) {
            val timerEntry = timer.get()
            model.addAttribute("timer", timerEntry)
            model.addAttribute("hasTimer", true)
            val projectName = resolveProjectName(timerEntry.projectId, authHeader)
            if (projectName != null) {
                model.addAttribute("projectName", projectName)
            }
        } else {
            model.addAttribute("hasTimer", false)
        }
        return "fragments/timer-display"
    }

    @PostMapping("/timer/start")
    fun startTimer(
        @RequestParam projectId: Long,
        @RequestParam employeeId: Long,
        @RequestParam projectName: String,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        session: jakarta.servlet.http.HttpSession,
        model: Model
    ): String {
        // Save to session
        session.setAttribute("selectedEmployeeId", employeeId)
        session.setAttribute("selectedProjectId", projectId.toString())

        // Check if there's already an active timer BEFORE calling service
        val existingTimer = timeTrackerService.getCurrentTimer(employeeId)
        if (existingTimer.isPresent) {
            // Show existing timer with warning
            val timer = existingTimer.get()
            model.addAttribute("timer", timer)
            model.addAttribute("hasTimer", true)
            model.addAttribute("projectName", resolveProjectName(timer.projectId, authHeader))
            model.addAttribute("showCloseButton", true)
            model.addAttribute("notificationVariant", "warning")
            model.addAttribute("notificationMessage", "Timer already running!")
            return "fragments/timer-display"
        }

        return try {
            val entry = timeTrackerService.startTimer(projectId, employeeId)
            model.addAttribute("timer", entry)
            model.addAttribute("projectName", projectName)
            model.addAttribute("hasTimer", true)
            model.addAttribute("notificationVariant", "success")
            model.addAttribute("notificationMessage", "Timer started!")
            "fragments/timer-display"
        } catch (e: Exception) {
            logger.error("Error starting timer: ${e.message}")
            model.addAttribute("hasTimer", false)
            model.addAttribute("notificationVariant", "danger")
            model.addAttribute("notificationMessage", "Error: ${e.message}")
            "fragments/timer-display"
        }
    }

    @PostMapping("/timer/stop")
    fun stopTimer(
        @RequestParam employeeId: Long?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        model: Model
    ): String {
        return try {
            timeTrackerService.stopTimer(employeeId, authHeader)
            model.addAttribute("hasTimer", false)
            model.addAttribute("notificationVariant", "success")
            model.addAttribute("notificationMessage", "Timer stopped!")
            "fragments/timer-display"
        } catch (e: Exception) {
            logger.error("Error stopping timer: ${e.message}")
            model.addAttribute("hasTimer", false)
            model.addAttribute("notificationVariant", "danger")
            model.addAttribute("notificationMessage", "Error: ${e.message}")
            "fragments/timer-display"
        }
    }

    @GetMapping("/entries/today")
    fun getTodayEntries(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) projectId: Long?,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        model: Model
    ): String {
        if (employeeId == null) {
            model.addAttribute("entries", emptyList<Any>())
            model.addAttribute("projects", emptyMap<Long, Any>())
            model.addAttribute("totalHours", 0)
            model.addAttribute("totalMinutes", 0)
            model.addAttribute("entryCount", 0)
            model.addAttribute("currentPage", 1)
            model.addAttribute("totalPages", 1)
            model.addAttribute("pageSize", limit)
            return "fragments/today-entries"
        }

        val today = LocalDate.now()
        val allEntries = timeTrackerService.getTimeEntries(employeeId, projectId)
        val todayEntries = allEntries.filter { it.entryDate == today }

        val totalCount = todayEntries.size
        val pagedEntries = todayEntries.drop(offset).take(limit)

        val totalMinutes = todayEntries
            .filter { it.endTime != null }
            .sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes()
            }

        val projects = reaiApiService.getProjects(null, authHeader)
        val projectMap = projects.associateBy { it.id }

        val currentPage = (offset / limit) + 1
        val totalPages = (totalCount + limit - 1) / limit

        model.addAttribute("entries", pagedEntries)
        model.addAttribute("projects", projectMap)
        model.addAttribute("totalHours", totalMinutes / 60)
        model.addAttribute("totalMinutes", totalMinutes % 60)
        model.addAttribute("entryCount", totalCount)
        model.addAttribute("currentPage", currentPage)
        model.addAttribute("totalPages", totalPages)
        model.addAttribute("pageSize", limit)
        model.addAttribute("offset", offset)
        model.addAttribute("employeeId", employeeId)
        model.addAttribute("projectId", projectId)

        return "fragments/today-entries"
    }

    @GetMapping("/entries/all")
    fun getAllEntries(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) projectId: Long?,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        model: Model
    ): String {
        if (employeeId == null) {
            model.addAttribute("entries", emptyList<Any>())
            model.addAttribute("projects", emptyMap<Long, Any>())
            model.addAttribute("totalCount", 0)
            model.addAttribute("currentPage", 1)
            model.addAttribute("totalPages", 1)
            model.addAttribute("pageSize", limit)
            return "fragments/entries-list"
        }

        val allEntries = timeTrackerService.getTimeEntries(employeeId, projectId)
        val totalCount = allEntries.size
        val pagedEntries = allEntries.drop(offset).take(limit)

        val projects = reaiApiService.getProjects(null, authHeader)
        val projectMap = projects.associateBy { it.id }

        val currentPage = (offset / limit) + 1
        val totalPages = (totalCount + limit - 1) / limit

        model.addAttribute("entries", pagedEntries)
        model.addAttribute("projects", projectMap)
        model.addAttribute("totalCount", totalCount)
        model.addAttribute("currentPage", currentPage)
        model.addAttribute("totalPages", totalPages)
        model.addAttribute("pageSize", limit)
        model.addAttribute("offset", offset)
        model.addAttribute("employeeId", employeeId)
        model.addAttribute("projectId", projectId)

        return "fragments/entries-list"
    }

    @GetMapping("/stats")
    fun getStats(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) projectId: Long?,
        @RequestParam(required = false) tab: String?,
        model: Model
    ): String {
        if (employeeId == null) {
            model.addAttribute("totalHours", 0.0)
            model.addAttribute("totalEntries", 0)
            model.addAttribute("syncedEntries", 0)
            return "fragments/stats"
        }

        val allEntries = timeTrackerService.getTimeEntries(employeeId, projectId)

        // Filter by tab (today or all)
        val filteredEntries = if (tab == "today") {
            val today = LocalDate.now()
            allEntries.filter { it.entryDate == today }
        } else {
            allEntries
        }

        val totalHours = filteredEntries
            .filter { it.totalHours != null }
            .sumOf { it.totalHours ?: 0.0 }

        val syncedCount = filteredEntries.count { it.synced }

        model.addAttribute("totalHours", totalHours)
        model.addAttribute("totalEntries", filteredEntries.size)
        model.addAttribute("syncedEntries", syncedCount)

        return "fragments/stats"
    }

    @PostMapping("/sync")
    fun syncEntries(
        @RequestParam(required = false) employeeId: Long?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        model: Model
    ): String {
        if (employeeId == null) {
            model.addAttribute("variant", "danger")
            model.addAttribute("message", "Please select an employee first")
            return "fragments/notification"
        }

        return try {
            val synced = timeTrackerService.syncTodayAllDataAggregated(employeeId, authHeader)
            model.addAttribute("variant", if (synced) "success" else "danger")
            model.addAttribute("message", if (synced) "Synced successfully!" else "Sync failed")
            "fragments/notification"
        } catch (e: Exception) {
            logger.error("Error syncing: ${e.message}")
            model.addAttribute("variant", "danger")
            model.addAttribute("message", "Error: ${e.message}")
            "fragments/notification"
        }
    }

    @DeleteMapping("/timer/active")
    fun deleteActiveTimer(
        @RequestParam employeeId: Long?,
        model: Model
    ): String {
        return try {
            val deleted = timeTrackerService.deleteActiveTimer(employeeId)
            if (deleted) {
                model.addAttribute("hasTimer", false)
                model.addAttribute("success", true)
                model.addAttribute("message", "Active timer deleted")
            } else {
                model.addAttribute("hasTimer", false)
                model.addAttribute("error", "No active timer found")
            }
            "fragments/timer-display"
        } catch (e: Exception) {
            logger.error("Error deleting active timer: ${e.message}")
            model.addAttribute("error", e.message)
            "fragments/timer-display"
        }
    }

    @PostMapping("/timer/close")
    fun closeActiveTimer(
        @RequestParam employeeId: Long?,
        model: Model
    ): String {
        return try {
            val entry = timeTrackerService.closeActiveTimer(employeeId)
            if (entry != null) {
                model.addAttribute("hasTimer", false)
                model.addAttribute("success", true)
                model.addAttribute("message", "Active timer closed (0 hours)")
            } else {
                model.addAttribute("hasTimer", false)
                model.addAttribute("error", "No active timer found")
            }
            "fragments/timer-display"
        } catch (e: Exception) {
            logger.error("Error closing active timer: ${e.message}")
            model.addAttribute("error", e.message)
            "fragments/timer-display"
        }
    }

    private fun resolveProjectName(projectId: Long, authHeader: String?): String? =
        runCatching {
            reaiApiService.getProjects(null, authHeader)
                .firstOrNull { it.id == projectId }
                ?.name
        }.getOrNull()

}
