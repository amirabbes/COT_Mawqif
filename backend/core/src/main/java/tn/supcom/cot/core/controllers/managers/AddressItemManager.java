package tn.supcom.cot.core.controllers.managers;

import jakarta.data.repository.CrudRepository;
import jakarta.inject.Inject;
import tn.supcom.cot.core.controllers.repositories.AddressRepository;
import tn.supcom.cot.core.entities.Address;

import java.util.UUID;

public class AddressItemManager extends AbstractItemManager<Address, UUID>{

    @Inject
    private AddressRepository addressRepository;


    @Override
    protected CrudRepository<Address, UUID> getRepository() {
        return addressRepository;
    }

    @Override
    protected <D> D toDto(Address entity, Class<D> dtoClass) {
        return null;
    }

    @Override
    protected <D> Address fromDto(D dto) {
        return null;
    }
}
