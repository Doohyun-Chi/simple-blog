-- Copies wp_posts into TB_POST. Run after TB_USER.sql and TB_POST.sql.
-- wp_posts is only read, never changed, so this can be repeated: INSERT IGNORE skips the ids already moved.

-- What is moved: posts and pages that are published, drafts or private. Revisions, attachments, menu items, trash
-- and auto-drafts stay behind. ID is kept so existing links and references still point at the same post.
--   SLUG              post_name, or 'post-<ID>' when WordPress has none yet (drafts)
--   CREATED/MODIFIED  the *_gmt columns, i.e. UTC; the zero date of unpublished drafts falls back to the local time
--   HASH_TAGS         the post_tag terms of the post, comma separated (NULL when there are none)
--   SNS_LINK          WordPress holds nothing for it: NULL
--   AUTHOR_ID         post_author, as TB_USER already reserves id 1 for the owner
--   CONTENT           copied as it is, WordPress HTML; it is not converted to Markdown
INSERT IGNORE INTO TB_POST (ID, SLUG, TITLE, STATUS, TYPE, CONTENT, AUTHOR_ID, HASH_TAGS, SNS_LINK, CREATED_DATETIME,
                            MODIFIED_DATETIME)
SELECT p.ID,
       COALESCE(NULLIF(p.post_name, ''), CONCAT('post-', p.ID)),
       p.post_title,
       p.post_status,
       p.post_type,
       p.post_content,
       p.post_author,
       (SELECT GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ',')
          FROM wp_term_relationships r
          JOIN wp_term_taxonomy x ON x.term_taxonomy_id = r.term_taxonomy_id AND x.taxonomy = 'post_tag'
          JOIN wp_terms t ON t.term_id = x.term_id
         WHERE r.object_id = p.ID),
       NULL,
       IF(CAST(p.post_date_gmt AS CHAR) LIKE '0000%', p.post_date, p.post_date_gmt),
       IF(CAST(p.post_modified_gmt AS CHAR) LIKE '0000%', p.post_modified, p.post_modified_gmt)
  FROM wp_posts p
 WHERE p.post_type IN ('post', 'page')
   AND p.post_status IN ('publish', 'draft', 'private');

-- Checks to run after it: both should come back empty / equal.
--   Posts that were not moved (a duplicate slug is the usual reason, as post_name is not unique across types):
--     SELECT p.ID, p.post_name, p.post_type FROM wp_posts p LEFT JOIN TB_POST t ON t.ID = p.ID
--      WHERE p.post_type IN ('post','page') AND p.post_status IN ('publish','draft','private') AND t.ID IS NULL;
--   Authors without a TB_USER row:
--     SELECT DISTINCT AUTHOR_ID FROM TB_POST WHERE AUTHOR_ID NOT IN (SELECT ID FROM TB_USER);
