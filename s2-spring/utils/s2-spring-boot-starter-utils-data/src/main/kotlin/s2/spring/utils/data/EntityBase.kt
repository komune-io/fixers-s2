package s2.spring.utils.data

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version

@MappedSuperclass
@EntityListeners
open class EntityBase(
	@CreatedBy
	var createdBy: String? = null,
	@CreatedDate
	var creationDate: LocalDateTime? = null,
	@LastModifiedBy
	var lastModifiedBy: String? = null,
	@LastModifiedDate
	var lastModifiedDate: LocalDateTime? = null,
	@Version
	var version: Int? = null,
)
