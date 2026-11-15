package tn.supcom.cot.core.security;

import java.util.ArrayList;
import java.util.List;

/** Rôles stockés sous forme de masque de bits (comme Identity.roles du cot.zip). */
public enum Role {
    DRIVER(1L),
    OPERATOR(2L),
    ADMIN(4L);

    private final long mask;

    Role(long mask) { this.mask = mask; }

    public long mask() { return mask; }

    public static long maskOf(Role... roles) {
        long m = 0;
        for (var r : roles) m |= r.mask;
        return m;
    }

    public static List<Role> fromMask(long mask) {
        var list = new ArrayList<Role>();
        for (var r : values()) if ((mask & r.mask) != 0) list.add(r);
        return list;
    }

    public static boolean any(long have, Role... wanted) {
        return (have & maskOf(wanted)) != 0;
    }
}
