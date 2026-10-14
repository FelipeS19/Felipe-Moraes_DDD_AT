package br.com.freela.contrato.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processPendingMessages() {
        List<OutboxMessage> pending = repository.findPendingMessages();
        
        for (OutboxMessage message : pending) {
            try {
                log.info("outbox.processando mensagemId={} eventType={} correlationId={}", 
                        message.getId(), message.getEventType(), message.getCorrelationId());
                
                var node = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(message.getPayload());
                node.put("correlationId", message.getCorrelationId());
                node.put("eventType", message.getEventType());
                
                String finalPayload = objectMapper.writeValueAsString(node);
                
                ProducerRecord<String, String> record = new ProducerRecord<>("contratos-events", message.getAggregateId(), finalPayload);
                if (message.getCorrelationId() != null) {
                    record.headers().add(new RecordHeader("X-Correlation-Id", message.getCorrelationId().getBytes(StandardCharsets.UTF_8)));
                }
                record.headers().add(new RecordHeader("eventType", message.getEventType().getBytes(StandardCharsets.UTF_8)));

                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                
                message.markAsProcessed();
                repository.save(message);
                
                log.info("outbox.processado.sucesso mensagemId={} eventType={} contratoId={}", 
                        message.getId(), message.getEventType(), message.getAggregateId());
            } catch (Exception e) {
                log.error("outbox.processado.erro mensagemId={} erro={}", message.getId(), e.getMessage());
            }
        }
    }
}
