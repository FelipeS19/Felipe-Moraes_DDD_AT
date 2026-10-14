package br.com.freela.auditoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditoriaServiceTest {

    private EventoAuditoriaRepository repository;
    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        repository = mock(EventoAuditoriaRepository.class);
        service = new AuditoriaService(repository);
    }

    @Test
    @DisplayName("Deve salvar evento na tabela de auditoria quando não existir previamente")
    void deveSalvarNovoEvento() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(eventId, UUID.randomUUID(), "ContratoCriado", "corr-1", "{}");

        verify(repository, times(1)).save(any(EventoAuditoria.class));
    }

    @Test
    @DisplayName("IDEMPOTÊNCIA: Não deve salvar duplicata se eventId já existir na auditoria")
    void deveIgnorarEventoDuplicado() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        service.registrar(eventId, UUID.randomUUID(), "ContratoCriado", "corr-dup", "{}");

        verify(repository, never()).save(any());
    }
}
