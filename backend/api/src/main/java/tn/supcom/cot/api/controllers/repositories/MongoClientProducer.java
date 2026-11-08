package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class MongoClientProducer {

    @Produces
    @ApplicationScoped
    public MongoClient mongoClient() {
        var uri = ConfigProvider.getConfig().getValue("jnosql.mongodb.url", String.class);
        return MongoClients.create(uri);
    }

    public void close(@Disposes MongoClient client) {
        client.close();
    }
}
