package br.com.freela.notificacao.infrastructure.messaging;

import br.com.freela.notificacao.NotificacaoService;
import br.com.freela.notificacao.infrastructure.idempotency.ProcessedEvent;
import br.com.freela.notificacao.infrastructure.idempotency.ProcessedEventRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ContratoEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContratoEventListener.class);
    private final NotificacaoService notificacaoService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public ContratoEventListener(NotificacaoService notificacaoService, 
                                 ProcessedEventRepository processedEventRepository, 
                                 ObjectMapper objectMapper) {
        this.notificacaoService = notificacaoService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(attempts = "3", dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "contratos-events", groupId = "notificacao-group", concurrency = "3")
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

            log.info("kafka.consumer.notificacao.recebido correlationId={} eventId={} contratoId={} eventType={}",
                    correlationId, eventId, contratoId, eventType);

            // Controle de idempotência
            if (processedEventRepository.existsById(eventId)) {
                log.warn("kafka.consumer.notificacao.duplicado correlationId={} eventId={} contratoId={} - evento descartado",
                        correlationId, eventId, contratoId);
                return;
            }

            UUID clienteId = node.has("clienteId") && !node.get("clienteId").isNull() ? UUID.fromString(node.get("clienteId").asText()) : null;
            UUID freelancerId = node.has("freelancerId") && !node.get("freelancerId").isNull() ? UUID.fromString(node.get("freelancerId").asText()) : null;

            switch (eventType) {
                case "ContratoCriado" -> {
                    String titulo = node.has("titulo") ? node.get("titulo").asText() : "Sem título";
                    if (clienteId != null) {
                        notificacaoService.registrar(contratoId, clienteId, "EMAIL",
                                "Seu contrato '" + titulo + "' foi criado com sucesso.", correlationId);
                    }
                    if (freelancerId != null) {
                        notificacaoService.registrar(contratoId, freelancerId, "APP_PUSH",
                                "Novo contrato associado a você: '" + titulo + "'", correlationId);
                    }
                }
                case "ContratoEntregaRegistrada" -> {
                    if (clienteId != null) {
                        notificacaoService.registrar(contratoId, clienteId, "EMAIL",
                                "O freelancer registrou a entrega do contrato. Por favor, revise e aprove.", correlationId);
                    }
                    if (freelancerId != null) {
                        notificacaoService.registrar(contratoId, freelancerId, "APP_PUSH",
                                "Sua entrega para o contrato foi registrada com sucesso.", correlationId);
                    }
                }
                case "ContratoConcluido" -> {
                    BigDecimal valor = node.has("valor") ? new BigDecimal(node.get("valor").asText()) : BigDecimal.ZERO;
                    if (clienteId != null) {
                        notificacaoService.registrar(contratoId, clienteId, "EMAIL",
                                "Contrato concluído com sucesso. Obrigado por utilizar a plataforma!", correlationId);
                    }
                    if (freelancerId != null) {
                        notificacaoService.registrar(contratoId, freelancerId, "APP_PUSH",
                                "Parabéns! Contrato concluído. Valor liberado: R$ " + valor, correlationId);
                    }
                }
                case "ContratoCancelado" -> {
                    if (clienteId != null) {
                        notificacaoService.registrar(contratoId, clienteId, "EMAIL",
                                "O contrato foi cancelado.", correlationId);
                    }
                    if (freelancerId != null) {
                        notificacaoService.registrar(contratoId, freelancerId, "APP_PUSH",
                                "O contrato foi cancelado.", correlationId);
                    }
                }
                default -> log.info("kafka.consumer.notificacao.ignorado correlationId={} eventType={}", correlationId, eventType);
            }

            processedEventRepository.save(new ProcessedEvent(eventId));
            log.info("kafka.consumer.notificacao.processado.sucesso correlationId={} eventId={} contratoId={}",
                    correlationId, eventId, contratoId);
        } catch (Exception e) {
            log.error("kafka.consumer.notificacao.erro payload={} erro={}", payload, e.getMessage(), e);
            throw new RuntimeException("Erro ao processar mensagem do Kafka", e);
        } finally {
            MDC.clear();
        }
    }

    @DltHandler
    public void handleDltMessage(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("kafka.consumer.notificacao.dlt recebido no topico={} payload={}", topic, payload);
    }
}
