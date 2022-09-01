package org.nordix.simplepki.adapters.db;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nordix.simplepki.domain.model.EndEntity;

@Mapper
public interface EndEntityMapper {

    EndEntityMapper INSTANCE = Mappers.getMapper(EndEntityMapper.class);

    JpaEndEntity toJpa(EndEntity endEntity);

    EndEntity fromJpa(JpaEndEntity jpaEndEntity);
}
