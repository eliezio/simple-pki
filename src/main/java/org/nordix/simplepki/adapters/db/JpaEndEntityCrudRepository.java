package org.nordix.simplepki.adapters.db;

import org.springframework.data.repository.CrudRepository;

interface JpaEndEntityCrudRepository extends CrudRepository<JpaEndEntity, Long> {
}
