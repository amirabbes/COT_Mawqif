package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.Reservation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dependent
public class ReservationRepository extends AbstractMongoRepository<Reservation> {

    @Inject
    public ReservationRepository(MongoClient client) {
        super(client, "reservations");
    }

    public List<Reservation> findByOwner(String ownerId) {
        return collection().find(new Document("ownerId", ownerId)).map(this::fromDocument).into(new ArrayList<>());
    }

    public List<Reservation> findByParking(String parkingId) {
        return collection().find(new Document("parkingId", parkingId)).map(this::fromDocument).into(new ArrayList<>());
    }

    @Override
    protected Reservation fromDocument(Document d) {
        var r = new Reservation();
        restoreBaseFields(r, d);
        r.setParkingId(d.getString("parkingId"));
        r.setPlate(d.getString("plate"));
        r.setOwnerId(d.getString("ownerId"));
        r.setOwnerName(d.getString("ownerName"));
        r.setFrom(d.getDate("from").toInstant());
        r.setTo(d.getDate("to").toInstant());
        r.setStatus(d.getString("status"));
        var created = d.getDate("createdAt");
        r.setCreatedAt(created == null ? null : created.toInstant());
        return r;
    }

    @Override
    protected Document toDocument(Reservation r) {
        return baseDocument(r)
                .append("parkingId", r.getParkingId())
                .append("plate", r.getPlate())
                .append("ownerId", r.getOwnerId())
                .append("ownerName", r.getOwnerName())
                .append("from", Date.from(r.getFrom()))
                .append("to", Date.from(r.getTo()))
                .append("status", r.getStatus())
                .append("createdAt", r.getCreatedAt() == null ? null : Date.from(r.getCreatedAt()));
    }
}
