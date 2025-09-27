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
class TimeEntry : Serializable {

    constructor()

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(nullable = false)
    var startTime: LocalDateTime = LocalDateTime.now()

    var endTime: LocalDateTime? = null

    var synced: Boolean = false

    @Column(name = "entry_date", nullable = false)
    var entryDate: LocalDate = LocalDate.now()

    @Column(name = "start_time_millis")
    var startTimeMillis: Long? = null

    @Column(name = "end_time_millis")
    var endTimeMillis: Long? = null

    @Column(name = "total_hours")
    var totalHours: Double? = null

    @Column(name = "total_milliseconds")
    var totalMilliseconds: Long? = null

    @Column(nullable = false)
    var projectId: Long = 0

    @Column(nullable = false)
    var employeeId: Long = 0

    constructor(employeeId: Long, projectId: Long) : this() {
        this.employeeId = employeeId
        this.projectId = projectId
    }

    fun stop() {
        endTime = LocalDateTime.now()
    }

    val isActive: Boolean
        get() = endTime == null

}
