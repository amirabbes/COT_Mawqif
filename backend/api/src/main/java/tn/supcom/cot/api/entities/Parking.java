package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.time.Instant;

/** Un parking et ses compteurs. L'identifiant est celui du contrat MQTT (ex. "parking-1"). */
@Entity("parkings")
public class Parking extends NosqlEntity {

    public static final String BARRIER_OPEN = "open";
    public static final String BARRIER_CLOSED = "closed";
    public static final String BARRIER_UNKNOWN = "unknown";

    @Column private String name;
    @Column private Double lat;
    @Column private Double lon;
    @Column private int totalSlots;
    @Column private int freeSlots;
    @Column private String barrierState = BARRIER_UNKNOWN;
    @Column private Instant updatedAt;

    public static Parking of(String id, String name) {
        var p = new Parking();
        p.setId(id);
        p.name = name;
        return p;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
    public int getFreeSlots() { return freeSlots; }
    public void setFreeSlots(int freeSlots) { this.freeSlots = freeSlots; }
    public String getBarrierState() { return barrierState; }
    public void setBarrierState(String barrierState) { this.barrierState = barrierState; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
