package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.Alert;

import java.util.Date;

@Dependent
public class AlertRepository extends AbstractMongoRepository<Alert> {

    @Inject
    public AlertRepository(MongoClient client) {
        super(client, "alerts");
    }

    @Override
    protected Alert fromDocument(Document document) {
        var alert = new Alert();
        restoreBaseFields(alert, document);
        alert.setParkingId(document.getString("parkingId"));
        alert.setType(document.getString("type"));
        alert.setStatus(document.getString("status"));
        alert.setSlot(document.getInteger("slot"));
        alert.setPlate(document.getString("plate"));
        alert.setMessage(document.getString("message"));
        var createdAt = document.getDate("createdAt");
        alert.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        var acknowledgedAt = document.getDate("acknowledgedAt");
        alert.setAcknowledgedAt(acknowledgedAt == null ? null : acknowledgedAt.toInstant());
        return alert;
    }

    @Override
    protected Document toDocument(Alert alert) {
        return baseDocument(alert)
                .append("parkingId", alert.getParkingId())
                .append("type", alert.getType())
                .append("status", alert.getStatus())
                .append("slot", alert.getSlot())
                .append("plate", alert.getPlate())
                .append("message", alert.getMessage())
                .append("createdAt", alert.getCreatedAt() == null ? null : Date.from(alert.getCreatedAt()))
                .append("acknowledgedAt", alert.getAcknowledgedAt() == null ? null : Date.from(alert.getAcknowledgedAt()));
    }
}
