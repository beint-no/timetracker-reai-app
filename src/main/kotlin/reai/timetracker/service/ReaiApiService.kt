package reai.timetracker.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reai.timetracker.entity.EmployeesDto
import reai.timetracker.entity.TimeEntry
import java.time.LocalDateTime
import java.time.ZoneId

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

    fun getEmployees(tenantId: Long): List<EmployeesDto> {
        logger.info("Fetching employees for tenantId=$tenantId from $reaiApiBaseUrl")
        val response = try {
            webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/list-employees?tenantId=$tenantId")
                .header("X-Api-Secret", apiSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(EmployeesDto::class.java)
                .block()
        } catch (e: Exception) {
            logger.error("Error fetching employees: ${e.message}", e)
            return emptyList()
        }
        logger.info("Raw response: $response")

        return try {
            webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/list-employees?tenantId=$tenantId")
//                .header("X-Api-Secret", apiSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(EmployeesDto::class.java)
                .collectList()
                .block() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error parsing employees: ${e.message}", e)
            emptyList()
        }.also { employees ->
            if (employees.isNotEmpty()) {
                logger.info("Received ${employees.size} employees:")
                employees.forEach { employee ->
                    logger.info("Employee: ID=${employee.id}, Name=${employee.name}")
                }
            } else {
                logger.warn("No employees found for tenantId=$tenantId")
            }
        }
    }

    fun getEmployees(): List<EmployeesDto> = getEmployees(1L)

    fun getEmployee(id: Long, tenantId: Long): EmployeesDto? {
        logger.info("Fetching employee ID=$id for tenantId=$tenantId from $reaiApiBaseUrl")
        val response = try {
            webClient.get()
                .uri("$reaiApiBaseUrl/api/employees/$id?tenantId=$tenantId")
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
                .uri("$reaiApiBaseUrl/api/employees/$id?tenantId=$tenantId")
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

    fun syncTimeEntry(entry: TimeEntry): Boolean {
        logger.info("Syncing time entry for employeeId=${entry.employeeId}, tenantId=${entry.tenantId}")
        val reaiEntry = mapToReaiEntry(entry)
        return try {
            webClient.post()
                .uri("$reaiApiBaseUrl/api/timesheet/entries?tenantId=${entry.tenantId}")
                .header("X-Api-Secret", apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reaiEntry)
                .retrieve()
                .toBodilessEntity()
                .block()
            logger.info("Successfully synced time entry: employeeId=${entry.employeeId}, projectName=${entry.projectName}, startTime=${entry.startTime}")
            true
        } catch (e: Exception) {
            logger.error("Error syncing time entry: ${e.message}", e)
            false
        }
    }

    private fun mapToReaiEntry(entry: TimeEntry): TimesheetEntryDto =
        TimesheetEntryDto(
            employeeId = entry.employeeId,
            projectName = entry.projectName,
            startTime = entry.startTime?.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))?.toLocalDateTime(),
            endTime = entry.endTime?.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))?.toLocalDateTime(),
            description = entry.description,
            billable = entry.billable,
            tenantId = entry.tenantId
        )

    data class TimesheetEntryDto(
        @JsonProperty("employeeId") val employeeId: Long? = null,
        @JsonProperty("projectName") val projectName: String? = null,
        @JsonProperty("startTime") val startTime: LocalDateTime? = null,
        @JsonProperty("endTime") val endTime: LocalDateTime? = null,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("billable") val billable: Boolean = true,
        @JsonProperty("tenant_id") val tenantId: Long? = null
    )

}