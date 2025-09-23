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

    @Value("${reai.api.base-url:http://localhost:8080}")
    private String reaiApiBaseUrl;

    @Value("${reai.api.tenant-id:1}")
    private Long tenantId;

    public ReaiApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<Employee> getEmployees() {
        try {
            // TODO: Replace with actual ReAI API call
            // For now, return mock data
            List<Employee> employees = new ArrayList<>();
            employees.add(new Employee(1L, "John Doe", "john@company.com"));
            employees.add(new Employee(2L, "Jane Smith", "jane@company.com"));
            employees.add(new Employee(3L, "Bob Johnson", "bob@company.com"));
            employees.add(new Employee(4L, "Alice Brown", "alice@company.com"));
            return employees;

            /*
            // Actual implementation when ReAI API is ready:
            return webClient.get()
                .uri(reaiApiBaseUrl + "/api/employees?tenantId=" + tenantId)
                .retrieve()
                .bodyToFlux(Employee.class)
                .collectList()
                .block();
            */
        } catch (Exception e) {
            // Fallback to mock data
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
            // TODO: Implement actual sync to ReAI API
            /*
            webClient.post()
                .uri(reaiApiBaseUrl + "/api/timesheet/entries")
                .bodyValue(entry)
                .retrieve()
                .toBodilessEntity()
                .block();
            */

            // For now, simulate successful sync
            System.out.println("Syncing time entry: " + entry.getProjectName() + " - " + entry.getDurationMinutes() + " minutes");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to sync time entry: " + e.getMessage());
            return false;
        }
    }

    private List<Employee> getMockEmployees() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1L, "John Doe", "john@company.com"));
        employees.add(new Employee(2L, "Jane Smith", "jane@company.com"));
        return employees;
    }
}