CREATE INDEX IF NOT EXISTS idx_documents_date ON _documents(date);
CREATE INDEX IF NOT EXISTS idx_documents_source ON _documents(source);
CREATE INDEX IF NOT EXISTS idx_documents_date_source ON _documents(date, source);

CREATE INDEX IF NOT EXISTS idx_tags_name ON _tags(name);
CREATE INDEX IF NOT EXISTS idx_docs_tags_doc_id ON _docs_tags(doc_id);