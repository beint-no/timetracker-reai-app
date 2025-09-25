package reai.timetracker.entity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "time_entries")
class TimeEntry(
    var projectName: String = "",
    var employeeId: Long = 0,
    tenantId: Long? = null
) : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var startTime: LocalDateTime = LocalDateTime.now()

    var endTime: LocalDateTime? = null
    var description: String? = null

    @Transient
    var employeeName: String? = null

    var billable: Boolean = true
    var synced: Boolean = false

    @Column(name = "tenant_id")
    var tenantId: Long? = tenantId

    @Column(name = "entry_date", nullable = false)
    var entryDate: LocalDate = LocalDate.now()

    constructor() : this("", 0, null)

    constructor(employeeId: Long, projectName: String) : this(projectName, employeeId, null)

    fun stop() {
        endTime = LocalDateTime.now()
    }

    val isActive: Boolean
        get() = endTime == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimeEntry) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "TimeEntry(id=$id, projectName='$projectName', employeeId=$employeeId, entryDate=$entryDate)"
    }
}
