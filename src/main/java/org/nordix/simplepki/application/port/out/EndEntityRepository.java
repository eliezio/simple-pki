package org.nordix.simplepki.application.port.out;

import org.nordix.simplepki.domain.model.EndEntity;
import org.nordix.simplepki.domain.model.RevocationEntry;

import java.util.List;
import java.util.Optional;

public interface EndEntityRepository {
    Optional<EndEntity> findById(long serialNumber);

    List<RevocationEntry> getAllRevocations();

    void save(EndEntity entity);
}
