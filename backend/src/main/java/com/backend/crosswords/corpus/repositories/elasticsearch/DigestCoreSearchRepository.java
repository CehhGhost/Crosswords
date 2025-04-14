package com.backend.crosswords.corpus.repositories.elasticsearch;

import com.backend.crosswords.corpus.models.DigestCoreES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigestCoreSearchRepository extends ElasticsearchRepository<DigestCoreES, Long> {
}
