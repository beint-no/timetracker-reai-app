package reai.timetracker.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): ModelAndView {
        logger.error("Bad request: ${ex.message}", ex)
        val mav = ModelAndView("error")
        mav.addObject("status", "400")
        mav.addObject("error", "Bad Request")
        mav.addObject("message", ex.message ?: "Invalid request")
        return mav
    }

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(ex: IllegalStateException): ModelAndView {
        logger.error("Unauthorized: ${ex.message}", ex)
        val mav = ModelAndView("error")
        mav.addObject("status", "401")
        mav.addObject("error", "Unauthorized")
        mav.addObject("message", ex.message ?: "Authentication required")
        return mav
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NoSuchElementException): ModelAndView {
        logger.error("Not found: ${ex.message}", ex)
        val mav = ModelAndView("error")
        mav.addObject("status", "404")
        mav.addObject("error", "Not Found")
        mav.addObject("message", ex.message ?: "Resource not found")
        return mav
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): ModelAndView {
        logger.error("Internal server error: ${ex.message}", ex)
        val mav = ModelAndView("error")
        mav.addObject("status", "500")
        mav.addObject("error", "Internal Server Error")
        mav.addObject("message", "An unexpected error occurred")
        return mav
    }
}
