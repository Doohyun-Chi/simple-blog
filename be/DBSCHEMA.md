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
