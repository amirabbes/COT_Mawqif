package tn.supcom.cot.iam.controllers.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.iam.entities.Tenant;

import java.util.Optional;

@Dependent
public class TenantRepository extends AbstractMongoRepository<Tenant> {

    @Inject
    public TenantRepository(MongoClient client) {
        super(client, "tenants");
    }

    public void ensureIndexes() {
        collection().createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    public Optional<Tenant> findByName(String name) {
        return Optional.ofNullable(collection().find(new Document("name", name)).first()).map(this::fromDocument);
    }

    @Override
    protected Tenant fromDocument(Document d) {
        var t = new Tenant();
        restoreBaseFields(t, d);
        t.setName(d.getString("name"));
        t.setSecretHash(d.getString("secretHash"));
        t.setRedirectUri(d.getString("redirectUri"));
        var roles = d.get("allowedRoles", Number.class);
        t.setAllowedRoles(roles == null ? 0L : roles.longValue());
        t.setAllowedScopes(d.getString("allowedScopes"));
        t.setSupportedGrantTypes(d.getString("supportedGrantTypes"));
        return t;
    }

    @Override
    protected Document toDocument(Tenant t) {
        return baseDocument(t)
                .append("name", t.getName())
                .append("secretHash", t.getSecretHash())
                .append("redirectUri", t.getRedirectUri())
                .append("allowedRoles", t.getAllowedRoles())
                .append("allowedScopes", t.getAllowedScopes())
                .append("supportedGrantTypes", t.getSupportedGrantTypes());
    }
}
