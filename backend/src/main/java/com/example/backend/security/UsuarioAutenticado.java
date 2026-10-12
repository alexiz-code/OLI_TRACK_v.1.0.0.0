package com.example.backend.security;

import com.example.backend.model.Rol;

/**
 * Principal de seguridad: lo que queda disponible en el SecurityContext tras
 * validar el idToken de Firebase y resolver el rol del usuario en Firestore.
 */
public class UsuarioAutenticado {

    private final String uid;
    private final String email;
    private final Rol rol;

    public UsuarioAutenticado(String uid, String email, Rol rol) {
        this.uid = uid;
        this.email = email;
        this.rol = rol;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public Rol getRol() {
        return rol;
    }
}
