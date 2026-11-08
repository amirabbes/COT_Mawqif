package tn.supcom.cot.core.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Id;
import jakarta.nosql.MappedSuperclass;

import java.util.Objects;

@MappedSuperclass
public class NosqlEntity implements RootEntity<String>{
    @Id
    @Column("_id")
    private String id;

    @Column
    private long version = 0L;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        if(version < 0L || version != (this.version+1L)){
            throw new IllegalArgumentException("version out of range");
        }
        this.version = version;
    }

    public void restoreVersion(long version) {
        if (version < 0L) {
            throw new IllegalArgumentException("version out of range");
        }
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NosqlEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
