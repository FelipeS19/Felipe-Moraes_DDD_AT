package br.com.freela.notificacao;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notificacoes")
public class Notificacao {
    @Id
    private UUID id;
    private UUID contratoId;
    private UUID destinatarioId;
    private String tipo;
    @Column(length=500)
    private String mensagem;
    private String correlationId;
    private Instant criadaEm;

    protected Notificacao() {}

    public Notificacao(UUID contratoId, UUID destinatarioId, String tipo, String mensagem, String correlationId) {
        this.id = UUID.randomUUID();
        this.contratoId = contratoId;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.correlationId = correlationId;
        this.criadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getContratoId() { return contratoId; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCriadaEm() { return criadaEm; }
}
