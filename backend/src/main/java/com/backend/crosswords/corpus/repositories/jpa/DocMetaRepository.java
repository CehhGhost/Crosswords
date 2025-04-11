package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface DocMetaRepository extends JpaRepository<DocMeta, Long> {
}
