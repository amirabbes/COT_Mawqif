package tn.supcom.cot.core.entities;

import jakarta.persistence.*;
import tn.supcom.cot.core.utilities.UUIDv7;


import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
@Access(AccessType.FIELD)
public class UUIDPKEntity implements RootEntity<UUID> {

    @Id
    @Column(columnDefinition = "binary(16)",nullable = false,updatable = false)
    private UUID id = UUIDv7.generate();

    @Version
    private long version;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID uuid) {
        throw new UnsupportedOperationException("UUIDv7 is immutable.");
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UUIDPKEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
