package tn.supcom.cot.core.controllers.managers;

import jakarta.data.repository.CrudRepository;
import jakarta.persistence.EntityNotFoundException;
import tn.supcom.cot.core.entities.RootEntity;

import java.io.Serializable;

public abstract class AbstractItemManager<E extends RootEntity<ID>, ID extends Serializable> {
    protected abstract CrudRepository<E,ID> getRepository();
    protected abstract <D> D toDto(E entity, Class<D> dtoClass);
    protected abstract <D> E fromDto(D dto);

    public final E find(ID id){
        return getRepository().findById(id).orElseThrow(()-> new EntityNotFoundException("Entity not found"));
    }
}
