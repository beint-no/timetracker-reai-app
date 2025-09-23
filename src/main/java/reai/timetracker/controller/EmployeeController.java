package reai.timetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reai.timetracker.config.UserPrincipal;
import reai.timetracker.entity.Employee;
import reai.timetracker.service.ReaiApiService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private ReaiApiService reaiApiService;


    @GetMapping
    public List<Employee> getEmployees(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return reaiApiService.getEmployees(user.getTenantId());
    }
}