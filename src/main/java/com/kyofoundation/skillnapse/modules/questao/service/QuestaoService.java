package com.kyofoundation.skillnapse.modules.questao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoDetalheResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoResumoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;
import com.kyofoundation.skillnapse.modules.questao.finder.QuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.mapper.QuestaoMapper;
import com.kyofoundation.skillnapse.modules.questao.repository.QuestaoRepository;
import com.kyofoundation.skillnapse.modules.questao.support.HashEnunciadoSupport;
import com.kyofoundation.skillnapse.modules.questao.validator.QuestaoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final QuestaoRepository questaoRepository;
    private final QuestaoFinder questaoFinder;
    private final QuestaoValidator questaoValidator;
    private final HashEnunciadoSupport hashEnunciadoSupport;
    private final TopicoFinder topicoFinder;

    @Transactional
    public QuestaoDetalheResponse criar(UUID userId, CriarQuestaoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        String hash = hashEnunciadoSupport.gerarHash(request.enunciado());
        questaoValidator.validarCriacao(request, hash);
        Questao questao = QuestaoMapper.toEntity(request, hash);
        Questao salva = questaoRepository.save(questao);
        return QuestaoMapper.toDetalheResponse(salva);
    }

    @Transactional(readOnly = true)
    public Page<QuestaoResumoResponse> buscarComFiltros(
            UUID userId,
            String assuntoGeral,
            String topicoReferencia,
            String banca,
            Integer ano,
            DificuldadeQuestao dificuldade,
            String termoBusca,
            Pageable pageable
    ) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Page<Questao> pagina = questaoFinder.buscarComFiltros(
                assuntoGeral,
                topicoReferencia,
                banca,
                ano,
                dificuldade,
                termoBusca,
                pageable
        );
        return pagina.map(QuestaoMapper::toResumoResponse);
    }

    @Transactional(readOnly = true)
    public QuestaoDetalheResponse buscarPorId(UUID userId, UUID id) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Questao questao = questaoFinder.findByIdComAlternativas(id);
        return QuestaoMapper.toDetalheResponse(questao);
    }

    @Transactional(readOnly = true)
    public List<QuestaoDetalheResponse> listarPorTopico(UUID userId, UUID topicoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Topico topico = topicoFinder.findById(topicoId);
        String topicoTitulo = topico.getTitulo() != null ? topico.getTitulo().trim() : "";

        List<Questao> questoes = questaoRepository.buscarQuestoesPorTopico(topicoId, topicoTitulo);
        return questoes.stream().map(QuestaoMapper::toDetalheResponse).toList();
    }
}
