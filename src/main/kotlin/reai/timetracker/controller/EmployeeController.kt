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
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<List<EmployeesDto>> {
        return try {
            val token = extractToken(access_token, authHeader)
            val employees = reaiApiService.getEmployees( token)
            ResponseEntity.ok(employees)
        } catch (e: IllegalStateException) {
            logger.error("Unauthorized access: ${e.message}")
            ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Error fetching employees: ${e.message}")
            ResponseEntity.status(500).body(emptyList())
        }
    }

    @GetMapping("/{id}")
    fun getEmployee(
        @PathVariable id: Long,
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<EmployeesDto> {
        return try {
            val token = extractToken(access_token, authHeader)
            val employee = reaiApiService.getEmployee(id, token)
            if (employee != null) ResponseEntity.ok(employee)
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.error("Unauthorized access: ${e.message}")
            ResponseEntity.status(401).build()
        } catch (e: Exception) {
            logger.error("Error fetching employee $id: ${e.message}")
            ResponseEntity.status(500).build()
        }
    }


    @GetMapping("/project")
    fun getProjects(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) access_token: String?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<List<ProjectDto>> {
        return try {
            val token = extractToken(access_token, authHeader)
            val projects = reaiApiService.getProjects(name, token)
            ResponseEntity.ok(projects)
        } catch (e: IllegalStateException) {
            logger.error("Unauthorized access: ${e.message}")
            ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Error fetching projects: ${e.message}")
            ResponseEntity.status(500).body(emptyList())
        }
    }


    private fun extractToken(queryToken: String?, authHeader: String?): String? {
        return when {
            authHeader?.startsWith("Bearer ") == true ->
                return authHeader
            else -> null
        }
    }
}