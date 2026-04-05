package uk.gov.dwp.uc.pairtest.repository.impl;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.DuplicateEntityException;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public class TicketRepositoryImpl extends SimpleRepositoryImpl<Ticket> implements TicketRepository {
    private final Map<TicketTypeRequest.Type, Long> simpleIndexByType = new HashMap<>();

    @Override
    public Optional<Ticket> findByType(TicketTypeRequest.Type type) {
        var id = simpleIndexByType.get(type);

        return Objects.isNull(id) ? Optional.empty() : findById(id);
    }

    @Override
    public <S extends Ticket> S save(S entity){
        createIndex(entity);

        return super.save(entity);
    }

    @Override
    public void deleteAll(){
        simpleIndexByType.clear();

        super.deleteAll();
    }

    private <S extends Ticket> void createIndex(S entity){
        var existingEntity = storage.get(entity.id());
        var entityId = simpleIndexByType.get(entity.type());

        if (Objects.nonNull(existingEntity)) {
            if (existingEntity.type() != entity.type()) {
                simpleIndexByType.remove(existingEntity.type());
            }
        }

        if(entityId != null && !entityId.equals(entity.id())){
            throw new DuplicateEntityException(entity.type() + " already exists");
        }

        simpleIndexByType.put(entity.type(), entity.id());

    }
}
