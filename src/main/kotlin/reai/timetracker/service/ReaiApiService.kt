package reai.timetracker.service

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import reai.timetracker.entity.EmployeesDto
import reai.timetracker.entity.TimeEntry
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ReaiApiService(
    private val restClientBuilder: RestClient.Builder
) {
    private val logger = LoggerFactory.getLogger(ReaiApiService::class.java)

    @Value("\${reai.api.base-url}")
    private lateinit var reaiApiBaseUrl: String

    @Value("\${api.secret:your-very-long-api-secret}")
    private lateinit var apiSecret: String

    private lateinit var restClient: RestClient

    @PostConstruct
    fun initRestClient() {
        restClient = restClientBuilder
            .baseUrl(reaiApiBaseUrl)
            .defaultHeader("User-Agent", "ReAI-TimeTracker/1.0")
            .build()
    }

    fun getEmployees(accessToken: String?): List<EmployeesDto> {
        return try {
            val request = restClient.get()
                .uri("/api/employee/list-employees")
                .accept(MediaType.APPLICATION_JSON)

            accessToken?.let { request.header("Authorization", it) }

            val employees = request
                .retrieve()
                .body(Array<EmployeesDto>::class.java)
                ?.toList() ?: emptyList()

            logger.info("Successfully received ${employees.size} employees")
            employees

        } catch (e: RestClientResponseException) {
            logger.error("HTTP error fetching employees: ${e.statusCode} - ${e.responseBodyAsString}")
            if (e.statusCode.value() == 401) {
                throw IllegalStateException("Unauthorized access to employees API")
            }
            emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching employees: ${e.message}", e)
            emptyList()
        }
    }

    fun getEmployee(id: Long, accessToken: String?): EmployeesDto? {
        return try {
            val request = restClient.get()
                .uri("api/employee/detail?id=${id}")
                .accept(MediaType.APPLICATION_JSON)

            accessToken?.let { request.header("Authorization", it) }

            val employee = request
                .retrieve()
                .body(EmployeesDto::class.java)

            employee?.also { emp ->
                logger.info("Employee: ID=${emp.id}, Name=${emp.name}")
            } ?: run {
                logger.warn("No employee found for ID=$id")
                null
            }

        } catch (e: RestClientResponseException) {
            logger.error("HTTP error fetching employee ID=$id: ${e.statusCode} - ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Error fetching employee ID=$id: ${e.message}", e)
            null
        }
    }
    fun getProjects(name: String?, accessToken: String?): List<ProjectDto> {
        return try {
            logger.info("Fetching projects for name: $name")

            val uriBuilder = StringBuilder("api/project/list-projects")
            if (!name.isNullOrBlank()) {
                uriBuilder.append("?name={name}")
            }

            val requestSpec = restClient.get()
                .uri(uriBuilder.toString()) { builder ->
                    if (!name.isNullOrBlank()) {
                        builder.queryParam("name", name)
                    }
                    builder.build()
                }
                .accept(MediaType.APPLICATION_JSON)

            accessToken?.let { requestSpec.header("Authorization", it) }

            val projects = requestSpec
                .retrieve()
                .body(Array<ProjectDto>::class.java)
                ?.toList() ?: emptyList()

            logger.info("Successfully received ${projects.size} projects")
            projects

        } catch (e: RestClientResponseException) {
            logger.error("HTTP error fetching projects: ${e.statusCode} - ${e.responseBodyAsString}")
            if (e.statusCode.value() == 401) {
                throw IllegalStateException("Unauthorized access to projects API")
            }
            emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching projects ${e.message}", e)
            emptyList()
        }
    }

    fun syncTimeEntry(entry: TimeEntry, accessToken: String?): Boolean {
        logger.info("Syncing time entry ID=${entry.id} to ReAI platform")

        return try {
            val timesheetEntry = mapToReaiEntry(entry)

            val request = restClient.post()
                .uri("/api/timesheet/create")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(timesheetEntry)

            accessToken?.let { request.header("Authorization", it) }

            val response = request
                .retrieve()
                .toEntity(String::class.java)

            val isSuccess = response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.CREATED

            if (isSuccess) {
                logger.info("Successfully synced entry ID=${entry.id} to ReAI platform. Response: ${response.body}")
            } else {
                logger.warn("Failed to sync entry ID=${entry.id}, status: ${response.statusCode}")
            }

            isSuccess

        } catch (e: RestClientResponseException) {
            logger.error("HTTP error syncing entry ID=${entry.id}: ${e.statusCode} - ${e.responseBodyAsString}")
            false
        } catch (e: Exception) {
            logger.error("Failed to sync entry ID=${entry.id} to ReAI platform: ${e.message}", e)
            false
        }
    }

    fun syncMultipleTimeEntries(entries: List<TimeEntry>): SyncResult {
        logger.info("Syncing ${entries.size} time entries to ReAI platform")

        var successCount = 0
        var failCount = 0
        val failedEntries = mutableListOf<Long>()

        entries.forEach { entry ->
            if (syncTimeEntry(entry, "")) {
                successCount++
            } else {
                failCount++
                entry.id?.let { failedEntries.add(it) }
            }
        }

        logger.info("Sync completed: $successCount success, $failCount failed")
        return SyncResult(successCount, failCount, failedEntries)
    }


    private fun mapToReaiEntry(entry: TimeEntry): TrackerTimesheetRequest {
        return TrackerTimesheetRequest(
            employeeId = entry.employeeId,
            projectId = entry.projectId,
            date = entry.entryDate,
            hours = BigDecimal(entry.totalHours ?: 0.0),
            startTime = entry.startTime,
            endTime = entry.endTime
        )
    }

    data class TrackerTimesheetRequest(
        @JsonProperty("employeeId")
        val employeeId: Long,

        @JsonProperty("projectId")
        val projectId: Long?,

        @JsonProperty("date")
        val date: LocalDate,

        @JsonProperty("hours")
        val hours: BigDecimal,

        @JsonProperty("startTime")
        val startTime: LocalDateTime?,

        @JsonProperty("endTime")
        val endTime: LocalDateTime?
    )

    data class SyncResult(
        val successCount: Int,
        val failCount: Int,
        val failedEntryIds: List<Long>
    )
}

data class ProjectDto(
    @JsonProperty("id") val id: Long,
    @JsonProperty("name") val name: String
)