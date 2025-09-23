package reai.timetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    public List<Employee> getEmployees() {
        return reaiApiService.getEmployees();
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable Long id) {
        return reaiApiService.getEmployee(id);
    }
}