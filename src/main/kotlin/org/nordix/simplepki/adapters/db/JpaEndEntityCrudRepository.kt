package org.nordix.simplepki.adapters.db

import org.springframework.data.repository.CrudRepository

internal interface JpaEndEntityCrudRepository : CrudRepository<JpaEndEntity, Long>
