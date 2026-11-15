package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.time.Instant;

/** Réservation d'une place dans un parking pour une plaque et une période. Identifiant : UUIDv7. */
@Entity("reservations")
public class Reservation extends NosqlEntity {

    public static final String ACTIVE = "active";
    public static final String USED = "used";
    public static final String CANCELLED = "cancelled";
    public static final String EXPIRED = "expired";

    @Column private String parkingId;
    @Column private String plate;
    /** Identifiant (Identity.id) du compte qui a réservé. */
    @Column private String ownerId;
    @Column private String ownerName;
    @Column private Instant from;
    @Column private Instant to;
    @Column private String status = ACTIVE;
    @Column private Instant createdAt;

    public static Reservation of(String parkingId, String plate, String ownerId, String ownerName, Instant from, Instant to) {
        var r = new Reservation();
        r.setId(UUIDv7.generate().toString());
        r.parkingId = parkingId;
        r.plate = plate;
        r.ownerId = ownerId;
        r.ownerName = ownerName;
        r.from = from;
        r.to = to;
        r.createdAt = Instant.now();
        return r;
    }

    /** Vrai si la période [from, to) chevauche [otherFrom, otherTo). */
    public boolean overlaps(Instant otherFrom, Instant otherTo) {
        return from.isBefore(otherTo) && otherFrom.isBefore(to);
    }

    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
