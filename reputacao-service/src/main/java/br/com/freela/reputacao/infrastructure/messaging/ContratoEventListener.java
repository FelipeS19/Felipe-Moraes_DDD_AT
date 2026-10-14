package br.com.freela.reputacao.infrastructure.messaging;

import br.com.freela.reputacao.ReputacaoService;
import br.com.freela.reputacao.infrastructure.idempotency.ProcessedEvent;
import br.com.freela.reputacao.infrastructure.idempotency.ProcessedEventRepository;
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
public class ContratoEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContratoEventListener.class);
    private final ReputacaoService reputacaoService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public ContratoEventListener(ReputacaoService reputacaoService, 
                                 ProcessedEventRepository processedEventRepository, 
                                 ObjectMapper objectMapper) {
        this.reputacaoService = reputacaoService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(attempts = "3", dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "contratos-events", groupId = "reputacao-group", concurrency = "3")
    @Transactional
    public void handleContratoEvent(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            String correlationId = node.has("correlationId") ? node.get("correlationId").asText() : "N/A";
            UUID eventId = node.has("eventId") ? UUID.fromString(node.get("eventId").asText()) : UUID.randomUUID();
            UUID contratoId = node.has("contratoId") ? UUID.fromString(node.get("contratoId").asText()) : null;

            MDC.put("correlationId", correlationId);
            MDC.put("eventId", eventId.toString());
            if (contratoId != null) MDC.put("contratoId", contratoId.toString());

            if ("ContratoConcluido".equals(eventType)) {
                var event = objectMapper.readValue(payload, ContratoConcluidoEvent.class);

                log.info("kafka.consumer.reputacao.recebido correlationId={} eventId={} contratoId={} freelancerId={} eventType={}",
                        correlationId, event.eventId(), event.contratoId(), event.freelancerId(), eventType);

                // Idempotência
                if (processedEventRepository.existsById(event.eventId())) {
                    log.warn("kafka.consumer.reputacao.duplicado correlationId={} eventId={} contratoId={} - evento descartado",
                            correlationId, event.eventId(), event.contratoId());
                    return;
                }

                reputacaoService.registrarContratoConcluido(event.contratoId(), event.freelancerId(), event.valor(), correlationId);

                processedEventRepository.save(new ProcessedEvent(event.eventId()));
                log.info("kafka.consumer.reputacao.processado.sucesso correlationId={} eventId={} contratoId={}",
                        correlationId, event.eventId(), event.contratoId());
            } else {
                log.info("kafka.consumer.reputacao.ignorado correlationId={} contratoId={} eventType={}",
                        correlationId, contratoId, eventType);
            }
        } catch (Exception e) {
            log.error("kafka.consumer.reputacao.erro payload={} erro={}", payload, e.getMessage(), e);
            throw new RuntimeException("Erro ao processar mensagem do Kafka", e);
        } finally {
            MDC.clear();
        }
    }

    @DltHandler
    public void handleDltMessage(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("kafka.consumer.reputacao.dlt recebido no topico={} payload={}", topic, payload);
    }
}
