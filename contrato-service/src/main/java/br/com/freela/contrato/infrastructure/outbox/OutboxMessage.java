package br.com.freela.contrato.infrastructure.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false)
    private String correlationId;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant processedAt;

    protected OutboxMessage() {}

    public OutboxMessage(String aggregateType, String aggregateId, String eventType, String payload, String correlationId) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
