package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.time.Instant;

/** Alerte reçue sur {@code alerts/<parkingId>/<type>}. */
@Entity("alerts")
public class Alert extends NosqlEntity {

    public static final String OPEN = "open";
    public static final String ACKNOWLEDGED = "acknowledged";

    @Column private String parkingId;
    @Column private String type;       // parking_full | wrong_parking | plate_refused | thing_offline
    @Column private String status = OPEN;
    @Column private Integer slot;
    @Column private String plate;
    @Column private String message;
    @Column private Instant createdAt;
    @Column private Instant acknowledgedAt;

    public static Alert of(String parkingId, String type) {
        var a = new Alert();
        a.setId(UUIDv7.generate().toString());
        a.parkingId = parkingId;
        a.type = type;
        a.createdAt = Instant.now();
        return a;
    }

    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSlot() { return slot; }
    public void setSlot(Integer slot) { this.slot = slot; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
