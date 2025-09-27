package reai.timetracker.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "employees")
data class Employee(


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = -1,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, unique = true)
    var email: String = "",

) : Serializable {
    val displayName: String
        get() = "$name ($email)"
}

data class EmployeesDto(
    val id: Long?,
    val name: String?,
)
