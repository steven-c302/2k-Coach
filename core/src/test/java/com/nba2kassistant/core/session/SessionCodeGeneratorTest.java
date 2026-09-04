package com.nba2kassistant.core.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCodeGeneratorTest {

    @Mock
    private SessionRepository sessionRepository;

    @Test
    void generatesASixCharacterCodeWhenUnused() {
        when(sessionRepository.existsByCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        String code = new SessionCodeGenerator(sessionRepository).generateUnique();

        assertThat(code).hasSize(6);
    }

    @Test
    void retriesOnCollisionUntilAnUnusedCodeIsFound() {
        when(sessionRepository.existsByCode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, true, false);

        String code = new SessionCodeGenerator(sessionRepository).generateUnique();

        assertThat(code).hasSize(6);
    }

    @Test
    void givesUpAfterTooManyCollisions() {
        when(sessionRepository.existsByCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        assertThatThrownBy(() -> new SessionCodeGenerator(sessionRepository).generateUnique())
                .isInstanceOf(IllegalStateException.class);
    }
}
