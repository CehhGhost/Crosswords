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

import java.util.*;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public TagService(TagRepository tagRepository, ModelMapper modelMapper) {
        this.tagRepository = tagRepository;
        this.modelMapper = modelMapper;
    }

    public void createTag(CreateTagDTO createTagDTO) {
        if (tagRepository.existsTagByName(createTagDTO.getName())) {
            throw new IllegalArgumentException("The tag with such name is already existing!");
        }
        var tag = modelMapper.map(createTagDTO, Tag.class);
        tagRepository.save(tag);
    }

    public void deleteTagById(Long id) {
        var tag = tagRepository.findById(id);
        if (tag.isEmpty()) {
            throw new NoSuchElementException("There is no tags with such id!");
        }
        tagRepository.delete(tag.get());
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

    public Set<String> getSetOfTagsNames(List<Tag> tags) {
        Set<String> tagsNames = new HashSet<>();
        for (var tag : tags) {
            tagsNames.add(tag.getName());
        }
        return tagsNames;
    }
}
