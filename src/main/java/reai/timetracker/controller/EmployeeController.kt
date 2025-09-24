package reai.timetracker.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reai.timetracker.entity.Employee
import reai.timetracker.service.ReaiApiService

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = ["http://localhost:8080"])
class EmployeeController(
        private val reaiApiService: ReaiApiService
) {
    private val logger = LoggerFactory.getLogger(EmployeeController::class.java)

    @GetMapping
    fun getEmployees(@RequestParam tenantId: Long): ResponseEntity<List<Employee>> {
        logger.debug("Fetching employees for user")
        return try {
            val employees = reaiApiService.getEmployees(tenantId)
            logger.info("Fetched ${employees.size} employees for tenantId: $tenantId")
            ResponseEntity.ok(employees)
        } catch (e: IllegalStateException) {
            logger.error("Failed to fetch employees: ${e.message}")
            ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Unexpected error fetching employees: ${e.message}")
            ResponseEntity.status(500).body(emptyList())
        }
    }
}
