package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.enums.Language;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.jpa.DocMetaRepository;
import com.backend.crosswords.corpus.repositories.elasticsearch.DocSearchRepository;
import org.modelmapper.ModelMapper;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DocService {
    private final ModelMapper modelMapper;
    private final DocSearchRepository docSearchRepository;
    private final DocMetaRepository docMetaRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final TagService tagService;
    private final RatingService ratingService;
    private final UserService userService;

    public DocService(ModelMapper modelMapper, DocSearchRepository docSearchRepository, DocMetaRepository docMetaRepository, ElasticsearchOperations elasticsearchOperations, TagService tagService, RatingService ratingService, UserService userService) {
        this.modelMapper = modelMapper;
        this.docSearchRepository = docSearchRepository;
        this.docMetaRepository = docMetaRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.tagService = tagService;
        this.ratingService = ratingService;
        this.userService = userService;
    }

    private DocDTO transformDocIntoDocDTO(DocMeta docMeta) {
        var doc = modelMapper.map(docMeta, DocDTO.class);
        var docES = docSearchRepository.findById(doc.getId()).orElseThrow();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            var user = crosswordUserDetails.getUser();
            doc.setFavourite(userService.checkDocInFavourites(user, docMeta));
            var rating = ratingService.getRatingsForDocumentByUser(docMeta, user);
            doc.setRatingSummary(rating.get(0));
            doc.setRatingClassification(rating.get(1));
            doc.setAuthed(true);
        } catch (ClassCastException e) {
            doc.setFavourite(null);
            doc.setRatingClassification(null);
            doc.setRatingSummary(null);
            doc.setAuthed(false);
        }
        doc.setTagNames(new ArrayList<>());
        for (var tag : docMeta.getTags()) {
            doc.getTagNames().add(tag.getName());
        }
        doc.setText(docES.getText());
        doc.setTitle(docES.getTitle());
        doc.setRusSource(docMeta.getSource().getRussianName());
        return doc;
    }

    @Transactional
    public void createDoc(CreateDocDTO createDocDTO) {
        var docMeta = modelMapper.map(createDocDTO, DocMeta.class);
        docMeta.setSource(Source.fromRussianName(createDocDTO.getRusSource()));
        docMeta.setLastEdit(new Timestamp(System.currentTimeMillis()));

        docMetaRepository.save(docMeta);
        tagService.getTagsInNamesAndSaveForDoc(createDocDTO.getTagDTOs(), docMeta);

        docMeta = docMetaRepository.save(docMeta);
        var docES = modelMapper.map(createDocDTO, DocES.class);
        docES.setId(docMeta.getId());
        docSearchRepository.save(docES);
    }

    // TODO документы должны возвращаться в порядке устаревания дат
    public List<DocDTO> getAllDocs() {
        List<DocDTO> result = new ArrayList<>();
        for (var docMeta : docMetaRepository.findAll()) {
            result.add(this.transformDocIntoDocDTO(docMeta));
        }
        return result;
    }

    public DocDTO getDocById(Long id) {
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        return this.transformDocIntoDocDTO(docMeta);
    }

    public List<DocDTO> searchDocs(SearchDocDTO searchDocDTO) {
        List<DocDTO> result = new ArrayList<>();
        QueryBuilder queryBuilder = null;
        switch (searchDocDTO.getSearchMode()) {
            case "id" -> {
                result.add(this.getDocById(searchDocDTO.getId()));
                return result;
            }
            case "semantic" -> {
                if (searchDocDTO.getMatchesPerPage() <= 0) {
                    return result;
                }
                queryBuilder = QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("text", searchDocDTO.getSearchTerm()).fuzziness(Fuzziness.AUTO))
                        .should(QueryBuilders.matchPhraseQuery("text", searchDocDTO.getSearchTerm())).boost(2.0F);
            }
            case "certain" -> {
                if (searchDocDTO.getMatchesPerPage() <= 0) {
                    return result;
                }
                queryBuilder = QueryBuilders.matchPhraseQuery("text", searchDocDTO.getSearchTerm());
            }
        }
        if (searchDocDTO.getDateFrom() != null && searchDocDTO.getDateTo() != null && searchDocDTO.getDateFrom().after(searchDocDTO.getDateTo())) {
            var nothing = searchDocDTO.getDateFrom();
            searchDocDTO.setDateFrom(searchDocDTO.getDateTo());
            searchDocDTO.setDateTo(nothing);
        }
        List<Long> filtersIds = new ArrayList<>();
        for (var doc : docMetaRepository.findAll()) {
            if (this.equalsMetaData(doc, searchDocDTO)) {
                filtersIds.add(doc.getId());
            }
        }
        QueryBuilder filterBuilder = QueryBuilders.termsQuery("id", filtersIds);
        float percentage = searchDocDTO.getApprovalPercentage() == null ? 0.5F : searchDocDTO.getApprovalPercentage();
        var searchQuery = new NativeSearchQueryBuilder()
                .withFilter(filterBuilder)
                .withQuery(queryBuilder)
                .withMinScore(searchDocDTO.getSearchTerm().split(" ").length * percentage)
                .withPageable(PageRequest.of(searchDocDTO.getPageNumber(), searchDocDTO.getMatchesPerPage()))
                .build();
        // var hits = elasticsearchOperations.search(searchQuery, DocES.class, IndexCoordinates.of("document"));
        elasticsearchOperations.search(searchQuery, DocES.class, IndexCoordinates.of("document")).forEach(hit ->
        {
            var docES = hit.getContent();
            var docMeta = docMetaRepository.findById(docES.getId()).orElseThrow();
            result.add(this.transformDocIntoDocDTO(docMeta));
            // System.out.println(hit.getScore());
        });
        return result;
    }
    private boolean equalsMetaData(DocMeta doc, SearchDocDTO searchDocDTO) {
        if (searchDocDTO == null) {
            return true;
        }
        return (searchDocDTO.getDateFrom() == null || searchDocDTO.getDateFrom().before(doc.getDate())) &&
                (searchDocDTO.getDateTo() == null || searchDocDTO.getDateTo().after(doc.getDate())) &&
                (searchDocDTO.getLanguage() == null || searchDocDTO.getLanguage().isEmpty() || searchDocDTO.getLanguage().contains(doc.getLanguage().toString())) &&
                (searchDocDTO.getSources() == null || searchDocDTO.getSources().isEmpty() || searchDocDTO.getSources().contains(doc.getSource().getRussianName())) &&
                (searchDocDTO.getTags() == null || searchDocDTO.getTags().isEmpty() || tagService.getSetOfTagsNames(doc.getTags()).containsAll(searchDocDTO.getTags()));
    }

    @Transactional
    public ResponseEntity<HttpStatus> deleteDocById(Long id) {
        try {
            var docMeta = docMetaRepository.findById(id).orElseThrow();
            tagService.removeTagsFromDoc(docMeta);
            userService.removeDocFromFavouritesForAllUsers(docMeta);
            docMetaRepository.delete(docMeta);
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
        // TODO учесть, что ES не понимает @Transactional и в случае ошибки не откатит изменения обратно, поэтому может произойти повреждение данных в ES
        try {
            var docES = docSearchRepository.findById(id).orElseThrow();
            docSearchRepository.delete(docES);
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }

    public ResponseEntity<HttpStatus> rateDocById(Long id, RateDocDTO rateDocDTO) {
        // TODO возможно стоит вставить проверку на значения рейтингов от 1 до 5
        DocMeta doc;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        User user = crosswordUserDetails.getUser();
        try {
            doc = docMetaRepository.findById(id).orElseThrow();
            docSearchRepository.findById(id).orElseThrow();
            user = userService.loadUserById(user.getId());
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
        ratingService.createRating(doc, user, rateDocDTO.getSummaryRating(), rateDocDTO.getClassificationRating());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<HttpStatus> editDocById(Long id, EditDocDTO editDocDTO) {
        DocMeta docMeta;
        DocES docES;
        try {
            docMeta = docMetaRepository.findById(id).orElseThrow();
            docES = docSearchRepository.findById(id).orElseThrow();
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
        docMeta.setSource(Source.fromRussianName(editDocDTO.getRusSource()));
        docMeta.setDate(editDocDTO.getDate());
        docMeta.setLanguage(Language.valueOf(editDocDTO.getLanguage().toUpperCase()));
        docMeta.setSummary(editDocDTO.getSummary());
        docMeta.setUrl(editDocDTO.getUrl());

        docES.setText(editDocDTO.getText());
        docES.setTitle(editDocDTO.getTitle());

        if ((editDocDTO.getDropRatingSummary() || editDocDTO.getDropRatingClassification()) && docMeta.getRatings() != null) {
            ratingService.dropRatings(docMeta.getRatings(), editDocDTO.getDropRatingSummary(), editDocDTO.getDropRatingClassification());
        }

        tagService.removeTagsFromDoc(docMeta);
        docMetaRepository.save(docMeta);
        tagService.getTagsInNamesAndSaveForDoc(editDocDTO.getTagDTOs(), docMeta);

        docMeta.setLastEdit(new Timestamp(System.currentTimeMillis()));

        docMetaRepository.save(docMeta);
        docSearchRepository.save(docES);

        return ResponseEntity.ok(HttpStatus.OK);
    }
    public void deleteDocumentsItself() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of("document"));
        if (indexOps.exists()) {
            boolean isDeleted = indexOps.delete();
            if (isDeleted) {
                System.out.println("Index '" + "document" + "' deleted successfully.");
            } else {
                System.out.println("Failed to delete index '" + "document" + "'.");
            }
        } else {
            System.out.println("Index '" + "document" + "' does not exist.");
        }
    }

    public void addDocToFavouritesById(Long id) {
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        userService.addDocToFavourites(docMeta, crosswordUserDetails.getUser());
    }

    public void removeDocFromFavouritesById(Long id) {
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        userService.removeDocFromFavourites(docMeta, crosswordUserDetails.getUser());
    }
}
