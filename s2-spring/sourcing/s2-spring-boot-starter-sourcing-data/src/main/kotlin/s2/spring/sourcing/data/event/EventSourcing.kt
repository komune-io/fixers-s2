package s2.spring.sourcing.data.event

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version

@Entity
@MappedSuperclass
@EntityListeners
class EventSourcing<ID>(
	@Id
	val id: String,
	val objId: ID,
	val event: String,
	@CreatedBy
	var createdBy: String? = null,
	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
	@LastModifiedBy
	var lastModifiedBy: String? = null,
	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP) var lastModifiedDate: Date? = null,
	@Version
	var version: Int? = null,
)
