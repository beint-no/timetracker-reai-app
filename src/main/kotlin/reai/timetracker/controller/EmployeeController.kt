package reai.timetracker.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reai.timetracker.entity.EmployeesDto
import reai.timetracker.service.ProjectDto
import reai.timetracker.service.ReaiApiService

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = ["http://localhost:8080"])
class EmployeeController(
    private val reaiApiService: ReaiApiService
) {
    private val logger = LoggerFactory.getLogger(EmployeeController::class.java)

    @GetMapping
    fun getEmployees(
        @RequestParam(required = false) tenantId: Long?,
        @RequestParam(required = false) access_token: String?
    ): ResponseEntity<List<EmployeesDto>> {
        return try {
            logger.info("Fetching employees for tenantId: $tenantId with access_token: $access_token")
            val employees = reaiApiService.getEmployees(tenantId, access_token)
            ResponseEntity.ok(employees)
        } catch (e: IllegalStateException) {
            logger.error("Unauthorized access: ${e.message}")
            ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Error fetching employees: ${e.message}")
            ResponseEntity.status(500).body(emptyList())
        }
    }

    @GetMapping(value = ["/project"], params = ["tenantId"])
    fun getProjects(
        @RequestParam(required = false) tenantId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) access_token: String?
    ): ResponseEntity<List<ProjectDto>> {
        return try {
            logger.info("Fetching projects for tenantId: $tenantId, name: $name, access_token: $access_token")
            val projects = reaiApiService.getProjects(tenantId, name, access_token)
            ResponseEntity.ok(projects)
        } catch (e: IllegalStateException) {
            logger.error("Unauthorized access: ${e.message}")
            ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Error fetching projects: ${e.message}")
            ResponseEntity.status(500).body(emptyList())
        } as ResponseEntity<List<ProjectDto>>
    }
}