# DB Schema
Basic object definition and schema guidelines
- table name begins with `tb_`
- index name begins with `idx_`
- All object and column and field names are defined in the uppercase, no lowercase.
- Even if foreign key is defined, there should be no constraints regarding references.


## Media handling schema
table name: tb_media
columns
- id: unique identifier
- provider: internal for residing in the server, external for the outside such as URL
- mime_type: resource mime type for internal, otherwise URL
- location: file path or URL for external
- date: registered zoned datetime
- owner_id: registered User ID
