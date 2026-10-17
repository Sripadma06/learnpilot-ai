package com.learnpilot.quiz;

import com.learnpilot.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class QuizController {
    public record AttemptRequest(@NotNull List<Integer> answers) {}

    private final QuizService service;
    private final CurrentUser currentUser;

    public QuizController(QuizService service, CurrentUser currentUser) {
        this.service = service; this.currentUser = currentUser;
    }

    @PostMapping("/documents/{documentId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizService.QuizView generate(@PathVariable UUID documentId,
                                         @RequestParam(defaultValue = "5") int count, Authentication auth) {
        return service.generate(currentUser.id(auth), documentId, count);
    }

    @GetMapping("/quizzes")
    public List<QuizService.QuizSummary> list(Authentication auth) {
        return service.list(currentUser.id(auth));
    }

    @GetMapping("/quizzes/{id}")
    public QuizService.QuizView get(@PathVariable UUID id, Authentication auth) {
        return service.get(currentUser.id(auth), id);
    }

    @PostMapping("/quizzes/{id}/attempts")
    public QuizService.AttemptResult submit(@PathVariable UUID id, @Valid @RequestBody AttemptRequest request,
                                            Authentication auth) {
        return service.submit(currentUser.id(auth), id, request.answers());
    }
}
