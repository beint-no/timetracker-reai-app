package reai.timetracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reai.timetracker.entity.Employee;
import reai.timetracker.entity.TimeEntry;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReaiApiService {

    private final WebClient webClient;

    @Value("${reai.api.base-url}")
    private String reaiApiBaseUrl;

    @Value("${reai.api.client-id}")
    private String clientId;

    @Value("${reai.api.client-secret}")
    private String clientSecret;

    public ReaiApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<Employee> getEmployees() {
        try {
            return webClient.get()
                    .uri(reaiApiBaseUrl + "/api/employees?tenantId=1")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .retrieve()
                    .bodyToFlux(Employee.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            return getMockEmployees();
        }
    }

    public Employee getEmployee(Long id) {
        return getEmployees().stream()
                .filter(emp -> emp.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean syncTimeEntry(TimeEntry entry) {
        try {
            // Create a simple DTO to send to ReAI API
            TimesheetEntryDto reaiEntry = mapToReaiEntry(entry);

            webClient.post()
                    .uri(reaiApiBaseUrl + "/api/timesheet/entries")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .bodyValue(reaiEntry)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

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
        return dto;
    }

    private String getAccessToken() {
        return "mock-access-token";
    }

    private List<Employee> getMockEmployees() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1L, "John Doe", "john@company.com"));
        employees.add(new Employee(2L, "Jane Smith", "jane@company.com"));
        employees.add(new Employee(3L, "Bob Johnson", "bob@company.com"));
        employees.add(new Employee(4L, "Alice Brown", "alice@company.com"));
        return employees;
    }

    // DTO class để gửi data tới ReAI API
    public static class TimesheetEntryDto {
        private Long employeeId;
        private String projectName;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private String description;
        private boolean billable;

        // Getters and setters
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public java.time.LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(java.time.LocalDateTime startTime) { this.startTime = startTime; }

        public java.time.LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(java.time.LocalDateTime endTime) { this.endTime = endTime; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isBillable() { return billable; }
        public void setBillable(boolean billable) { this.billable = billable; }
    }
}