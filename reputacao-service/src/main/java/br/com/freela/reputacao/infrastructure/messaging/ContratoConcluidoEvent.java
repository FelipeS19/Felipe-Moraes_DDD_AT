package br.com.freela.reputacao.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContratoConcluidoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID contratoId,
        UUID freelancerId,
        BigDecimal valor,
        String correlationId
) {}
