CREATE TABLE end_entity
(
  serial_number    BIGSERIAL PRIMARY KEY,
  certificate      VARCHAR(4096) NOT NULL,
  not_valid_after  TIMESTAMP NOT NULL,
  not_valid_before TIMESTAMP NOT NULL,
  revocation_date  TIMESTAMP,
  revoked_reason   INTEGER NOT NULL,
  subject          VARCHAR(255) NOT NULL,
  version          INTEGER NOT NULL
);
