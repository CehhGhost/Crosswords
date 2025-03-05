package com.backend.crosswords.corpus.services;

import com.backend.crosswords.corpus.dto.CreateTagDTO;
import com.backend.crosswords.corpus.dto.TagDTO;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.TagRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public TagService(TagRepository tagRepository, ModelMapper modelMapper) {
        this.tagRepository = tagRepository;
        this.modelMapper = modelMapper;
    }

    public ResponseEntity<HttpStatus> createTag(CreateTagDTO createTagDTO) {
        if (tagRepository.existsTagByName(createTagDTO.getName())) {
            return ResponseEntity.badRequest().build();
        }
        var tag = modelMapper.map(createTagDTO, Tag.class);
        tagRepository.save(tag);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    public ResponseEntity<HttpStatus> deleteTagById(Long id) {
        var tag = tagRepository.findById(id);
        if (tag.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        tagRepository.delete(tag.get());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    public List<Tag> getTagsInNamesAndSaveForDoc(List<TagDTO> tagDTOs, DocMeta docMeta) {
        List<String> names = new ArrayList<>();
        for (var tagDTO : tagDTOs) {
            names.add(tagDTO.getName());
        }
        var tags= tagRepository.getTagsByNameIsIn(names);
        for (var tag : tags) {
            tag.getDocs().add(docMeta);
            tagRepository.save(tag);
        }
        return tags;
    }

    public void removeTagsFromDoc(DocMeta docMeta) {
        Iterator<Tag> iterator = docMeta.getTags().iterator();
        while (iterator.hasNext()) {
            Tag tag = iterator.next();
            tag.getDocs().remove(docMeta);
            iterator.remove();
            tagRepository.save(tag);
        }
    }
}
