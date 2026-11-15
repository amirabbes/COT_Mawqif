package tn.supcom.cot.iam.controllers.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.iam.entities.Identity;

import java.util.Date;
import java.util.Optional;

@Dependent
public class IdentityRepository extends AbstractMongoRepository<Identity> {

    @Inject
    public IdentityRepository(MongoClient client) {
        super(client, "identities");
    }

    /** Garantit l'unicité du nom d'utilisateur même en cas d'inscriptions simultanées. */
    public void ensureIndexes() {
        collection().createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
    }

    public Optional<Identity> findByUsername(String username) {
        return Optional.ofNullable(collection().find(new Document("username", username)).first()).map(this::fromDocument);
    }

    @Override
    protected Identity fromDocument(Document d) {
        var i = new Identity();
        restoreBaseFields(i, d);
        i.setUsername(d.getString("username"));
        i.setPasswordHash(d.getString("passwordHash"));
        var roles = d.get("roles", Number.class);
        i.setRoles(roles == null ? 0L : roles.longValue());
        i.setProvidedScopes(d.getString("providedScopes"));
        var created = d.getDate("createdAt");
        i.setCreatedAt(created == null ? null : created.toInstant());
        return i;
    }

    @Override
    protected Document toDocument(Identity i) {
        return baseDocument(i)
                .append("username", i.getUsername())
                .append("passwordHash", i.getPasswordHash())
                .append("roles", i.getRoles())
                .append("providedScopes", i.getProvidedScopes())
                .append("createdAt", i.getCreatedAt() == null ? null : Date.from(i.getCreatedAt()));
    }
}
