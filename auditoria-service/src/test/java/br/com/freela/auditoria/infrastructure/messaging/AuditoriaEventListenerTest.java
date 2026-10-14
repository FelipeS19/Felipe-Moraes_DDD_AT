package br.com.freela.auditoria.infrastructure.messaging;

import br.com.freela.auditoria.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditoriaEventListenerTest {

    private AuditoriaService auditoriaService;
    private ObjectMapper objectMapper;
    private AuditoriaEventListener listener;

    @BeforeEach
    void setUp() {
        auditoriaService = mock(AuditoriaService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        listener = new AuditoriaEventListener(auditoriaService, objectMapper);
    }

    @Test
    @DisplayName("Deve registrar evento recebido na auditoria com correlationId e aggregateId")
    void deveRegistrarEventoAuditoria() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoCriado",
            "eventId": "%s",
            "contratoId": "%s",
            "correlationId": "corr-audit-1"
        }
        """.formatted(eventId, contratoId);

        listener.handleContratoEvent(payload);

        verify(auditoriaService, times(1)).registrar(eq(eventId), eq(contratoId), eq("ContratoCriado"), eq("corr-audit-1"), any());
    }
}
