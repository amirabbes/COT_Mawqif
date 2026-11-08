package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.time.Instant;

/** Une place de parking. Identifiant : "<parkingId>:<NN>" (ex. "parking-1:07"). */
@Entity("parking_slots")
public class ParkingSlot extends NosqlEntity {

    public static final String FREE = "free";
    public static final String OCCUPIED = "occupied";
    public static final String RESERVED = "reserved";

    @Column private String parkingId;
    @Column private int number;
    @Column private String state = FREE;
    @Column private String plate;
    @Column private Instant updatedAt;

    public static String idOf(String parkingId, int number) {
        return "%s:%02d".formatted(parkingId, number);
    }

    public static ParkingSlot of(String parkingId, int number) {
        var s = new ParkingSlot();
        s.setId(idOf(parkingId, number));
        s.parkingId = parkingId;
        s.number = number;
        return s;
    }

    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
