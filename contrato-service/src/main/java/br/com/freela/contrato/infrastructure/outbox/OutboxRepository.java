package br.com.freela.contrato.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {
    
    @Query("SELECT o FROM OutboxMessage o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    List<OutboxMessage> findPendingMessages();
}
