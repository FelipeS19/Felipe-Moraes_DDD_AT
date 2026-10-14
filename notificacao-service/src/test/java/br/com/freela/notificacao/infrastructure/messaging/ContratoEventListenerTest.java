package br.com.freela.notificacao.infrastructure.messaging;

import br.com.freela.notificacao.NotificacaoService;
import br.com.freela.notificacao.infrastructure.idempotency.ProcessedEvent;
import br.com.freela.notificacao.infrastructure.idempotency.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContratoEventListenerTest {

    private NotificacaoService notificacaoService;
    private ProcessedEventRepository processedEventRepository;
    private ObjectMapper objectMapper;
    private ContratoEventListener listener;

    @BeforeEach
    void setUp() {
        notificacaoService = mock(NotificacaoService.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        listener = new ContratoEventListener(notificacaoService, processedEventRepository, objectMapper);
    }

    @Test
    @DisplayName("Deve processar ContratoCriado e enviar notificações para cliente e freelancer")
    void deveProcessarContratoCriado() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoCriado",
            "eventId": "%s",
            "contratoId": "%s",
            "clienteId": "%s",
            "freelancerId": "%s",
            "titulo": "Construção de API",
            "valor": 2500.00,
            "correlationId": "corr-001"
        }
        """.formatted(eventId, contratoId, clienteId, freelancerId);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        listener.handleContratoEvent(payload);

        verify(notificacaoService, times(1)).registrar(eq(contratoId), eq(clienteId), eq("EMAIL"), any(), eq("corr-001"));
        verify(notificacaoService, times(1)).registrar(eq(contratoId), eq(freelancerId), eq("APP_PUSH"), any(), eq("corr-001"));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("IDEMPOTÊNCIA: Deve ignorar evento já processado e não duplicar notificações")
    void deveIgnorarEventoDuplicado() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoCriado",
            "eventId": "%s",
            "contratoId": "%s",
            "clienteId": "%s",
            "freelancerId": "%s",
            "titulo": "Construção de API",
            "valor": 2500.00,
            "correlationId": "corr-dup"
        }
        """.formatted(eventId, contratoId, UUID.randomUUID(), UUID.randomUUID());

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        listener.handleContratoEvent(payload);

        verify(notificacaoService, never()).registrar(any(), any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar ContratoConcluido e notificar as partes")
    void deveProcessarContratoConcluido() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoConcluido",
            "eventId": "%s",
            "contratoId": "%s",
            "clienteId": "%s",
            "freelancerId": "%s",
            "valor": 3000.00,
            "correlationId": "corr-fim"
        }
        """.formatted(eventId, contratoId, clienteId, freelancerId);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        listener.handleContratoEvent(payload);

        verify(notificacaoService, times(1)).registrar(eq(contratoId), eq(clienteId), eq("EMAIL"), any(), eq("corr-fim"));
        verify(notificacaoService, times(1)).registrar(eq(contratoId), eq(freelancerId), eq("APP_PUSH"), any(), eq("corr-fim"));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }
}
