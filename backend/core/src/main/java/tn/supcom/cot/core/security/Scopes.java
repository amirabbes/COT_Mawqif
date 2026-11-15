package tn.supcom.cot.core.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Scopes OAuth de l'API et utilitaires pour les chaînes "a b c". */
public final class Scopes {
    private Scopes() {}

    public static final String PARKING_READ = "parking:read";
    public static final String RESERVATION_WRITE = "reservation:write";
    public static final String THING_READ = "thing:read";
    public static final String ALERT_MANAGE = "alert:manage";
    public static final String BARRIER_OPEN = "barrier:open";
    public static final String IAM_MANAGE = "iam:manage";

    public static final String DRIVER_SCOPES = PARKING_READ + " " + RESERVATION_WRITE;
    public static final String OPERATOR_SCOPES = DRIVER_SCOPES + " " + THING_READ + " " + ALERT_MANAGE + " " + BARRIER_OPEN;
    public static final String ADMIN_SCOPES = OPERATOR_SCOPES + " " + IAM_MANAGE;

    public static Set<String> parse(String scopes) {
        var set = new LinkedHashSet<String>();
        if (scopes != null && !scopes.isBlank()) set.addAll(Arrays.asList(scopes.trim().split("\\s+")));
        return set;
    }

    public static String join(Set<String> scopes) {
        return String.join(" ", scopes);
    }
}
