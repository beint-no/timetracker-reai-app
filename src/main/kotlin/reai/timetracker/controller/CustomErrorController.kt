package reai.timetracker.controller

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
class CustomErrorController : ErrorController {

    private val logger = LoggerFactory.getLogger(CustomErrorController::class.java)

    @RequestMapping("/error", produces = [MediaType.TEXT_HTML_VALUE])
    fun handleError(request: HttpServletRequest): ModelAndView {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int ?: 500
        val exception = request.getAttribute("jakarta.servlet.error.exception") as? Throwable
        val message = request.getAttribute("jakarta.servlet.error.message") as? String
            ?: exception?.message
            ?: "An unexpected error occurred"

        logger.error("Error page requested: status={}, message={}", status, message, exception)

        val mav = ModelAndView("error")
        mav.addObject("status", status.toString())
        mav.addObject("error", HttpStatus.valueOf(status).reasonPhrase)
        mav.addObject("message", message)
        
        return mav
    }
}
