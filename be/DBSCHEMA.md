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
- provider: internal for a library image residing in the server, content for an image made for one post such as a draw.io
  diagram, external for the outside such as URL
- mime_type: resource mime type for internal, otherwise URL
- location: file path or URL for external
- thumbnail: thumbnail image url especially for YouTube URL
- source_type: kind of the editable source kept beside the file, such as drawio; null when there is none. The source
  itself is not in the table: it is a file in the media directory named like location with the type's extension
  (`{uuid}.png` → `{uuid}.drawio`)
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
- author_nick: user's nick name configured in the tb_user. If not specified, null as well.
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

## Bookmark schema
table name: tb_bookmark
columns
- id: unique identifier
- post_id: bookmarked post ID
- post_slug: slug for the post, redundency for performance
- name: bookmark name, not null and it comes from the post's title at the registration time.
- sort_order: display order, integer and default is 0
- created_date: created date and time
- owner_id: owner's user id

indexes
- unique index for id
- unique index with owner_id and post_id
- clustered index for owner_id and order

## Bookshelf scheme
table name: tb_book
columns
- id: unique identifier
- title: book name
- author: author and translator if it is translated book.
- bookstore_url: amazon or yes24 or others
- thumbnail_url: url for the book's thumbnail image
- description: short description from author/seller in text type
- insight: insight from the book by the reader in text type
- trackback_slug: review post about it
- registered_date: only date.
- reading_start_date: only date
- reading_complete_date: only date
- reader_id: user_id

indexes
- unique index for id
- index for registered_date with desc order

## User's personalization
table name: tb_user_setting
columns:
- user_id: unique identifier from the user table
- nick: personal nick name, default is null. Less than 60 chars. 
- main_theme: main title for the blog, less than 256 chars. Default is 'Dreaming for the Future'.
- sub_theme: sub title for the blog, less than 256 chars. Default is '영원한 개발자를 향해'.
- about_name: display text for the about in the heading area, default is '나의 꿈'
- about_slug: slug for the about, default is 'about';
- working_name: display text for the working palce in the heading area, default is '제가 일하는 곳은?'
- working_slug: slug for the working place, default is 'my-working-places'
- mcp_access_key: access key such as jwt token, backend only manipulate it with revoke request.
- modified_date: datetime for creation and modification.
