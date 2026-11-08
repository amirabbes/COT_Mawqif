package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.time.Instant;

/** Journal des messages {@code things/<thingId>/datum} (base des KPI et de l'historique). */
@Entity("data")
public class Datum extends NosqlEntity {

    @Column private String thingId;
    @Column private String event;
    @Column private String parkingId;
    @Column private String payload;      // message JSON d'origine
    @Column private Instant receivedAt;

    public static Datum of(String thingId, String event, String parkingId, String payload) {
        var d = new Datum();
        d.setId(UUIDv7.generate().toString());
        d.thingId = thingId;
        d.event = event;
        d.parkingId = parkingId;
        d.payload = payload;
        d.receivedAt = Instant.now();
        return d;
    }

    public String getThingId() { return thingId; }
    public void setThingId(String thingId) { this.thingId = thingId; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
