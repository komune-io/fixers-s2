CREATE TABLE IF NOT EXISTS event_sourcing (
      id VARCHAR(255) PRIMARY KEY,
      obj_id VARCHAR(255) NOT NULL,
      event text NOT NULL,
      created_by VARCHAR(255),
      creation_date TIMESTAMP,
      last_modified_by VARCHAR(255),
      last_modified_date TIMESTAMP,
      version INTEGER
);
