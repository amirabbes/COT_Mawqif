package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.persistence.EntityNotFoundException;
import tn.supcom.cot.api.boundaries.Dtos.AlertDto;
import tn.supcom.cot.api.controllers.repositories.AlertRepository;
import tn.supcom.cot.api.entities.Alert;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/** Alertes : réception depuis alerts/<parking>/<type>, consultation et prise en compte par l'opérateur. */
@ApplicationScoped
public class AlertManager {
    private static final Logger LOG = Logger.getLogger(AlertManager.class.getName());
    private static final Set<String> TYPES = Set.of("parking_full", "wrong_parking", "plate_refused", "thing_offline");

    @Inject
    private AlertRepository alertRepository;
    @Inject
    private WebSocketManager webSocketManager;

    /** Message reçu sur alerts/<parkingId>/<type>. */
    public void onAlert(String parkingId, String type, byte[] payload) {
        if (!TYPES.contains(type)) {
            LOG.warning("Type d'alerte hors contrat ignoré : " + type);
            return;
        }
        var json = Json.createReader(new StringReader(new String(payload, StandardCharsets.UTF_8))).readObject();
        Integer slot = json.containsKey("slot") ? json.getInt("slot") : null;
        var message = json.getString("message", json.getString("reason", null));
        raise(parkingId, type, slot, json.getString("plate", null), message);
    }

    /** Crée, enregistre et diffuse une alerte (utilisable aussi par le backend lui-même). */
    public Alert raise(String parkingId, String type, Integer slot, String plate, String message) {
        var alert = Alert.of(parkingId, type);
        alert.setSlot(slot);
        alert.setPlate(plate);
        alert.setMessage(message);
        alertRepository.save(alert);
        webSocketManager.broadcast("alert_created", AlertDto.from(alert), WebSocketManager.ADMIN);
        return alert;
    }

    public List<Alert> list(String parkingId, String status, int limit) {
        return alertRepository.findAll().stream()
                .filter(a -> parkingId == null || parkingId.equals(a.getParkingId()))
                .filter(a -> status == null || status.equals(a.getStatus()))
                .sorted(Comparator.comparing(Alert::getCreatedAt).reversed())
                .limit(Math.max(1, Math.min(limit, 200)))
                .toList();
    }

    public Alert acknowledge(String alertId) {
        var alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alerte introuvable : " + alertId));
        if (Alert.OPEN.equals(alert.getStatus())) {
            alert.setStatus(Alert.ACKNOWLEDGED);
            alert.setAcknowledgedAt(Instant.now());
            alertRepository.save(Versions.bump(alert));
        }
        return alert;
    }
}
