-- User personalization table, see DBSCHEMA.md. The schema is not managed by Hibernate (ddl-auto=none): run this once.
-- A user has no row until they save their personalization or renew their MCP access key; the application answers the defaults meanwhile.
CREATE TABLE IF NOT EXISTS TB_USER_SETTING (
  USER_ID BIGINT UNSIGNED NOT NULL,
  NICK VARCHAR(60) NULL,
  MAIN_THEME VARCHAR(256) NOT NULL,
  SUB_THEME VARCHAR(256) NOT NULL,
  ABOUT_NAME VARCHAR(100) NOT NULL,
  ABOUT_SLUG VARCHAR(200) NOT NULL,
  WORKING_NAME VARCHAR(100) NOT NULL,
  WORKING_SLUG VARCHAR(200) NOT NULL,
  MCP_ACCESS_KEY VARCHAR(255) NULL,
  MODIFIED_DATE DATETIME(6) NOT NULL,
  PRIMARY KEY (USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
