package reai.timetracker.controller

import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {

    @Value("\${reai.login.url}")
    private lateinit var reaiLoginUrl: String

    @GetMapping("/")
    fun index(model: Model, session: HttpSession): String {
        model.addAttribute("reaiLoginUrl", reaiLoginUrl)

        // Pass session data to template
        val selectedEmployeeId = session.getAttribute("selectedEmployeeId") as? Long
        val selectedProjectId = session.getAttribute("selectedProjectId") as? String

        model.addAttribute("selectedEmployeeId", selectedEmployeeId)
        model.addAttribute("selectedProjectId", selectedProjectId)

        return "index"
    }

    @GetMapping("/tracker")
    fun tracker(model: Model, session: HttpSession): String {
        model.addAttribute("reaiLoginUrl", reaiLoginUrl)

        val selectedEmployeeId = session.getAttribute("selectedEmployeeId") as? Long
        val selectedProjectId = session.getAttribute("selectedProjectId") as? String

        model.addAttribute("selectedEmployeeId", selectedEmployeeId)
        model.addAttribute("selectedProjectId", selectedProjectId)

        return "index"
    }
}
