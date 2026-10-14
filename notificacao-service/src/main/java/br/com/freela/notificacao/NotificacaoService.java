package br.com.freela.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private final NotificacaoRepository repository;

    public NotificacaoService(NotificacaoRepository r) {
        this.repository = r;
    }

    @Transactional
    public void registrar(UUID contratoId, UUID destinatarioId, String tipo, String mensagem, String correlationId) {
        log.info("notificacao.registro.inicio correlationId={} contratoId={} destinatarioId={} tipo={}",
                correlationId, contratoId, destinatarioId, tipo);
        var n = repository.save(new Notificacao(contratoId, destinatarioId, tipo, mensagem, correlationId));
        log.info("notificacao.registro.sucesso correlationId={} notificacaoId={} contratoId={} destinatarioId={}",
                correlationId, n.getId(), contratoId, destinatarioId);
    }
}
