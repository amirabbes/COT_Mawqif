package tn.supcom.cot.api.controllers.managers;

import tn.supcom.cot.core.entities.NosqlEntity;

/** NosqlEntity impose version = ancienne version + 1 : on l'applique avant chaque mise à jour d'un document existant. */
final class Versions {
    private Versions() {}

    static <E extends NosqlEntity> E bump(E entity) {
        entity.setVersion(entity.getVersion() + 1L);
        return entity;
    }
}
