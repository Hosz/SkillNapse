package com.kyofoundation.skillnapse.modules.auth.service;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.UnauthorizedException;
import com.kyofoundation.skillnapse.modules.auth.dto.request.LoginRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.request.RegistroRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.response.LoginResponse;
import com.kyofoundation.skillnapse.modules.auth.dto.response.RegistroResponse;
import com.kyofoundation.skillnapse.modules.auth.entity.TokenAtualizacao;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.TokenAtualizacaoFinder;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.repository.TokenAtualizacaoRepository;
import com.kyofoundation.skillnapse.modules.auth.repository.UsuarioRepository;
import com.kyofoundation.skillnapse.modules.auth.support.JwtService;
import com.kyofoundation.skillnapse.modules.auth.validator.TokenAtualizacaoValidator;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenAtualizacaoRepository tokenAtualizacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserFinder userFinder;

    @Mock
    private TokenAtualizacaoFinder tokenAtualizacaoFinder;

    @Mock
    private TokenAtualizacaoValidator tokenAtualizacaoValidator;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso e codificar senha")
    void deveRegistrarUsuarioComSucesso() {
        RegistroRequest request = new RegistroRequest("Aluno Kyo", "aluno@skillnapse.com", "senha123");

        doNothing().when(usuarioValidator).validarRegistro(request);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashSeguro");

        RegistroResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.nome()).isEqualTo("Aluno Kyo");
        assertThat(response.email()).isEqualTo("aluno@skillnapse.com");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario salvo = usuarioCaptor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Aluno Kyo");
        assertThat(salvo.getEmail()).isEqualTo("aluno@skillnapse.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("$2a$10$hashSeguro");
        assertThat(salvo.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve autenticar usuário com credenciais válidas e gerar Access e Refresh tokens")
    void deveRealizarLoginComSucessoEGerarTokens() {
        LoginRequest request = new LoginRequest("aluno@skillnapse.com", "senha123");
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nome("Aluno Kyo")
                .email("aluno@skillnapse.com")
                .senhaHash("$2a$10$hashSeguro")
                .ativo(true)
                .build();

        doNothing().when(usuarioValidator).validarLogin(request);
        when(userFinder.findByEmail("aluno@skillnapse.com")).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarCredenciais(usuario, "senha123");

        when(jwtService.gerarAccessToken(usuario)).thenReturn("mock.access.jwt");
        when(jwtService.gerarRefreshToken()).thenReturn("mock-refresh-token-uuid");
        when(jwtService.hashToken("mock-refresh-token-uuid")).thenReturn("hash-mock-refresh-token-uuid");
        when(jwtService.calcularExpiracaoRefreshToken()).thenReturn(Instant.now().plusSeconds(604800));
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(usuarioId);
        assertThat(response.nome()).isEqualTo("Aluno Kyo");
        assertThat(response.email()).isEqualTo("aluno@skillnapse.com");
        assertThat(response.accessToken()).isEqualTo("mock.access.jwt");
        assertThat(response.refreshToken()).isEqualTo("mock-refresh-token-uuid");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);

        ArgumentCaptor<TokenAtualizacao> tokenCaptor = ArgumentCaptor.forClass(TokenAtualizacao.class);
        verify(tokenAtualizacaoRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("hash-mock-refresh-token-uuid");
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando email não for encontrado no login")
    void deveLancarUnauthorizedExceptionQuandoEmailNaoEncontrado() {
        LoginRequest request = new LoginRequest("inexistente@skillnapse.com", "senha123");

        doNothing().when(usuarioValidator).validarLogin(request);
        when(userFinder.findByEmail("inexistente@skillnapse.com"))
                .thenThrow(new UnauthorizedException("Credenciais inválidas: e-mail ou senha incorretos."));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Credenciais inválidas: e-mail ou senha incorretos.");
    }

    @Test
    @DisplayName("Deve renovar token com sucesso através de rotação de refresh token com hash e lock")
    void deveRenovarTokenComSucesso() {
        String tokenAntigo = "refresh-token-antigo";
        String hashAntigo = "hash-refresh-token-antigo";
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nome("Aluno Kyo")
                .email("aluno@skillnapse.com")
                .ativo(true)
                .build();

        TokenAtualizacao tokenSalvo = TokenAtualizacao.builder()
                .token(hashAntigo)
                .usuario(usuario)
                .expiraEm(Instant.now().plusSeconds(3600))
                .revogado(false)
                .build();

        when(jwtService.hashToken(tokenAntigo)).thenReturn(hashAntigo);
        when(tokenAtualizacaoFinder.findByTokenForUpdate(hashAntigo)).thenReturn(tokenSalvo);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);

        when(jwtService.gerarAccessToken(usuario)).thenReturn("novo.access.jwt");
        when(jwtService.gerarRefreshToken()).thenReturn("novo-refresh-token-uuid");
        when(jwtService.hashToken("novo-refresh-token-uuid")).thenReturn("hash-novo-refresh-token-uuid");
        when(jwtService.calcularExpiracaoRefreshToken()).thenReturn(Instant.now().plusSeconds(604800));
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.renovarToken(tokenAntigo);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("novo.access.jwt");
        assertThat(response.refreshToken()).isEqualTo("novo-refresh-token-uuid");
        assertThat(tokenSalvo.getRevogado()).isTrue();
    }

    @Test
    @DisplayName("Deve detectar tentativa de reutilização de token revogado e revogar todas as sessões do usuário")
    void deveDetectarReutilizacaoDeTokenERevogarSessoes() {
        String tokenRevogado = "refresh-token-ja-usado";
        String hashRevogado = "hash-token-ja-usado";
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nome("Aluno Kyo")
                .email("aluno@skillnapse.com")
                .ativo(true)
                .build();

        TokenAtualizacao tokenSalvo = TokenAtualizacao.builder()
                .token(hashRevogado)
                .usuario(usuario)
                .expiraEm(Instant.now().plusSeconds(3600))
                .revogado(true)
                .build();

        when(jwtService.hashToken(tokenRevogado)).thenReturn(hashRevogado);
        when(tokenAtualizacaoFinder.findByTokenForUpdate(hashRevogado)).thenReturn(tokenSalvo);
        org.mockito.Mockito.doThrow(new UnauthorizedException("Tentativa de reutilização de token detectada. A sessão foi invalidada por segurança."))
                .when(tokenAtualizacaoValidator).validarNaoRevogado(tokenSalvo);

        assertThatThrownBy(() -> authService.renovarToken(tokenRevogado))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Tentativa de reutilização de token detectada. A sessão foi invalidada por segurança.");

        verify(tokenAtualizacaoRepository).revogarTodosPorUsuario(usuarioId);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException ao tentar renovar token expirado")
    void deveLancarUnauthorizedExceptionQuandoRefreshTokenExpirado() {
        String tokenExpirado = "refresh-token-expirado";
        String hashExpirado = "hash-token-expirado";
        TokenAtualizacao tokenSalvo = TokenAtualizacao.builder()
                .token(hashExpirado)
                .expiraEm(Instant.now().minusSeconds(100))
                .revogado(false)
                .build();

        when(jwtService.hashToken(tokenExpirado)).thenReturn(hashExpirado);
        when(tokenAtualizacaoFinder.findByTokenForUpdate(hashExpirado)).thenReturn(tokenSalvo);
        org.mockito.Mockito.doThrow(new UnauthorizedException("Token de atualização expirado."))
                .when(tokenAtualizacaoValidator).validarNaoExpirado(tokenSalvo);

        assertThatThrownBy(() -> authService.renovarToken(tokenExpirado))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token de atualização expirado.");

        assertThat(tokenSalvo.getRevogado()).isTrue();
        verify(tokenAtualizacaoRepository).save(tokenSalvo);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando refresh token for nulo ou vazio")
    void deveLancarBadRequestExceptionQuandoRefreshTokenVazio() {
        org.mockito.Mockito.doThrow(new BadRequestException("O token de atualização não pode ser nulo ou vazio."))
                .when(tokenAtualizacaoValidator).validarTokenString("");

        assertThatThrownBy(() -> authService.renovarToken(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O token de atualização não pode ser nulo ou vazio.");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando refresh token exceder 255 caracteres")
    void deveLancarBadRequestExceptionQuandoRefreshTokenExceder255Caracteres() {
        String tokenLongo = "T".repeat(256);
        org.mockito.Mockito.doThrow(new BadRequestException("O token de atualização não pode ter mais de 255 caracteres."))
                .when(tokenAtualizacaoValidator).validarTokenString(tokenLongo);

        assertThatThrownBy(() -> authService.renovarToken(tokenLongo))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O token de atualização não pode ter mais de 255 caracteres.");
    }

    @Test
    @DisplayName("Deve revogar token com sucesso ao chamar revogarToken")
    void deveRevogarTokenComSucesso() {
        String token = "token-para-revogar";
        String tokenHash = "hash-token-para-revogar";
        TokenAtualizacao tokenSalvo = TokenAtualizacao.builder()
                .token(tokenHash)
                .revogado(false)
                .build();

        when(jwtService.hashToken(token)).thenReturn(tokenHash);
        when(tokenAtualizacaoFinder.findByToken(tokenHash)).thenReturn(Optional.of(tokenSalvo));

        authService.revogarToken(token);

        assertThat(tokenSalvo.getRevogado()).isTrue();
        verify(tokenAtualizacaoRepository).save(tokenSalvo);
    }

    @Test
    @DisplayName("Deve obter dados do usuario autenticado com sucesso")
    void deveObterUsuarioAutenticadoComSucesso() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Aluno Kyo")
                .email("aluno@skillnapse.com")
                .ativo(true)
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);

        RegistroResponse response = authService.obterUsuarioAutenticado(usuario.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(usuario.getId());
        assertThat(response.nome()).isEqualTo("Aluno Kyo");
        assertThat(response.email()).isEqualTo("aluno@skillnapse.com");

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
    }
}
