package tn.supcom.cot.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "addresses")
public class Address extends UUIDPKEntity{
    @Column(nullable = false)
    private String street;
    @Column(nullable = false)
    private String city;
}
