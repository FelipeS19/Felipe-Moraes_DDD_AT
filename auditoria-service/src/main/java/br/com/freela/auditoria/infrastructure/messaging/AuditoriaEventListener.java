package br.com.freela.auditoria.infrastructure.messaging;

import br.com.freela.auditoria.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class AuditoriaEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaEventListener.class);
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public AuditoriaEventListener(AuditoriaService auditoriaService, ObjectMapper objectMapper) {
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(attempts = "3", dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "contratos-events", groupId = "auditoria-group", concurrency = "3")
    @Transactional
    public void handleContratoEvent(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "DESCONHECIDO";
            UUID eventId = node.has("eventId") ? UUID.fromString(node.get("eventId").asText()) : UUID.randomUUID();
            UUID aggregateId = node.has("contratoId") ? UUID.fromString(node.get("contratoId").asText()) : null;
            String correlationId = node.has("correlationId") ? node.get("correlationId").asText() : "NO-CORRELATION-ID";

            MDC.put("correlationId", correlationId);
            MDC.put("eventId", eventId.toString());
            if (aggregateId != null) MDC.put("contratoId", aggregateId.toString());

            log.info("kafka.consumer.auditoria.recebido correlationId={} eventId={} contratoId={} eventType={}",
                    correlationId, eventId, aggregateId, eventType);

            auditoriaService.registrar(eventId, aggregateId, eventType, correlationId, payload);

            log.info("kafka.consumer.auditoria.processado.sucesso correlationId={} eventId={} contratoId={}",
                    correlationId, eventId, aggregateId);
        } catch (Exception e) {
            log.error("kafka.consumer.auditoria.erro payload={} erro={}", payload, e.getMessage(), e);
            throw new RuntimeException("Erro ao processar mensagem do Kafka na auditoria", e);
        } finally {
            MDC.clear();
        }
    }

    @DltHandler
    public void handleDltMessage(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("kafka.consumer.auditoria.dlt recebido no topico={} payload={}", topic, payload);
    }
}
