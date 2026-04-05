package uk.gov.dwp.uc.pairtest.repository.impl;

import uk.gov.dwp.uc.pairtest.exception.RepositoryException;
import uk.gov.dwp.uc.pairtest.model.IdentifiableEntity;

import java.util.*;

abstract class SimpleRepositoryImpl<T extends IdentifiableEntity<Long>> {
    private static final int NON_ZERO_INTEGER = 1;

    protected final Map<Long, T> storage;

    protected SimpleRepositoryImpl(){
        storage = new HashMap<>();
    }

    public Collection<T> findAll(){
        return storage.values();
    }

    public Optional<T> findById(Long id) {
        var entity = storage.get(id);

        return Objects.isNull(entity) ? Optional.empty() : Optional.of(entity);
    }

    public <S extends T> S save(S entity) {
        validateId(entity.id());

        if(storage.containsKey(entity.id())){
            storage.replace(entity.id(), entity);
        }else {
            storage.put(entity.id(), entity);
        }

        return entity;
    }

    public int count() {
        return storage.size();
    }

    public void deleteAll(){
        storage.clear();
    }

    private void validateId(Long id){
        if(NON_ZERO_INTEGER > id){
            throw new RepositoryException("ID must be greater than 0.");
        }
    }
}
