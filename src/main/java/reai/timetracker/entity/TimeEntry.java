package reai.timetracker.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
public class TimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String description;

    @Column(nullable = false)
    private Long employeeId;

    @Transient
    private String employeeName;

    private boolean billable = true;

    private boolean synced = false;

    @Column(name = "tenant_id")
    private Long tenantId;

    public TimeEntry() {}

    public TimeEntry(String projectName, Long employeeId) {
        this.projectName = projectName;
        this.employeeId = employeeId;
        this.startTime = LocalDateTime.now();
    }
    public Duration getDuration() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }

    public void stop() {
        this.endTime = LocalDateTime.now();
    }

    public boolean isActive() {
        return endTime == null;
    }

    public long getDurationMinutes() {
        return getDuration().toMinutes();
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public boolean isBillable() { return billable; }
    public void setBillable(boolean billable) { this.billable = billable; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}