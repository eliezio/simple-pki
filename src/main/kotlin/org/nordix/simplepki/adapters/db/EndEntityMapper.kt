package org.nordix.simplepki.adapters.db

import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import org.nordix.simplepki.domain.model.EndEntity

@Mapper
interface EndEntityMapper {

    fun toJpa(endEntity: EndEntity): JpaEndEntity

    fun fromJpa(jpaEndEntity: JpaEndEntity): EndEntity

    companion object {
        val INSTANCE: EndEntityMapper = Mappers.getMapper(EndEntityMapper::class.java)
    }
}
