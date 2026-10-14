package br.com.freela.reputacao.infrastructure.messaging;

import br.com.freela.reputacao.ReputacaoService;
import br.com.freela.reputacao.infrastructure.idempotency.ProcessedEvent;
import br.com.freela.reputacao.infrastructure.idempotency.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReputacaoEventListenerTest {

    private ReputacaoService reputacaoService;
    private ProcessedEventRepository processedEventRepository;
    private ObjectMapper objectMapper;
    private ContratoEventListener listener;

    @BeforeEach
    void setUp() {
        reputacaoService = mock(ReputacaoService.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        listener = new ContratoEventListener(reputacaoService, processedEventRepository, objectMapper);
    }

    @Test
    @DisplayName("Deve atualizar reputação do freelancer ao receber ContratoConcluido")
    void deveAtualizarReputacao() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoConcluido",
            "eventId": "%s",
            "contratoId": "%s",
            "freelancerId": "%s",
            "valor": 3500.00,
            "correlationId": "corr-rep-1"
        }
        """.formatted(eventId, contratoId, freelancerId);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        listener.handleContratoEvent(payload);

        verify(reputacaoService, times(1)).registrarContratoConcluido(eq(contratoId), eq(freelancerId), eq(new BigDecimal("3500.00")), eq("corr-rep-1"));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("IDEMPOTÊNCIA: Reprocessamento do mesmo evento não deve incrementar a reputação novamente")
    void deveIgnorarEventoDuplicadoNaReputacao() {
        UUID eventId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        String payload = """
        {
            "eventType": "ContratoConcluido",
            "eventId": "%s",
            "contratoId": "%s",
            "freelancerId": "%s",
            "valor": 3500.00,
            "correlationId": "corr-rep-dup"
        }
        """.formatted(eventId, contratoId, freelancerId);

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        listener.handleContratoEvent(payload);

        verify(reputacaoService, never()).registrarContratoConcluido(any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }
}
