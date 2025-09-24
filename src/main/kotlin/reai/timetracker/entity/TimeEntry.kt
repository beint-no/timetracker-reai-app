package reai.timetracker.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime

@Entity
@Table(name = "time_entries")
data class TimeEntry(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var projectName: String = "",

    @Column(nullable = false)
    var startTime: LocalDateTime = LocalDateTime.now(),

    var endTime: LocalDateTime? = null,

    var description: String? = null,

    @Column(nullable = false)
    var employeeId: Long = 0,

    @Transient
    var employeeName: String? = null,

    var billable: Boolean = true,

    var synced: Boolean = false,

    @Column(name = "tenant_id")
    var tenantId: Long? = null

) : Serializable {

    val duration: Duration
        get() = Duration.between(startTime, endTime ?: LocalDateTime.now())

    fun stop() {
        endTime = LocalDateTime.now()
    }

    val isActive: Boolean
        get() = endTime == null

    val durationMinutes: Long
        get() = duration.toMinutes()
}
