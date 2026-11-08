package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityNotFoundException;
import tn.supcom.cot.api.boundaries.Dtos.ThingDto;
import tn.supcom.cot.api.controllers.repositories.DatumRepository;
import tn.supcom.cot.api.controllers.repositories.ThingRepository;
import tn.supcom.cot.api.entities.Datum;
import tn.supcom.cot.api.entities.Thing;
import tn.supcom.cot.api.mqtt.Topics;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/** Objets connectés : traite les messages things/<id>/status et things/<id>/datum du contrat MQTT. */
@ApplicationScoped
public class ThingManager {
    private static final Logger LOG = Logger.getLogger(ThingManager.class.getName());

    @Inject
    private ThingRepository thingRepository;
    @Inject
    private DatumRepository datumRepository;
    @Inject
    private ParkingManager parkingManager;
    @Inject
    private AlertManager alertManager;
    @Inject
    private WebSocketManager webSocketManager;

    public List<Thing> list(String parkingId, String state) {
        return thingRepository.findAll().stream()
                .filter(t -> parkingId == null || parkingId.equals(t.getParkingId()))
                .filter(t -> state == null || state.equals(t.getState()))
                .sorted(Comparator.comparing(Thing::getId))
                .toList();
    }

    public Thing find(String thingId) {
        return thingRepository.findById(thingId)
                .orElseThrow(() -> new EntityNotFoundException("Objet introuvable : " + thingId));
    }

    /** things/<thingId>/status : {"state":"online"|"offline"}. */
    public void updateStatus(String thingId, String s) {
        var state = parse(s).getString("state", Thing.OFFLINE);
        var thing = thingRepository.findById(thingId).map(Versions::bump).orElseGet(() -> Thing.of(thingId));
        var wasOnline = Thing.ONLINE.equals(thing.getState());
        thing.setState(state);
        thing.setLastSeen(Instant.now());
        thingRepository.save(thing);
        webSocketManager.broadcast("thing_status", ThingDto.from(thing), WebSocketManager.ADMIN);

        if (wasOnline && Thing.OFFLINE.equals(state) && thing.getParkingId() != null) {
            alertManager.raise(thing.getParkingId(), "thing_offline", null, null, thingId + " hors ligne");
        }
    }

    /** things/<thingId>/datum : le champ "event" choisit le traitement. */
    public void updateDatum(String thingId, byte[] payload) {
        var text = new String(payload, StandardCharsets.UTF_8);
        var json = parse(text);
        var event = json.getString("event", "unknown");
        var parkingId = json.getString("parking", null);

        datumRepository.save(Datum.of(thingId, event, parkingId, text));

        var thing = thingRepository.findById(thingId).map(Versions::bump).orElseGet(() -> Thing.of(thingId));
        thing.setState(Thing.ONLINE);
        thing.setLastSeen(Instant.now());
        thing.setLastDatum(text);
        if (parkingId != null) thing.setParkingId(parkingId);
        thingRepository.save(thing);
        webSocketManager.broadcast("thing_status", ThingDto.from(thing), WebSocketManager.ADMIN);

        if (parkingId == null) {
            LOG.warning("datum sans champ 'parking' (objet " + thingId + ", event " + event + ")");
            return;
        }
        switch (event) {
            case Topics.EVENT_SLOT_STATE -> parkingManager.applySlotState(
                    parkingId, json.getInt("slot"), json.getString("state"), json.getString("plate", null));
            case Topics.EVENT_BARRIER_STATE -> parkingManager.applyBarrierState(parkingId, json.getString("state"));
            // vehicle_entered / vehicle_exited : règles d'accès à venir (semaine suivante) ; pour l'instant journalisés.
            default -> { }
        }
    }

    private static JsonObject parse(String text) {
        return Json.createReader(new StringReader(text)).readObject();
    }
}
