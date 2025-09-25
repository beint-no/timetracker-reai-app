package reai.timetracker.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reai.timetracker.entity.EmployeesDto
import reai.timetracker.entity.TimeEntry
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class ReaiApiService(
    webClientBuilder: WebClient.Builder
) {
    private val webClient: WebClient = webClientBuilder.build()
    private val logger = LoggerFactory.getLogger(ReaiApiService::class.java)

    @Value("\${reai.api.base-url:http://localhost:8080}")
    private lateinit var reaiApiBaseUrl: String

    @Value("\${api.secret:your-very-long-api-secret}")
    private lateinit var apiSecret: String

    fun getEmployees(tenantId: Long?, accessToken: String?): List<EmployeesDto> {
        return try {
            val employeesArray = webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/list-employees?tenantId=$tenantId")
                .header("Authorization", accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Array<EmployeesDto>::class.java)
                .block()

            val employees = employeesArray?.toList() ?: emptyList()
            employees

        } catch (e: Exception) {
            emptyList()
        }
    }


    fun getEmployee(id: Long, tenantId: Long): EmployeesDto? {
        logger.info("Fetching employee ID=$id for tenantId=$tenantId from $reaiApiBaseUrl")
        val response = try {
            webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/employee/$id/detail?tenantId=$tenantId")
                .header("X-Api-Secret", apiSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        } catch (e: Exception) {
            logger.error("Error fetching employee: ${e.message}", e)
            return null
        }
        logger.info("Raw response: $response")

        return try {
            webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/employee/$id?tenantId=$tenantId")
                .header("X-Api-Secret", apiSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(EmployeesDto::class.java)
                .block()
        } catch (e: Exception) {
            logger.error("Error parsing employee ID=$id: ${e.message}", e)
            null
        }?.also { employee ->
            logger.info("Employee: ID=${employee.id}, Name=${employee.name}")
        } ?: run {
            logger.warn("No employee found for ID=$id and tenantId=$tenantId")
            null
        }
    }

    fun getEmployee(id: Long): EmployeesDto? = getEmployee(id, 1L)


    fun getProjects(tenantId: Long?, name: String?, accessToken: String?): List<ProjectDto?> {

        return try {
            val projectsArray = webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/list-project?tenantId=$tenantId&name=$name")
                .header("Authorization", accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Array<ProjectDto>::class.java)
                .block()

            val projects = projectsArray?.toList() ?: emptyList()
            logger.info("Successfully received ${projects.size} projects")

            projects

        } catch (e: Exception) {
            logger.error("Error fetching projects for tenantId=$tenantId: ${e.message}", e)
            emptyList()
        }
    }

    fun syncTimeEntry(entry: TimeEntry): Boolean {
        logger.info("Syncing time entry ID=${entry.id} to ReAI platform")

        return try {
            val timesheetEntry = mapToReaiEntry(entry)

            val response = webClient.post()
                .uri("$reaiApiBaseUrl/apps/tracker-time/timesheet/create")
                .header("X-Api-Secret", apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(timesheetEntry), TimesheetEntryDto::class.java)
                .retrieve()
                .toEntity(String::class.java)
                .block()

            val isSuccess = response?.statusCode == HttpStatus.OK || response?.statusCode == HttpStatus.CREATED

            if (isSuccess) {
                logger.info("Successfully synced entry ID=${entry.id} to ReAI platform")
            } else {
                logger.warn("Failed to sync entry ID=${entry.id}, status: ${response?.statusCode}")
            }

            isSuccess

        } catch (e: WebClientResponseException) {
            logger.error("HTTP error syncing entry ID=${entry.id}: ${e.statusCode} - ${e.responseBodyAsString}")
            false
        } catch (e: Exception) {
            logger.error("Failed to sync entry ID=${entry.id} to ReAI platform: ${e.message}", e)
            false
        }
    }

    private fun mapToReaiEntry(entry: TimeEntry): TimesheetEntryDto {
        return TimesheetEntryDto(
            employeeId = entry.employeeId,
            projectName = entry.projectName,
            startTime = entry.startTime,
            endTime = entry.endTime,
            description = entry.description,
            billable = entry.billable,
            tenantId = entry.tenantId
        )
    }

    data class TimesheetEntryDto(
        @JsonProperty("employeeId")
        val employeeId: Long? = null,

        @JsonProperty("projectName")
        val projectName: String? = null,

        @JsonProperty("startTime")
        val startTime: LocalDateTime? = null,

        @JsonProperty("endTime")
        val endTime: LocalDateTime? = null,

        @JsonProperty("description")
        val description: String? = null,

        @JsonProperty("billable")
        val billable: Boolean = true,

        @JsonProperty("tenantId")
        val tenantId: Long? = null
    )

}
data class ProjectDto(
    @JsonProperty("id") val id: Long,
    @JsonProperty("name") val name: String,
    @JsonProperty("tenantId") val tenantId: Long?
)