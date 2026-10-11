package com.kyofoundation.skillnapse.modules.auth.service;

import com.kyofoundation.skillnapse.common.exception.UnauthorizedException;
import com.kyofoundation.skillnapse.modules.auth.dto.request.LoginRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.request.RegistroRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.response.LoginResponse;
import com.kyofoundation.skillnapse.modules.auth.dto.response.RegistroResponse;
import com.kyofoundation.skillnapse.modules.auth.entity.TokenAtualizacao;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.TokenAtualizacaoFinder;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.mapper.AuthMapper;
import com.kyofoundation.skillnapse.modules.auth.repository.TokenAtualizacaoRepository;
import com.kyofoundation.skillnapse.modules.auth.repository.UsuarioRepository;
import com.kyofoundation.skillnapse.modules.auth.support.JwtService;
import com.kyofoundation.skillnapse.modules.auth.validator.TokenAtualizacaoValidator;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenAtualizacaoRepository tokenAtualizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioValidator usuarioValidator;
    private final TokenAtualizacaoValidator tokenAtualizacaoValidator;
    private final JwtService jwtService;
    private final UserFinder userFinder;
    private final TokenAtualizacaoFinder tokenAtualizacaoFinder;

    @Transactional
    public RegistroResponse register(RegistroRequest request) {
        usuarioValidator.validarRegistro(request);

        String senhaHash = passwordEncoder.encode(request.senha());
        Usuario usuarioRegistrado = AuthMapper.registrar(request, senhaHash);
        usuarioRepository.save(usuarioRegistrado);

        return AuthMapper.toRegistroResponse(usuarioRegistrado);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        usuarioValidator.validarLogin(request);

        Usuario usuario = userFinder.findByEmail(request.email());

        usuarioValidator.validarCredenciais(usuario, request.senha());

        String accessToken = jwtService.gerarAccessToken(usuario);
        String refreshToken = jwtService.gerarRefreshToken();
        String tokenHash = jwtService.hashToken(refreshToken);
        Instant expiraEm = jwtService.calcularExpiracaoRefreshToken();

        TokenAtualizacao tokenAtualizacao = AuthMapper.toTokenAtualizacao(usuario, tokenHash, expiraEm);
        tokenAtualizacaoRepository.save(tokenAtualizacao);

        return AuthMapper.toLoginResponse(
                usuario,
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponse renovarToken(String refreshToken) {
        tokenAtualizacaoValidator.validarTokenString(refreshToken);

        String tokenHash = jwtService.hashToken(refreshToken);
        TokenAtualizacao tokenSalvo = tokenAtualizacaoFinder.findByTokenForUpdate(tokenHash);

        if (Boolean.TRUE.equals(tokenSalvo.getRevogado())) {
            // Detecção de reutilização de refresh token (Token Reuse Detection - RFC 6819)
            tokenAtualizacaoRepository.revogarTodosPorUsuario(tokenSalvo.getUsuario().getId());
            tokenAtualizacaoValidator.validarNaoRevogado(tokenSalvo);
        }

        if (tokenSalvo.getExpiraEm().isBefore(Instant.now())) {
            tokenSalvo.setRevogado(true);
            tokenAtualizacaoRepository.save(tokenSalvo);
            tokenAtualizacaoValidator.validarNaoExpirado(tokenSalvo);
        }

        Usuario usuario = tokenSalvo.getUsuario();
        usuarioValidator.validarUsuarioAtivo(usuario);

        // Rotação do refresh token
        tokenSalvo.setRevogado(true);
        tokenAtualizacaoRepository.save(tokenSalvo);

        String novoAccessToken = jwtService.gerarAccessToken(usuario);
        String novoRefreshToken = jwtService.gerarRefreshToken();
        String novoTokenHash = jwtService.hashToken(novoRefreshToken);
        Instant novaExpiracao = jwtService.calcularExpiracaoRefreshToken();

        TokenAtualizacao novoTokenAtualizacao = AuthMapper.toTokenAtualizacao(usuario, novoTokenHash, novaExpiracao);
        tokenAtualizacaoRepository.save(novoTokenAtualizacao);

        return AuthMapper.toLoginResponse(
                usuario,
                novoAccessToken,
                novoRefreshToken,
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void revogarToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = jwtService.hashToken(refreshToken);
            tokenAtualizacaoFinder.findByToken(tokenHash).ifPresent(token -> {
                token.setRevogado(true);
                tokenAtualizacaoRepository.save(token);
            });
        }
    }

    @Transactional(readOnly = true)
    public RegistroResponse obterUsuarioAutenticado(UUID userId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);
        return AuthMapper.toRegistroResponse(usuario);
    }
}
