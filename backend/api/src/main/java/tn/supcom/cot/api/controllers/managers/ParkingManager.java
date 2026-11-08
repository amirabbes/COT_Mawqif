package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import tn.supcom.cot.api.boundaries.Dtos.ParkingDto;
import tn.supcom.cot.api.boundaries.Dtos.SlotDto;
import tn.supcom.cot.api.controllers.repositories.ParkingRepository;
import tn.supcom.cot.api.controllers.repositories.ParkingSlotRepository;
import tn.supcom.cot.api.entities.Parking;
import tn.supcom.cot.api.entities.ParkingSlot;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Parkings, places et compteurs. Appelé par les ressources REST et par ThingManager (messages MQTT). */
@ApplicationScoped
public class ParkingManager {

    @Inject
    private ParkingRepository parkingRepository;
    @Inject
    private ParkingSlotRepository slotRepository;
    @Inject
    private WebSocketManager webSocketManager;

    public List<Parking> list() {
        return parkingRepository.findAll().stream().sorted(Comparator.comparing(Parking::getId)).toList();
    }

    public Parking find(String parkingId) {
        return parkingRepository.findById(parkingId)
                .orElseThrow(() -> new EntityNotFoundException("Parking introuvable : " + parkingId));
    }

    public List<ParkingSlot> slotsOf(String parkingId, String state) {
        find(parkingId); // 404 si le parking n'existe pas
        return slotRepository.findByParking(parkingId).stream()
                .filter(s -> state == null || state.equals(s.getState()))
                .sorted(Comparator.comparingInt(ParkingSlot::getNumber))
                .toList();
    }

    /** Message datum "slot_state" : met à jour la place, recalcule les compteurs, diffuse. */
    public synchronized void applySlotState(String parkingId, int number, String state, String plate) {
        var parking = ensureParking(parkingId);
        var existing = slotRepository.findById(ParkingSlot.idOf(parkingId, number));
        var slot = existing.map(Versions::bump).orElseGet(() -> ParkingSlot.of(parkingId, number));
        slot.setState(state);
        slot.setPlate(ParkingSlot.FREE.equals(state) ? null : plate);
        slot.setUpdatedAt(Instant.now());
        slotRepository.save(slot);

        var all = slotRepository.findByParking(parkingId);
        parking.setTotalSlots(all.size());
        parking.setFreeSlots((int) all.stream().filter(s -> ParkingSlot.FREE.equals(s.getState())).count());
        parking.setUpdatedAt(Instant.now());
        parkingRepository.save(Versions.bump(parking));

        webSocketManager.broadcast("slot_updated", SlotDto.from(slot), WebSocketManager.DRIVER, WebSocketManager.ADMIN);
        webSocketManager.broadcast("parking_updated", ParkingDto.from(parking), WebSocketManager.DRIVER, WebSocketManager.ADMIN);
    }

    /** Message datum "barrier_state". */
    public synchronized void applyBarrierState(String parkingId, String state) {
        var parking = ensureParking(parkingId);
        parking.setBarrierState(state);
        parking.setUpdatedAt(Instant.now());
        parkingRepository.save(Versions.bump(parking));
        webSocketManager.broadcast("parking_updated", ParkingDto.from(parking), WebSocketManager.DRIVER, WebSocketManager.ADMIN);
    }

    private Parking ensureParking(String parkingId) {
        return parkingRepository.findById(parkingId)
                .orElseGet(() -> parkingRepository.save(Parking.of(parkingId, parkingId)));
    }
}
