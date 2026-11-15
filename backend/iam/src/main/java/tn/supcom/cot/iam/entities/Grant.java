package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.time.Instant;

/** Trace de la dernière connexion d'un utilisateur via un client. Équivalent de Grant du cot.zip. */
@Entity("issued_grants")
public class Grant extends NosqlEntity {

    @Column private String tenantId;
    @Column private String identityId;
    @Column private String approvedScopes;
    @Column private Instant issuanceDateTime;

    /** La clé composite du cot.zip (GrantPK) devient un identifiant texte "tenantId:identityId". */
    public static String keyOf(String tenantId, String identityId) {
        return tenantId + ":" + identityId;
    }

    public static Grant of(String tenantId, String identityId, String approvedScopes) {
        var g = new Grant();
        g.setId(keyOf(tenantId, identityId));
        g.tenantId = tenantId;
        g.identityId = identityId;
        g.approvedScopes = approvedScopes;
        g.issuanceDateTime = Instant.now();
        return g;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }
    public String getApprovedScopes() { return approvedScopes; }
    public void setApprovedScopes(String approvedScopes) { this.approvedScopes = approvedScopes; }
    public Instant getIssuanceDateTime() { return issuanceDateTime; }
    public void setIssuanceDateTime(Instant issuanceDateTime) { this.issuanceDateTime = issuanceDateTime; }
}
