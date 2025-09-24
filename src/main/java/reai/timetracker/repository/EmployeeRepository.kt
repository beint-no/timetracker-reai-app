package reai.timetracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reai.timetracker.entity.Employee;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByTenantId(Long tenantId);
    List<Employee> findByNameContainingIgnoreCase(String name);
}
