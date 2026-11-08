package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.ParkingSlot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dependent
public class ParkingSlotRepository extends AbstractMongoRepository<ParkingSlot> {

    @Inject
    public ParkingSlotRepository(MongoClient client) {
        super(client, "parking_slots");
    }

    public List<ParkingSlot> findByParking(String parkingId) {
        return collection().find(new Document("parkingId", parkingId))
                .map(this::fromDocument)
                .into(new ArrayList<>());
    }

    @Override
    protected ParkingSlot fromDocument(Document document) {
        var slot = new ParkingSlot();
        restoreBaseFields(slot, document);
        slot.setParkingId(document.getString("parkingId"));
        slot.setNumber(document.getInteger("number", 0));
        slot.setState(document.getString("state"));
        slot.setPlate(document.getString("plate"));
        var updatedAt = document.getDate("updatedAt");
        slot.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return slot;
    }

    @Override
    protected Document toDocument(ParkingSlot slot) {
        return baseDocument(slot)
                .append("parkingId", slot.getParkingId())
                .append("number", slot.getNumber())
                .append("state", slot.getState())
                .append("plate", slot.getPlate())
                .append("updatedAt", slot.getUpdatedAt() == null ? null : Date.from(slot.getUpdatedAt()));
    }
}
