package com.example.backend.security;

import com.example.backend.exception.TokenInvalidoException;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.util.ReactiveFirebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Verifica el idToken de Firebase contra el servicio de Firebase Authentication
 * (verifyIdTokenAsync, sin bloquear) y luego resuelve el rol del usuario
 * consultando el documento correspondiente en la colección "usuarios".
 */
@Component
public class FirebaseReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final FirebaseAuth firebaseAuth;
    private final UsuarioRepository usuarioRepository;

    public FirebaseReactiveAuthenticationManager(FirebaseAuth firebaseAuth, UsuarioRepository usuarioRepository) {
        this.firebaseAuth = firebaseAuth;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String idToken = (String) authentication.getCredentials();

        return ReactiveFirebase.<FirebaseToken>defer(() -> firebaseAuth.verifyIdTokenAsync(idToken))
                .onErrorMap(e -> new TokenInvalidoException("Token de Firebase inválido o expirado"))
                .flatMap(this::resolverUsuario)
                .map(FirebaseAuthenticationToken::new);
    }

    private Mono<UsuarioAutenticado> resolverUsuario(FirebaseToken token) {
        return usuarioRepository.findById(token.getUid())
                .switchIfEmpty(Mono.error(new TokenInvalidoException(
                        "El usuario autenticado no tiene un perfil registrado en OLITRACK")))
                .flatMap(this::validarActivo)
                .map(usuario -> new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRol()));
    }

    private Mono<Usuario> validarActivo(Usuario usuario) {
        if (!usuario.isActivo()) {
            return Mono.error(new TokenInvalidoException("El usuario está desactivado"));
        }
        return Mono.just(usuario);
    }
}
