package tn.supcom.cot.iam.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.iam.entities.Grant;

import java.util.Date;

@Dependent
public class GrantRepository extends AbstractMongoRepository<Grant> {

    @Inject
    public GrantRepository(MongoClient client) {
        super(client, "issued_grants");
    }

    @Override
    protected Grant fromDocument(Document d) {
        var g = new Grant();
        restoreBaseFields(g, d);
        g.setTenantId(d.getString("tenantId"));
        g.setIdentityId(d.getString("identityId"));
        g.setApprovedScopes(d.getString("approvedScopes"));
        var issued = d.getDate("issuanceDateTime");
        g.setIssuanceDateTime(issued == null ? null : issued.toInstant());
        return g;
    }

    @Override
    protected Document toDocument(Grant g) {
        return baseDocument(g)
                .append("tenantId", g.getTenantId())
                .append("identityId", g.getIdentityId())
                .append("approvedScopes", g.getApprovedScopes())
                .append("issuanceDateTime", g.getIssuanceDateTime() == null ? null : Date.from(g.getIssuanceDateTime()));
    }
}
