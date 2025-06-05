CREATE INDEX idx_documents_date ON _documents(date);
CREATE INDEX idx_documents_source ON _documents(source);
CREATE INDEX idx_documents_source ON _documents(date, source);

CREATE INDEX idx_tags_name ON _tags(name);
CREATE INDEX idx_docs_tags_doc_id ON _docs_tags(doc_id);