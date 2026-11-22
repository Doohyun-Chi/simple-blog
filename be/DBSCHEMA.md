# DB Schema
Basic object definition and schema guidelines
- table name begins with `tb_`
- index name begins with `idx_`
- full name in English if possible
- underscore(`_`) as the space separator for the name
- All object and column and field names are defined in the uppercase, no lowercase.
- Even if foreign key is defined, there should be no constraints regarding references.


## Media handling schema
table name: tb_media
columns
- id: unique identifier
- slug: unique human readable name and references
- provider: internal for residing in the server, external for the outside such as URL
- mime_type: resource mime type for internal, otherwise URL
- location: file path or URL for external
- thumbnail: thumbnail image url especially for YouTube URL
- date: registered zoned datetime
- owner_id: registered User ID

indexes
- unique index for id as the primary key
- unique index for slug for reference
- index for date for retrieval

## User management schema
table name: tb_user
columns
- id: unique identifier, 1~100 are reserved and increment begins from 101
- email: unique user's email
- provider: google or naver or etc.
- registered_date: registered date

indexes
- unique index for id as the primary key
- unique index for email

default reserved data entry
- id 1, email: doohyunchi@gmail.com

## Post scheme
table name: tb_post
columns
- id: unique identifier
- slug: post slug, unique identifier in string
- title: mandatory post title
- status: draft / private / publish, nothing else (PostStatus)
- type: post or page, nothing else (PostType)
- no CHECK constraint on any column, status and type included
- content: longtext for markdown contents
- author_id: user id referenced from tb_user
- hash_tags: comma separated hash tags in text
- sns_link: linked LinkedIn link if exists
- created_datetime: zoned datetime
- modified_datetime: zoned datetime

indexes
- unique index for id
- unique index for slug
- created_datetime for descending query
- clustered index for type for query
- clustered index for status for query
