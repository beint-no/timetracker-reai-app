package reai.timetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reai.timetracker.entity.TimeEntry;
import reai.timetracker.service.TimeTrackerService;

import java.util.List;

@RestController
@RequestMapping("/api/time")
@CrossOrigin(origins = "*")
public class TimeTrackerController {

    @Autowired
    private TimeTrackerService timeTrackerService;

    @PostMapping("/start")
    public ResponseEntity<TimeEntry> startTimer(
            @RequestParam String projectName,
            @RequestParam Long employeeId) {

        TimeEntry entry = timeTrackerService.startTimer(projectName, employeeId);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/stop")
    public ResponseEntity<TimeEntry> stopTimer(@RequestParam Long employeeId) {
        TimeEntry entry = timeTrackerService.stopTimer(employeeId);
        if (entry != null) {
            return ResponseEntity.ok(entry);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/current")
    public ResponseEntity<TimeEntry> getCurrentTimer(@RequestParam Long employeeId) {
        return timeTrackerService.getCurrentTimer(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entries")
    public List<TimeEntry> getTimeEntries(@RequestParam Long employeeId) {
        return timeTrackerService.getTimeEntries(employeeId);
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<TimeEntry> updateEntry(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean billable) {

        TimeEntry updated = timeTrackerService.updateEntry(id, description, billable);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncToReai() {
        int synced = timeTrackerService.syncUnsyncedEntries();
        return ResponseEntity.ok("Synced " + synced + " entries to ReAI");
    }
}
