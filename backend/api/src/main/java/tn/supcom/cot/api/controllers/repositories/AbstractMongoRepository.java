package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.bson.Document;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

abstract class AbstractMongoRepository<T extends NosqlEntity> {
    private final MongoCollection<Document> collection;

    AbstractMongoRepository(MongoClient client, String collectionName) {
        var databaseName = ConfigProvider.getConfig().getValue("jnosql.document.database", String.class);
        this.collection = client.getDatabase(databaseName).getCollection(collectionName, Document.class);
    }

    protected MongoCollection<Document> collection() {
        return collection;
    }

    public Optional<T> findById(String id) {
        var document = collection.find(new Document("_id", id)).first();
        return Optional.ofNullable(document).map(this::fromDocument);
    }

    public List<T> findAll() {
        return collection.find().map(this::fromDocument).into(new ArrayList<>());
    }

    public T save(T entity) {
        var document = toDocument(entity);
        collection.replaceOne(new Document("_id", entity.getId()), document, new ReplaceOptions().upsert(true));
        return entity;
    }

    protected abstract T fromDocument(Document document);

    protected abstract Document toDocument(T entity);

    protected static Document baseDocument(NosqlEntity entity) {
        return new Document("_id", entity.getId()).append("version", entity.getVersion());
    }

    protected static void restoreBaseFields(NosqlEntity entity, Document document) {
        entity.setId(document.getString("_id"));
        entity.restoreVersion(document.getLong("version") == null ? 0L : document.getLong("version"));
    }
}
