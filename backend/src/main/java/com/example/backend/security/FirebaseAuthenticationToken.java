package com.example.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication de Spring Security respaldado por un UsuarioAutenticado.
 *
 * Antes de autenticar: solo lleva el idToken crudo como credenciales.
 * Después de autenticar (FirebaseReactiveAuthenticationManager): lleva el
 * UsuarioAutenticado como principal y su rol como GrantedAuthority ("ROLE_X").
 */
public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credentials;

    /** Constructor para el token aún no autenticado (solo trae el idToken). */
    public FirebaseAuthenticationToken(String idToken) {
        super(List.of());
        this.principal = null;
        this.credentials = idToken;
        setAuthenticated(false);
    }

    /** Constructor para el token ya autenticado, con el usuario resuelto. */
    public FirebaseAuthenticationToken(UsuarioAutenticado usuario) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        this.principal = usuario;
        this.credentials = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
