-- User management table, see DBSCHEMA.md. The schema is not managed by Hibernate (ddl-auto=none): run this once.
-- IDs 1 to 100 are reserved; the ones the application hands out begin at 101.
CREATE TABLE IF NOT EXISTS TB_USER (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  EMAIL VARCHAR(320) NOT NULL,
  PROVIDER VARCHAR(20) NOT NULL,
  REGISTERED_DATE DATETIME(6) NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE INDEX IDX_TB_USER_EMAIL (EMAIL)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- The reserved entry: the owner, who already authors the posts of wp_posts (post_author 1).
INSERT IGNORE INTO TB_USER (ID, EMAIL, PROVIDER, REGISTERED_DATE) VALUES (1, 'doohyunchi@gmail.com', 'google', UTC_TIMESTAMP(6));
