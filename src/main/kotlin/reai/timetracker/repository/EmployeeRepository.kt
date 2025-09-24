package reai.timetracker.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import reai.timetracker.entity.Employee

@Repository
interface EmployeeRepository : JpaRepository<Employee, Long> {
    fun findByTenantId(tenantId: Long): List<Employee>
    fun findByNameContainingIgnoreCase(name: String): List<Employee>
}
