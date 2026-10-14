package br.com.freela.notificacao.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContratoCriadoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID contratoId,
        UUID clienteId,
        UUID freelancerId,
        String titulo,
        BigDecimal valor,
        String correlationId
) {}
