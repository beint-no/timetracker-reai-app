package reai.timetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
open class TimeTrackerApplication

fun main(args: Array<String>) {
    runApplication<TimeTrackerApplication>(*args)
}
