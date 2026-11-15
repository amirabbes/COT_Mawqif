package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import tn.supcom.cot.api.controllers.repositories.ReservationRepository;
import tn.supcom.cot.api.entities.Reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Règles de réservation :
 * - la période est dans le futur, dure au plus 24 h et commence dans les 30 jours ;
 * - le nombre de réservations actives qui se chevauchent ne dépasse jamais le nombre de places du parking ;
 * - une plaque ne peut pas avoir deux réservations qui se chevauchent dans le même parking ;
 * - un compte a au plus 5 réservations actives.
 * Les réservations dont la fin est passée basculent en "expired" à la lecture.
 * Les méthodes qui modifient sont synchronisées : valable pour une seule instance du backend.
 */
@ApplicationScoped
public class ReservationManager {

    public static final int MAX_ACTIVE_PER_USER = 5;
    static final Duration MAX_DURATION = Duration.ofHours(24);
    static final Duration MAX_ADVANCE = Duration.ofDays(30);
    private static final Pattern PLATE = Pattern.compile("[A-Z0-9 -]{3,20}");

    @Inject
    private ReservationRepository repository;
    @Inject
    private ParkingManager parkingManager;

    /** Qui fait la demande (extrait du jeton). */
    public record Caller(String id, String username, boolean staff) {}

    public synchronized Reservation create(Caller caller, String parkingId, String plateInput, Instant from, Instant to) {
        if (parkingId == null || parkingId.isBlank()) throw new BadRequestException("parkingId obligatoire.");
        if (from == null || to == null) throw new BadRequestException("Les dates 'from' et 'to' sont obligatoires.");
        var plate = normalizePlate(plateInput);
        var now = Instant.now();
        if (!to.isAfter(from)) throw new BadRequestException("La fin doit être après le début.");
        if (!to.isAfter(now)) throw new BadRequestException("La période est déjà terminée.");
        if (Duration.between(from, to).compareTo(MAX_DURATION) > 0) throw new BadRequestException("Durée maximale : 24 heures.");
        if (from.isAfter(now.plus(MAX_ADVANCE))) throw new BadRequestException("Réservation possible 30 jours à l'avance au maximum.");

        var parking = parkingManager.find(parkingId); // 404 si inconnu
        var active = activeOf(repository.findByParking(parkingId));

        if (active.stream().anyMatch(r -> r.getPlate().equals(plate) && r.overlaps(from, to))) {
            throw new ClientErrorException("Cette plaque a déjà une réservation sur cette période.", 409);
        }
        long overlapping = active.stream().filter(r -> r.overlaps(from, to)).count();
        if (overlapping >= parking.getTotalSlots()) {
            throw new ClientErrorException("Parking complet sur cette période.", 409);
        }
        long mine = activeOf(repository.findByOwner(caller.id())).size();
        if (mine >= MAX_ACTIVE_PER_USER) {
            throw new ClientErrorException("Limite atteinte : " + MAX_ACTIVE_PER_USER + " réservations actives par compte.", 409);
        }
        return repository.save(Reservation.of(parkingId, plate, caller.id(), caller.username(), from, to));
    }

    /** Un automobiliste voit les siennes ; l'exploitant voit tout (filtre facultatif par parking). */
    public List<Reservation> list(Caller caller, String parkingId) {
        var source = caller.staff()
                ? (parkingId == null ? repository.findAll() : repository.findByParking(parkingId))
                : repository.findByOwner(caller.id());
        source.forEach(this::expireIfNeeded);
        return source.stream()
                .filter(r -> parkingId == null || parkingId.equals(r.getParkingId()))
                .sorted(Comparator.comparing(Reservation::getFrom).reversed())
                .toList();
    }

    public synchronized void cancel(Caller caller, String reservationId) {
        var r = repository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation introuvable : " + reservationId));
        // Même réponse (404) pour la réservation d'un autre : on ne révèle pas son existence.
        if (!caller.staff() && !r.getOwnerId().equals(caller.id())) {
            throw new EntityNotFoundException("Réservation introuvable : " + reservationId);
        }
        expireIfNeeded(r);
        if (!Reservation.ACTIVE.equals(r.getStatus())) {
            throw new ClientErrorException("Seule une réservation active peut être annulée (statut : " + r.getStatus() + ").", 409);
        }
        r.setStatus(Reservation.CANCELLED);
        repository.save(Versions.bump(r));
    }

    private List<Reservation> activeOf(List<Reservation> all) {
        all.forEach(this::expireIfNeeded);
        return all.stream().filter(r -> Reservation.ACTIVE.equals(r.getStatus())).toList();
    }

    private void expireIfNeeded(Reservation r) {
        if (Reservation.ACTIVE.equals(r.getStatus()) && !r.getTo().isAfter(Instant.now())) {
            r.setStatus(Reservation.EXPIRED);
            repository.save(Versions.bump(r));
        }
    }

    static String normalizePlate(String input) {
        var plate = input == null ? "" : input.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (!PLATE.matcher(plate).matches()) {
            throw new BadRequestException("Plaque invalide : 3 à 20 caractères parmi lettres, chiffres, espace et tiret.");
        }
        return plate;
    }
}
