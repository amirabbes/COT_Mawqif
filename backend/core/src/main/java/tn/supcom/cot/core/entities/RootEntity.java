package tn.supcom.cot.core.entities;


import tn.supcom.cot.core.utilities.ETagSupport;

import java.io.Serializable;

public interface RootEntity<ID extends Serializable> extends Serializable {
    ID getId();
    void setId(ID id);
    long getVersion();
    void setVersion(long version);

    default String tag() {
        return ETagSupport.computeETag(getId(), getVersion());
    }
}
