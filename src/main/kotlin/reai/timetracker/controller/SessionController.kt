package reai.timetracker.controller

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/session")
class SessionController {

    @PostMapping("/employee")
    @ResponseBody
    fun setEmployee(
        @RequestParam employeeId: Long?,
        session: HttpSession
    ): Map<String, Any> {
        if (employeeId != null) {
            session.setAttribute("selectedEmployeeId", employeeId)
        } else {
            session.removeAttribute("selectedEmployeeId")
        }
        return mapOf("success" to true)
    }

    @PostMapping("/project")
    @ResponseBody
    fun setProject(
        @RequestParam projectId: String?,
        session: HttpSession
    ): Map<String, Any> {
        if (projectId != null && projectId != "") {
            session.setAttribute("selectedProjectId", projectId)
        } else {
            session.removeAttribute("selectedProjectId")
        }
        return mapOf("success" to true)
    }

    @GetMapping("/employee")
    @ResponseBody
    fun getEmployee(session: HttpSession): Map<String, Any?> {
        val employeeId = session.getAttribute("selectedEmployeeId")
        return mapOf("employeeId" to employeeId)
    }

    @GetMapping("/project")
    @ResponseBody
    fun getProject(session: HttpSession): Map<String, Any?> {
        val projectId = session.getAttribute("selectedProjectId")
        return mapOf("projectId" to projectId)
    }
}