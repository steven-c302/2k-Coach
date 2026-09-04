package com.nba2kassistant.core.session;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Short, human-typeable join codes (e.g. "K3F9QZ") — not UUIDs, since a friend reads this off a screen. */
@Component
public class SessionCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 — easy to misread
    private static final int LENGTH = 6;
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final SessionRepository sessionRepository;

    public SessionCodeGenerator(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generate();
            if (!sessionRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique session code after " + MAX_ATTEMPTS + " attempts");
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
