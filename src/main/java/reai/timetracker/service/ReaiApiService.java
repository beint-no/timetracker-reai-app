package reai.timetracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reai.timetracker.entity.Employee;
import reai.timetracker.entity.TimeEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReaiApiService {

    private final WebClient webClient;

    @Value("${reai.api.base-url:http://localhost:8080}")
    private String reaiApiBaseUrl;

    @Value("${reai.api.client-id:}")
    private String clientId;

    @Value("${reai.api.client-secret:}")
    private String clientSecret;

    public ReaiApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<Employee> getEmployees(Long tenantId) {
        try {
            return webClient.get()
                    .uri(reaiApiBaseUrl + "/api/employees?tenantId=" + tenantId)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .retrieve()
                    .bodyToFlux(Employee.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch employees from ReAI API: " + e.getMessage());
            return getMockEmployees(tenantId);
        }
    }

    public List<Employee> getEmployees() {
        // Fallback method for backward compatibility
        return getEmployees(1L);
    }

    public Employee getEmployee(Long id, Long tenantId) {
        try {
            return webClient.get()
                    .uri(reaiApiBaseUrl + "/api/employees/" + id + "?tenantId=" + tenantId)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .retrieve()
                    .bodyToMono(Employee.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch employee from ReAI API: " + e.getMessage());
            return getMockEmployees(tenantId).stream()
                    .filter(emp -> emp.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    public Employee getEmployee(Long id) {
        // Fallback method for backward compatibility
        return getEmployee(id, 1L);
    }

    public boolean syncTimeEntry(TimeEntry entry) {
        try {
            TimesheetEntryDto reaiEntry = mapToReaiEntry(entry);

            webClient.post()
                    .uri(reaiApiBaseUrl + "/api/timesheet/entries")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .bodyValue(reaiEntry)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            System.out.println("Successfully synced time entry: " + entry.getProjectName());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to sync time entry: " + e.getMessage());
            return false;
        }
    }

    private TimesheetEntryDto mapToReaiEntry(TimeEntry entry) {
        TimesheetEntryDto dto = new TimesheetEntryDto();
        dto.setEmployeeId(entry.getEmployeeId());
        dto.setProjectName(entry.getProjectName());
        dto.setStartTime(entry.getStartTime());
        dto.setEndTime(entry.getEndTime());
        dto.setDescription(entry.getDescription());
        dto.setBillable(entry.isBillable());
        dto.setTenantId(entry.getTenantId());
        return dto;
    }

    private String getAccessToken() {
        // TODO: Implement actual OAuth 2.0 flow
        // For now, return mock token
        return "mock-access-token";
    }

    private List<Employee> getMockEmployees(Long tenantId) {
        List<Employee> employees = new ArrayList<>();

        if (tenantId.equals(1L)) {
            employees.add(new Employee(1L, "John Doe", "john@tenant1.com"));
            employees.add(new Employee(2L, "Jane Smith", "jane@tenant1.com"));
            employees.add(new Employee(3L, "Bob Johnson", "bob@tenant1.com"));
        } else if (tenantId.equals(2L)) {
            employees.add(new Employee(4L, "Alice Brown", "alice@tenant2.com"));
            employees.add(new Employee(5L, "Charlie Wilson", "charlie@tenant2.com"));
        }

        // Set tenant ID for all employees
        employees.forEach(emp -> emp.setTenantId(tenantId));

        return employees;
    }

    public static class TimesheetEntryDto {
        private Long employeeId;
        private String projectName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String description;
        private boolean billable;
        private Long tenantId;

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isBillable() { return billable; }
        public void setBillable(boolean billable) { this.billable = billable; }

        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }
}