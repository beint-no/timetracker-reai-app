package reai.timetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
open class TimeTrackerApplication

fun main(args: Array<String>) {
    runApplication<TimeTrackerApplication>(*args)
}

