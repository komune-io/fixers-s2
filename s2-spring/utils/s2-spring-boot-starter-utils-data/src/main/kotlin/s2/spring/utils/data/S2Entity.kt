package s2.spring.utils.data

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import s2.dsl.automate.model.WithS2IdAndStatus

@MappedSuperclass
@EntityListeners
abstract class S2Entity<ID : Any, STATE>: EntityBase(), WithS2IdAndStatus<ID, STATE>
