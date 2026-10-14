package br.com.freela.contrato.domain;

import br.com.freela.contrato.domain.event.ContratoCancelado;
import br.com.freela.contrato.domain.event.ContratoConcluido;
import br.com.freela.contrato.domain.event.ContratoCriado;
import br.com.freela.contrato.domain.event.ContratoEntregaRegistrada;
import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.model.StatusContrato;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContratoDomainTest {

    private final UUID clienteId = UUID.randomUUID();
    private final UUID freelancerId = UUID.randomUUID();

    @Test
    @DisplayName("Deve criar contrato ativo e produzir evento ContratoCriado")
    void deveCriarContratoComSucesso() {
        var contrato = Contrato.criar(clienteId, freelancerId, "API Gateway", new BigDecimal("1500.00"));

        assertNotNull(contrato.id());
        assertEquals(StatusContrato.ATIVO, contrato.status());
        assertEquals("API Gateway", contrato.titulo());
        assertEquals(new BigDecimal("1500.00"), contrato.valor());

        var events = contrato.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst() instanceof ContratoCriado);
        var event = (ContratoCriado) events.getFirst();
        assertEquals(contrato.id(), event.contratoId());
        assertEquals(clienteId, event.clienteId());
        assertEquals(freelancerId, event.freelancerId());
    }

    @Test
    @DisplayName("Deve transitar para ENTREGA_REGISTRADA e produzir evento")
    void deveRegistrarEntrega() {
        var contrato = Contrato.criar(clienteId, freelancerId, "Desenvolvimento Web", new BigDecimal("2000.00"));
        contrato.pullDomainEvents(); // limpa evento de criação

        contrato.registrarEntrega();

        assertEquals(StatusContrato.ENTREGA_REGISTRADA, contrato.status());
        var events = contrato.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst() instanceof ContratoEntregaRegistrada);
        var event = (ContratoEntregaRegistrada) events.getFirst();
        assertEquals(contrato.id(), event.contratoId());
        assertEquals(clienteId, event.clienteId());
        assertEquals(freelancerId, event.freelancerId());
    }

    @Test
    @DisplayName("Deve transitar para CONCLUIDO e produzir evento")
    void deveConcluirContrato() {
        var contrato = Contrato.criar(clienteId, freelancerId, "Microserviços", new BigDecimal("3500.00"));
        contrato.registrarEntrega();
        contrato.pullDomainEvents(); // limpa eventos anteriores

        contrato.concluir();

        assertEquals(StatusContrato.CONCLUIDO, contrato.status());
        var events = contrato.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst() instanceof ContratoConcluido);
        var event = (ContratoConcluido) events.getFirst();
        assertEquals(contrato.id(), event.contratoId());
        assertEquals(freelancerId, event.freelancerId());
        assertEquals(new BigDecimal("3500.00"), event.valor());
    }

    @Test
    @DisplayName("Deve cancelar contrato ativo e produzir ContratoCancelado")
    void deveCancelarContratoAtivo() {
        var contrato = Contrato.criar(clienteId, freelancerId, "Design UI/UX", new BigDecimal("1200.00"));
        contrato.pullDomainEvents();

        contrato.cancelar();

        assertEquals(StatusContrato.CANCELADO, contrato.status());
        var events = contrato.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst() instanceof ContratoCancelado);
    }

    @Test
    @DisplayName("Não deve permitir concluir contrato antes de registrar entrega")
    void naoDeveConcluirSemEntrega() {
        var contrato = Contrato.criar(clienteId, freelancerId, "DevOps", new BigDecimal("2500.00"));

        assertThrows(IllegalStateException.class, contrato::concluir);
    }

    @Test
    @DisplayName("Não deve permitir cancelar contrato concluído")
    void naoDeveCancelarContratoConcluido() {
        var contrato = Contrato.criar(clienteId, freelancerId, "DevOps", new BigDecimal("2500.00"));
        contrato.registrarEntrega();
        contrato.concluir();

        assertThrows(IllegalStateException.class, contrato::cancelar);
    }
}
