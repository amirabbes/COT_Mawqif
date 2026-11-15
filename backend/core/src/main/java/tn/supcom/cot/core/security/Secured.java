package tn.supcom.cot.core.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protège une ressource ou une méthode REST : jeton Bearer valide obligatoire (émis par le service IAM).
 * {@code roles} : l'utilisateur doit avoir AU MOINS un de ces rôles (vide = tout utilisateur connecté).
 * {@code scopes} : le jeton doit contenir TOUS ces scopes.
 * Une annotation sur la méthode l'emporte sur celle de la classe.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
    Role[] roles() default {};

    String[] scopes() default {};
}
