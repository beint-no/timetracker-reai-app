package reai.timetracker.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reai.timetracker.entity.TimeEntry
import java.time.LocalDate
import java.util.*

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, Long> {

    fun findByEmployeeIdAndEndTimeIsNull(
        employeeId: Long?
    ): Optional<TimeEntry>

    fun findByEmployeeIdOrderByStartTimeDesc(
        employeeId: Long,
    ): List<TimeEntry>

    fun findByEmployeeIdAndProjectIdOrderByStartTimeDesc(
        employeeId: Long,
        projectId: Long
    ): List<TimeEntry>


    @Query("""
    SELECT e FROM TimeEntry e
    WHERE
       e.employeeId = :employeeId
      AND e.entryDate = :today
""")
    fun findEntriesToday(
        @Param("employeeId") employeeId: Long,
        @Param("today") today: LocalDate
    ): List<TimeEntry>

    fun findByEmployeeIdAndProjectIdAndEntryDateAndSyncedIsFalse(employeeId: Long, projectId: Long, entryDate: LocalDate): List<TimeEntry>
}
