package com.project.InboxBuddy;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class EmailGenController {

    private final EmailGenService emailGenService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("InboxBuddy API is running");
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest) {
        if (emailRequest == null || emailRequest.getEmailContent() == null
                || emailRequest.getEmailContent().isBlank()) {
            return ResponseEntity.badRequest().body("emailContent must not be empty");
        }
        String response = emailGenService.generateEmailReply(emailRequest);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().startsWith("Gemini API")
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body("Error generating email reply: " +
                        (ex.getMessage() == null ? "Unknown error" : ex.getMessage()));
    }
}
