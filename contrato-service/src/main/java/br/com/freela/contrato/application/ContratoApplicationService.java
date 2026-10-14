package br.com.freela.contrato.application;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.domain.shared.DomainEvent;
import br.com.freela.contrato.infrastructure.outbox.OutboxMessage;
import br.com.freela.contrato.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContratoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ContratoApplicationService.class);
    private final ContratoRepository repository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ContratoApplicationService(ContratoRepository repository, OutboxRepository outboxRepository, ObjectMapper objectMapper) { 
        this.repository = repository; 
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Contrato criar(CriarContratoCommand cmd, String correlationId) {
        String cid = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            log.info("contrato.criacao.inicio correlationId={} clienteId={} freelancerId={} titulo={} valor={}",
                    cid, cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
            Contrato contrato = Contrato.criar(cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
            MDC.put("contratoId", contrato.id().toString());
            log.info("contrato.dominio.criado correlationId={} contratoId={} status={} domainEvents={}",
                    cid, contrato.id(), contrato.status(), contrato.domainEvents().size());
            Contrato salvo = repository.salvar(contrato);

            // Publicação Transacional usando Outbox
            processarEventos(contrato, cid);

            log.info("contrato.criacao.sucesso correlationId={} contratoId={} clienteId={} freelancerId={} status={}",
                    cid, salvo.id(), salvo.clienteId(), salvo.freelancerId(), salvo.status());
            return salvo;
        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public void registrarEntrega(UUID id, String correlationId) {
        String cid = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        MDC.put("contratoId", id.toString());
        try {
            log.info("contrato.entrega.inicio correlationId={} contratoId={}", cid, id);
            var contrato = buscar(id);
            contrato.registrarEntrega();
            repository.salvar(contrato);
            processarEventos(contrato, cid);
            log.info("contrato.entrega.sucesso correlationId={} contratoId={}", cid, id);
        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public void concluir(UUID id, String correlationId) {
        String cid = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        MDC.put("contratoId", id.toString());
        try {
            log.info("contrato.conclusao.inicio correlationId={} contratoId={}", cid, id);
            var contrato = buscar(id);
            contrato.concluir();
            repository.salvar(contrato);
            processarEventos(contrato, cid);
            log.info("contrato.conclusao.sucesso correlationId={} contratoId={}", cid, id);
        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public void cancelar(UUID id, String correlationId) {
        String cid = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        MDC.put("contratoId", id.toString());
        try {
            log.info("contrato.cancelamento.inicio correlationId={} contratoId={}", cid, id);
            var contrato = buscar(id);
            contrato.cancelar();
            repository.salvar(contrato);
            processarEventos(contrato, cid);
            log.info("contrato.cancelamento.sucesso correlationId={} contratoId={}", cid, id);
        } finally {
            MDC.clear();
        }
    }

    private void processarEventos(Contrato contrato, String correlationId) {
        for (DomainEvent event : contrato.pullDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                var outboxMsg = new OutboxMessage("Contrato", contrato.id().toString(), event.eventType(), payload, correlationId);
                outboxRepository.save(outboxMsg);
                log.info("contrato.evento.outbox.salvo correlationId={} contratoId={} eventId={} eventType={}", correlationId, contrato.id(), event.eventId(), event.eventType());
            } catch (Exception e) {
                log.error("Erro ao serializar evento", e);
                throw new RuntimeException("Falha ao salvar evento no outbox", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public Contrato buscar(UUID id) {
        log.info("contrato.busca.inicio contratoId={}", id);
        var contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        log.info("contrato.busca.sucesso contratoId={} status={}", id, contrato.status());
        return contrato;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listar() {
        log.info("contrato.listagem.inicio");
        var contratos = repository.listar();
        log.info("contrato.listagem.sucesso quantidade={}", contratos.size());
        return contratos;
    }
}
