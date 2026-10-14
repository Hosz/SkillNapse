package com.kyofoundation.skillnapse.modules.questao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.service.OfensivaService;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.questao.dto.request.ResponderQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.HistoricoTentativaResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.ResultadoResolucaoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.AlternativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.entity.TentativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.finder.AlternativaQuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.QuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.SimuladoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.mapper.ResolucaoQuestaoMapper;
import com.kyofoundation.skillnapse.modules.questao.repository.TentativaQuestaoRepository;
import com.kyofoundation.skillnapse.modules.questao.support.ProficienciaTopicoSupport;
import com.kyofoundation.skillnapse.modules.questao.validator.ResolucaoQuestaoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolucaoQuestaoService {

    private final TentativaQuestaoRepository tentativaQuestaoRepository;
    private final QuestaoFinder questaoFinder;
    private final AlternativaQuestaoFinder alternativaQuestaoFinder;
    private final SimuladoFinder simuladoFinder;
    private final TopicoFinder topicoFinder;
    private final TentativaQuestaoFinder tentativaQuestaoFinder;
    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final ResolucaoQuestaoValidator resolucaoQuestaoValidator;
    private final OfensivaService ofensivaService;
    private final ProficienciaTopicoSupport proficienciaTopicoSupport;

    @Transactional
    public ResultadoResolucaoResponse responder(UUID questaoId, ResponderQuestaoRequest request, UUID userId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Questao questao = questaoFinder.findByIdComAlternativas(questaoId);
        AlternativaQuestao alternativaEscolhida = alternativaQuestaoFinder.findById(request.alternativaEscolhidaId());
        Simulado simulado = request.simuladoId() != null ? simuladoFinder.findById(request.simuladoId()) : null;
        Topico topico = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;

        resolucaoQuestaoValidator.validarResolucao(
                questao,
                alternativaEscolhida,
                request.tempoGastoSegundos(),
                simulado,
                topico,
                usuario
        );

        AlternativaQuestao alternativaCorreta = alternativaQuestaoFinder.findCorretaPorQuestaoId(questao.getId());
        boolean acertou = Boolean.TRUE.equals(alternativaEscolhida.getCorreta());

        Optional<TentativaQuestao> tentativaExistente = (simulado != null)
                ? tentativaQuestaoRepository.findFirstByUsuarioAndQuestaoAndSimulado(usuario, questao, simulado)
                : Optional.empty();

        TentativaQuestao tentativa;
        if (tentativaExistente.isPresent()) {
            tentativa = tentativaExistente.get();
            tentativa.setAlternativaEscolhida(alternativaEscolhida);
            tentativa.setAcertou(acertou);
            tentativa.setTempoGastoSegundos(request.tempoGastoSegundos());
            tentativa.setRespondidoEm(Instant.now());
            if (topico != null) {
                tentativa.setTopico(topico);
            }
        } else {
            tentativa = ResolucaoQuestaoMapper.toEntity(
                    usuario,
                    questao,
                    alternativaEscolhida,
                    acertou,
                    request.tempoGastoSegundos(),
                    simulado,
                    topico
            );
        }

        TentativaQuestao salva = tentativaQuestaoRepository.save(tentativa);

        LocalDate dataEstudo = salva.getRespondidoEm() != null
                ? salva.getRespondidoEm().atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(ZoneOffset.UTC);
        ofensivaService.registrarEstudoSilencioso(usuario, dataEstudo);

        if (simulado == null && topico != null) {
            proficienciaTopicoSupport.atualizarProficienciaTopico(usuario, topico);
        }

        return ResolucaoQuestaoMapper.toResultadoResponse(salva, alternativaCorreta);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoTentativaResponse> buscarHistorico(UUID userId, Boolean acertou, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Page<TentativaQuestao> pagina = tentativaQuestaoFinder.buscarPorUsuario(usuario, acertou, pageable);
        return pagina.map(ResolucaoQuestaoMapper::toHistoricoResponse);
    }
}
