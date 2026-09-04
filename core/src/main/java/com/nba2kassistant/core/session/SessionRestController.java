package com.nba2kassistant.core.session;

import com.nba2kassistant.core.session.dto.CreateSessionResponse;
import com.nba2kassistant.core.session.dto.SessionStateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionRestController {

    private final SessionCodeGenerator codeGenerator;
    private final SessionRepository sessionRepository;
    private final SessionRegistry sessionRegistry;

    public SessionRestController(
            SessionCodeGenerator codeGenerator,
            SessionRepository sessionRepository,
            SessionRegistry sessionRegistry
    ) {
        this.codeGenerator = codeGenerator;
        this.sessionRepository = sessionRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping
    @Transactional
    public CreateSessionResponse create() {
        String code = codeGenerator.generateUnique();

        Session entity = new Session();
        entity.setCode(code);
        entity.setState(SessionStatus.LOBBY.name());
        sessionRepository.save(entity);

        sessionRegistry.register(SessionState.newLobby(code));

        return new CreateSessionResponse(code);
    }

    @GetMapping("/{code}")
    public SessionStateResponse get(@PathVariable String code) {
        return sessionRegistry.find(code)
                .map(SessionActor::currentState)
                .map(SessionStateResponse::from)
                .orElseThrow(() -> new SessionNotFoundException(code));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
