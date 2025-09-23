package reai.timetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reai.timetracker.config.UserPrincipal;
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
            @RequestParam Long employeeId,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        TimeEntry entry = timeTrackerService.startTimer(projectName, employeeId, user.getTenantId());
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/stop")
    public ResponseEntity<TimeEntry> stopTimer(
            @RequestParam Long employeeId,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        TimeEntry entry = timeTrackerService.stopTimer(employeeId, user.getTenantId());
        if (entry != null) {
            return ResponseEntity.ok(entry);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/current")
    public ResponseEntity<TimeEntry> getCurrentTimer(
            @RequestParam Long employeeId,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return timeTrackerService.getCurrentTimer(employeeId, user.getTenantId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entries")
    public List<TimeEntry> getTimeEntries(
            @RequestParam Long employeeId,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return timeTrackerService.getTimeEntries(employeeId, user.getTenantId());
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<TimeEntry> updateEntry(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean billable,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        TimeEntry updated = timeTrackerService.updateEntry(id, description, billable, user.getTenantId());
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncToReai(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        int synced = timeTrackerService.syncUnsyncedEntries(user.getTenantId());
        return ResponseEntity.ok("Synced " + synced + " entries to ReAI");
    }

    @GetMapping("/entries/all")
    public List<TimeEntry> getAllTimeEntries(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return timeTrackerService.getAllTimeEntries(user.getTenantId());
    }
}