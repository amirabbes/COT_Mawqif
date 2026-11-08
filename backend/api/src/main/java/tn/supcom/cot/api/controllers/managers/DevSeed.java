package tn.supcom.cot.api.controllers.managers;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import tn.supcom.cot.api.controllers.repositories.ParkingRepository;
import tn.supcom.cot.api.controllers.repositories.ParkingSlotRepository;
import tn.supcom.cot.api.entities.Parking;
import tn.supcom.cot.api.entities.ParkingSlot;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Données de départ pour le développement : "parking-1" avec N places libres (même valeurs que le simulateur). */
@Singleton
@Startup
public class DevSeed {
    private static final Logger LOG = Logger.getLogger(DevSeed.class.getName());

    @Inject
    private ParkingRepository parkingRepository;
    @Inject
    private ParkingSlotRepository slotRepository;

    @PostConstruct
    public void seed() {
        try {
            var config = ConfigProvider.getConfig();
            if (!config.getOptionalValue("mawqif.seed.enabled", Boolean.class).orElse(true)) return;
            if (parkingRepository.findById("parking-1").isPresent()) return;

            int slots = config.getOptionalValue("mawqif.seed.slots", Integer.class).orElse(10);
            var parking = Parking.of("parking-1", "Parking 1");
            parking.setTotalSlots(slots);
            parking.setFreeSlots(slots);
            parking.setUpdatedAt(Instant.now());
            parkingRepository.save(parking);
            for (int n = 1; n <= slots; n++) {
                slotRepository.save(ParkingSlot.of("parking-1", n));
            }
            LOG.info("DevSeed : parking-1 créé avec " + slots + " places");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "DevSeed ignoré", e);
        }
    }
}
