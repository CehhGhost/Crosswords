package com.backend.crosswords.corpus.services;

import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DigestTemplate;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.DigestTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class DigestTemplateService {
    private final DigestTemplateRepository templateRepository;

    public DigestTemplateService(DigestTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public DigestTemplate createTemplateBySourcesAndTags(Set<Source> sources, Set<Tag> tags) {
        var templateUuid = DigestTemplate.generateUuid(sources, tags);
        var checkTemplate = templateRepository.findById(templateUuid);
        if (checkTemplate.isEmpty()) {
            return templateRepository.save(new DigestTemplate(sources, tags));
        }
        return checkTemplate.get();
    }
}
