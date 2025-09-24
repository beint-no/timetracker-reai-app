package reai.timetracker.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "employees")
data class Employee(

    @Id
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, unique = true)
    var email: String = "",

    var department: String? = null,

    @Column(name = "tenant_id")
    var tenantId: Long? = null
) : Serializable {
    val displayName: String
        get() = "$name ($email)"
}

data class EmployeesDto(
    val id: Long?,
    val name: String?,
)
