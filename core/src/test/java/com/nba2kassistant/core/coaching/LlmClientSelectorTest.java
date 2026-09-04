package com.nba2kassistant.core.coaching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmClientSelectorTest {

    @Mock
    private AnthropicLlmClient anthropicClient;
    @Mock
    private TemplateNarrationClient templateClient;

    @Test
    void usesTheTemplateClientWhenAnthropicIsNotConfigured() {
        when(anthropicClient.isConfigured()).thenReturn(false);
        when(templateClient.narrate(List.of())).thenReturn("template narration");

        String result = new LlmClientSelector(anthropicClient, templateClient).narrate(List.of());

        assertThat(result).isEqualTo("template narration");
    }

    @Test
    void usesAnthropicWhenConfigured() {
        when(anthropicClient.isConfigured()).thenReturn(true);
        when(anthropicClient.narrate(List.of())).thenReturn("anthropic narration");

        String result = new LlmClientSelector(anthropicClient, templateClient).narrate(List.of());

        assertThat(result).isEqualTo("anthropic narration");
    }

    @Test
    void fallsBackToTemplateWhenAnthropicCallThrows() {
        when(anthropicClient.isConfigured()).thenReturn(true);
        when(anthropicClient.narrate(List.of())).thenThrow(new RuntimeException("network error"));
        when(templateClient.narrate(List.of())).thenReturn("template narration");

        String result = new LlmClientSelector(anthropicClient, templateClient).narrate(List.of());

        assertThat(result).isEqualTo("template narration");
    }
}
