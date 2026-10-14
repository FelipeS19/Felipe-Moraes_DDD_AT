package br.com.freela.reputacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReputacaoService {
    private static final Logger log = LoggerFactory.getLogger(ReputacaoService.class);
    private final ReputacaoRepository repository;

    public ReputacaoService(ReputacaoRepository r) {
        this.repository = r;
    }

    @Transactional
    public void registrarContratoConcluido(UUID contratoId, UUID freelancerId, BigDecimal valor, String correlationId) {
        log.info("reputacao.atualizacao.inicio correlationId={} contratoId={} freelancerId={} valor={}",
                correlationId, contratoId, freelancerId, valor);
        var r = repository.findById(freelancerId).orElseGet(() -> new ReputacaoFreelancer(freelancerId));
        r.registrarContrato(valor);
        repository.save(r);
        log.info("reputacao.atualizacao.sucesso correlationId={} contratoId={} freelancerId={} contratosConcluidos={} valorTotal={}",
                correlationId, contratoId, freelancerId, r.getContratosConcluidos(), r.getValorTotal());
    }
}
