package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.security.Principal;
import java.time.Instant;

/** Un utilisateur (automobiliste, opérateur, admin). Équivalent de Identity du cot.zip, version MongoDB. */
@Entity("identities")
public class Identity extends NosqlEntity implements Principal {

    @Column private String username;
    /** Jamais le mot de passe en clair : format produit par PasswordHasher. */
    @Column private String passwordHash;
    /** Masque de bits des rôles (voir Role). */
    @Column private long roles;
    /** Scopes maximum que cet utilisateur peut obtenir, séparés par des espaces. */
    @Column private String providedScopes;
    @Column private Instant createdAt;

    public static Identity of(String username, String passwordHash, long roles, String providedScopes) {
        var i = new Identity();
        i.setId(UUIDv7.generate().toString());
        i.username = username;
        i.passwordHash = passwordHash;
        i.roles = roles;
        i.providedScopes = providedScopes;
        i.createdAt = Instant.now();
        return i;
    }

    @Override
    public String getName() { return username; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public long getRoles() { return roles; }
    public void setRoles(long roles) { this.roles = roles; }
    public String getProvidedScopes() { return providedScopes; }
    public void setProvidedScopes(String providedScopes) { this.providedScopes = providedScopes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
