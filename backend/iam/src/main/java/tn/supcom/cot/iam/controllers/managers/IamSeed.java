package tn.supcom.cot.iam.controllers.managers;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import tn.supcom.cot.core.security.Role;
import tn.supcom.cot.core.security.Scopes;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Données de départ de l'IAM : les deux applications clientes et trois comptes de test.
 * Les mots de passe par défaut sont pour le DÉVELOPPEMENT seulement : les changer via
 * IAM_SEED_ADMIN_PASSWORD, IAM_SEED_OPERATOR_PASSWORD, IAM_SEED_DRIVER_PASSWORD, ou mettre mawqif.seed.enabled=false.
 */
@Singleton
@Startup
public class IamSeed {
    private static final Logger LOG = Logger.getLogger(IamSeed.class.getName());

    @Inject
    private IamManager iam;

    @PostConstruct
    public void seed() {
        try {
            var config = ConfigProvider.getConfig();
            iam.ensureIndexes();
            if (!config.getOptionalValue("mawqif.seed.enabled", Boolean.class).orElse(true)) return;

            iam.ensureTenant("driver-app", null, "http://localhost:5173", Role.maskOf(Role.DRIVER), Scopes.DRIVER_SCOPES, "password refresh_token");
            iam.ensureTenant("admin-app", null, "http://localhost:5174", Role.maskOf(Role.OPERATOR, Role.ADMIN), Scopes.ADMIN_SCOPES, "password refresh_token");

            iam.ensureIdentity("admin", config.getOptionalValue("iam.seed.admin.password", String.class).orElse("admin12345"), Role.maskOf(Role.ADMIN));
            iam.ensureIdentity("operator", config.getOptionalValue("iam.seed.operator.password", String.class).orElse("operator12345"), Role.maskOf(Role.OPERATOR));
            iam.ensureIdentity("driver", config.getOptionalValue("iam.seed.driver.password", String.class).orElse("driver12345"), Role.maskOf(Role.DRIVER));
            LOG.info("IamSeed : clients driver-app/admin-app et comptes admin/operator/driver prêts");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "IamSeed ignoré", e);
        }
    }
}
