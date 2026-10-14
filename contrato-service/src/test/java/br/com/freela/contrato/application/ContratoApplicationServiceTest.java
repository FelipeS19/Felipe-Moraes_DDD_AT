package br.com.freela.contrato.application;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.infrastructure.outbox.OutboxMessage;
import br.com.freela.contrato.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContratoApplicationServiceTest {

    private ContratoRepository repository;
    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private ContratoApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ContratoRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ContratoApplicationService(repository, outboxRepository, objectMapper);
    }

    @Test
    @DisplayName("Ao criar contrato, deve salvar contrato e persistir evento na outbox")
    void deveCriarContratoEPersistirOutbox() {
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CriarContratoCommand(UUID.randomUUID(), UUID.randomUUID(), "Titulo Teste", new BigDecimal("1000.00"));
        var resultado = service.criar(cmd, "corr-123");

        assertNotNull(resultado);
        verify(repository, times(1)).salvar(any());

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository, times(1)).save(captor.capture());

        var outboxMsg = captor.getValue();
        assertEquals("ContratoCriado", outboxMsg.getEventType());
        assertEquals("corr-123", outboxMsg.getCorrelationId());
        assertEquals(resultado.id().toString(), outboxMsg.getAggregateId());
    }

    @Test
    @DisplayName("Ao registrar entrega, deve persistir ContratoEntregaRegistrada na outbox")
    void deveRegistrarEntregaEPersistirOutbox() {
        var contrato = Contrato.criar(UUID.randomUUID(), UUID.randomUUID(), "Titulo", new BigDecimal("1000.00"));
        contrato.pullDomainEvents(); // limpa evento criação

        when(repository.buscarPorId(contrato.id())).thenReturn(Optional.of(contrato));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrarEntrega(contrato.id(), "corr-entrega");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository, times(1)).save(captor.capture());
        assertEquals("ContratoEntregaRegistrada", captor.getValue().getEventType());
        assertEquals("corr-entrega", captor.getValue().getCorrelationId());
    }

    @Test
    @DisplayName("Ao concluir contrato, deve persistir ContratoConcluido na outbox")
    void deveConcluirEPersistirOutbox() {
        var contrato = Contrato.criar(UUID.randomUUID(), UUID.randomUUID(), "Titulo", new BigDecimal("1000.00"));
        contrato.registrarEntrega();
        contrato.pullDomainEvents();

        when(repository.buscarPorId(contrato.id())).thenReturn(Optional.of(contrato));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.concluir(contrato.id(), "corr-conclusao");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository, times(1)).save(captor.capture());
        assertEquals("ContratoConcluido", captor.getValue().getEventType());
        assertEquals("corr-conclusao", captor.getValue().getCorrelationId());
    }
}
