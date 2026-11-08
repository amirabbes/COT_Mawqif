package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.Parking;

import java.util.Date;

@Dependent
public class ParkingRepository extends AbstractMongoRepository<Parking> {

    @Inject
    public ParkingRepository(MongoClient client) {
        super(client, "parkings");
    }

    @Override
    protected Parking fromDocument(Document document) {
        var parking = new Parking();
        restoreBaseFields(parking, document);
        parking.setName(document.getString("name"));
        parking.setLat(document.getDouble("lat"));
        parking.setLon(document.getDouble("lon"));
        parking.setTotalSlots(document.getInteger("totalSlots", 0));
        parking.setFreeSlots(document.getInteger("freeSlots", 0));
        parking.setBarrierState(document.getString("barrierState"));
        var updatedAt = document.getDate("updatedAt");
        parking.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return parking;
    }

    @Override
    protected Document toDocument(Parking parking) {
        return baseDocument(parking)
                .append("name", parking.getName())
                .append("lat", parking.getLat())
                .append("lon", parking.getLon())
                .append("totalSlots", parking.getTotalSlots())
                .append("freeSlots", parking.getFreeSlots())
                .append("barrierState", parking.getBarrierState())
                .append("updatedAt", parking.getUpdatedAt() == null ? null : Date.from(parking.getUpdatedAt()));
    }
}
