package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.util.Arrays;

/** Une application cliente (ici : driver-app, admin-app). Équivalent de Tenant du cot.zip. */
@Entity("tenants")
public class Tenant extends NosqlEntity {

    /** client_id, unique (ex. "driver-app"). */
    @Column private String name;
    /** Hash du secret ; null pour un client public (les PWA). */
    @Column private String secretHash;
    /** Réservé au flux authorization_code (pas encore utilisé). */
    @Column private String redirectUri;
    /** Rôles autorisés à se connecter via ce client (masque de bits). */
    @Column private long allowedRoles;
    /** Scopes maximum que ce client peut demander, séparés par des espaces. */
    @Column private String allowedScopes;
    /** Ex. "password refresh_token". */
    @Column private String supportedGrantTypes;

    public static Tenant of(String name, String secretHash, String redirectUri, long allowedRoles,
                            String allowedScopes, String supportedGrantTypes) {
        var t = new Tenant();
        t.setId(UUIDv7.generate().toString());
        t.name = name;
        t.secretHash = secretHash;
        t.redirectUri = redirectUri;
        t.allowedRoles = allowedRoles;
        t.allowedScopes = allowedScopes;
        t.supportedGrantTypes = supportedGrantTypes;
        return t;
    }

    public boolean supports(String grantType) {
        return supportedGrantTypes != null && Arrays.asList(supportedGrantTypes.trim().split("\\s+")).contains(grantType);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public long getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(long allowedRoles) { this.allowedRoles = allowedRoles; }
    public String getAllowedScopes() { return allowedScopes; }
    public void setAllowedScopes(String allowedScopes) { this.allowedScopes = allowedScopes; }
    public String getSupportedGrantTypes() { return supportedGrantTypes; }
    public void setSupportedGrantTypes(String supportedGrantTypes) { this.supportedGrantTypes = supportedGrantTypes; }
}
