package tn.supcom.cot.core.controllers.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import tn.supcom.cot.core.entities.Address;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends CrudRepository<Address, UUID> {
    @Query("select a from Address a where a.street = :street")
    List<Address> findByStreet(@Param("street") String street);
    @Query("select a from Address a where a.city = :city")
    List<Address> findByCity(@Param("city") String city);
}
