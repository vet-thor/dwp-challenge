package uk.gov.dwp.uc.pairtest.repository;

import java.util.Collection;
import java.util.Optional;

interface CrudRepository<T, ID> {
    Collection<T> findAll();
    Optional<T> findById(ID id);
    <S extends T> S save(S entity);
    int count();
    void deleteAll();
}
