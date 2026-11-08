package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.Thing;

import java.util.Date;

@Dependent
public class ThingRepository extends AbstractMongoRepository<Thing> {

    @Inject
    public ThingRepository(MongoClient client) {
        super(client, "things");
    }

    @Override
    protected Thing fromDocument(Document document) {
        var thing = new Thing();
        restoreBaseFields(thing, document);
        thing.setType(document.getString("type"));
        thing.setParkingId(document.getString("parkingId"));
        thing.setState(document.getString("state"));
        var lastSeen = document.getDate("lastSeen");
        thing.setLastSeen(lastSeen == null ? null : lastSeen.toInstant());
        thing.setLastDatum(document.getString("lastDatum"));
        return thing;
    }

    @Override
    protected Document toDocument(Thing thing) {
        return baseDocument(thing)
                .append("type", thing.getType())
                .append("parkingId", thing.getParkingId())
                .append("state", thing.getState())
                .append("lastSeen", thing.getLastSeen() == null ? null : Date.from(thing.getLastSeen()))
                .append("lastDatum", thing.getLastDatum());
    }
}
