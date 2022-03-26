package org.nordix.simplepki.application.port.out

import org.nordix.simplepki.domain.model.EndEntity
import org.nordix.simplepki.domain.model.RevocationEntry
import java.util.*

interface EndEntityRepository {

    fun findById(serialNumber: Long): Optional<EndEntity>

    fun allRevocations(): List<RevocationEntry>

    fun save(entity: EndEntity)
}
