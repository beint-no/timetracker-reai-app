package reai.timetracker.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reai.timetracker.entity.Employee
import reai.timetracker.entity.TimeEntry
import java.time.LocalDateTime

@Service
class ReaiApiService(
        webClientBuilder: WebClient.Builder
) {
    private val webClient: WebClient = webClientBuilder.build()

    @Value("\${reai.api.base-url:http://localhost:8080}")
    private lateinit var reaiApiBaseUrl: String

    @Value("\${api.secret:your-very-long-api-secret}")
    private lateinit var apiSecret: String

    fun getEmployees(tenantId: Long): List<Employee> =
            try {
        webClient.get()
                .uri("$reaiApiBaseUrl/apps/tracker-time/list-employees?tenantId=$tenantId")
                .header("X-Api-Secret", apiSecret)
                .retrieve()
                .bodyToFlux(Employee::class.java)
                .collectList()
                .block() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun getEmployees(): List<Employee> = getEmployees(1L)

    fun getEmployee(id: Long, tenantId: Long): Employee? =
            try {
        webClient.get()
                .uri("$reaiApiBaseUrl/api/employees/$id?tenantId=$tenantId")
                .header("X-Api-Secret", apiSecret)
                .retrieve()
                .bodyToMono(Employee::class.java)
                .block()
    } catch (e: Exception) {
        null
    }

    fun getEmployee(id: Long): Employee? = getEmployee(id, 1L)

    fun syncTimeEntry(entry: TimeEntry): Boolean =
            try {
        val reaiEntry = mapToReaiEntry(entry)
        webClient.post()
                .uri("$reaiApiBaseUrl/api/timesheet/entries?tenantId=${entry.tenantId}")
                .header("X-Api-Secret", apiSecret)
                .bodyValue(reaiEntry)
                .retrieve()
                .toBodilessEntity()
                .block()
        true
    } catch (e: Exception) {
        false
    }

    private fun mapToReaiEntry(entry: TimeEntry): TimesheetEntryDto =
    TimesheetEntryDto(
            employeeId = entry.employeeId,
            projectName = entry.projectName,
            startTime = entry.startTime,
            endTime = entry.endTime,
            description = entry.description,
            billable = entry.billable,
            tenantId = entry.tenantId
    )

    data class TimesheetEntryDto(
            var employeeId: Long? = null,
            var projectName: String? = null,
            var startTime: LocalDateTime? = null,
            var endTime: LocalDateTime? = null,
            var description: String? = null,
            var billable: Boolean = true,
            var tenantId: Long? = null
    )
}
