//package reai.timetracker.controller
//
//import org.slf4j.LoggerFactory
//import org.springframework.http.ResponseEntity
//import org.springframework.web.bind.annotation.*
//import reai.timetracker.service.ReaiApiService
//
//@RestController
//@RequestMapping("/api/auth")
//class AuthController(
//        private val reaiApiService: ReaiApiService
//) {
//    private val logger = LoggerFactory.getLogger(AuthController::class.java)
//
//    @PostMapping("/init")
//    fun init(
//            @RequestHeader("Authorization") authHeader: String,
//            @RequestParam tenantId: Long
//    ): ResponseEntity<String> {
//        logger.debug("Initializing Time Tracker with tenantId: $tenantId")
//        return try {
//            val token = authHeader.removePrefix("Bearer ").trim()
//            reaiApiService.saveToken(token, tenantId)
//            logger.info("Successfully initialized Time Tracker for tenantId: $tenantId")
//            ResponseEntity.ok("Initialized successfully")
//        } catch (e: Exception) {
//            logger.error("Failed to initialize Time Tracker: ${e.message}")
//            ResponseEntity.status(500).body("Initialization failed")
//        }
//    }
//}
