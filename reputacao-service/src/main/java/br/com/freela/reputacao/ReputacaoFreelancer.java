package br.com.freela.reputacao;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="reputacoes")
public class ReputacaoFreelancer {
    @Id
    private UUID freelancerId;
    private int contratosConcluidos;
    private BigDecimal valorTotal = BigDecimal.ZERO;

    protected ReputacaoFreelancer() {}

    public ReputacaoFreelancer(UUID id) {
        this.freelancerId = id;
    }

    public void registrarContrato(BigDecimal valor) {
        this.contratosConcluidos++;
        this.valorTotal = this.valorTotal.add(valor);
    }

    public UUID getFreelancerId() { return freelancerId; }
    public int getContratosConcluidos() { return contratosConcluidos; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
