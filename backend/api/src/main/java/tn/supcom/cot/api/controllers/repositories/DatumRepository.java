package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.Datum;

import java.util.Date;

@Dependent
public class DatumRepository extends AbstractMongoRepository<Datum> {

    @Inject
    public DatumRepository(MongoClient client) {
        super(client, "data");
    }

    @Override
    protected Datum fromDocument(Document document) {
        var datum = new Datum();
        restoreBaseFields(datum, document);
        datum.setThingId(document.getString("thingId"));
        datum.setEvent(document.getString("event"));
        datum.setParkingId(document.getString("parkingId"));
        datum.setPayload(document.getString("payload"));
        var receivedAt = document.getDate("receivedAt");
        datum.setReceivedAt(receivedAt == null ? null : receivedAt.toInstant());
        return datum;
    }

    @Override
    protected Document toDocument(Datum datum) {
        var document = baseDocument(datum)
                .append("thingId", datum.getThingId())
                .append("event", datum.getEvent())
                .append("parkingId", datum.getParkingId())
                .append("payload", datum.getPayload())
                .append("receivedAt", datum.getReceivedAt() == null ? null : Date.from(datum.getReceivedAt()));
        return document;
    }
}
