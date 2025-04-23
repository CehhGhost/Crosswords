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
import org.springframework.security.web.firewall.RequestRejectedException;
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
    private final DocRatingService ratingService;
    private final UserService userService;
    private final PackageService packageService;
    private final AnnotationService annotationService;
    private final CommentService commentService;

    public DocService(ModelMapper modelMapper, DocSearchRepository docSearchRepository, DocMetaRepository docMetaRepository, ElasticsearchOperations elasticsearchOperations, TagService tagService, DocRatingService ratingService, UserService userService, PackageService packageService, AnnotationService annotationService, CommentService commentService) {
        this.modelMapper = modelMapper;
        this.docSearchRepository = docSearchRepository;
        this.docMetaRepository = docMetaRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.tagService = tagService;
        this.ratingService = ratingService;
        this.userService = userService;
        this.packageService = packageService;
        this.annotationService = annotationService;
        this.commentService = commentService;
    }

    private DocDTO transformDocIntoDocDTO(DocMeta docMeta) {
        var doc = modelMapper.map(docMeta, DocDTO.class);
        var docES = docSearchRepository.findById(doc.getId()).orElseThrow();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            var user = crosswordUserDetails.getUser();
            doc.setFavourite(packageService.checkDocInFavourites(user, docMeta));
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
        System.out.println("Количество символов принятого документа " + docES.getTitle() + ": " + docES.getText().length());
        docES.setId(docMeta.getId());
        docES = docSearchRepository.save(docES);
        System.out.println("Количество символов сохраненного документа: " + docES.getTitle() + ": " + docES.getText().length());
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
        var docMeta = docMetaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No documents with such id!"));
        var docDTO = this.transformDocIntoDocDTO(docMeta);
        if (docDTO.getDocsAnnotations() == null) {
            docDTO.setDocsAnnotations(new ArrayList<>());
        }
        for (var annotation : docMeta.getAnnotations()) {
            docDTO.getDocsAnnotations().add(modelMapper.map(annotation, AnnotationDTO.class));
        }
        return docDTO;
    }

    private SearchResultDTO formSearchResultDTO(int pageNumber, List<DocDTO> resultHits) {
        boolean isAuthed = true;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        } catch (ClassCastException e) {
            isAuthed = false;
        }
        return new SearchResultDTO(pageNumber, isAuthed, resultHits);
    }

    public SearchResultDTO searchDocs(SearchDocDTO searchDocDTO, User user) {
        List<DocDTO> resultHits = new ArrayList<>();
        QueryBuilder queryBuilder = null;
        float minScore = 1F;
        switch (searchDocDTO.getSearchMode()) {
            case "id" -> {
                resultHits.add(this.getDocById(Long.parseLong(searchDocDTO.getSearchTerm())));
                return this.formSearchResultDTO(searchDocDTO.getPageNumber(), resultHits);
            }
            case "semantic" -> {
                if (searchDocDTO.getMatchesPerPage() <= 0) {
                    return this.formSearchResultDTO(searchDocDTO.getPageNumber(), resultHits);
                }
                queryBuilder = QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("text", searchDocDTO.getSearchTerm()).fuzziness(Fuzziness.AUTO))
                        .should(QueryBuilders.matchPhraseQuery("text", searchDocDTO.getSearchTerm())).boost(2.0F);
                float percentage = searchDocDTO.getApprovalPercentage() == null ? 0.5F : searchDocDTO.getApprovalPercentage();
                minScore = searchDocDTO.getSearchTerm().split(" ").length * percentage;
            }
            case "certain" -> {
                if (searchDocDTO.getMatchesPerPage() <= 0) {
                    return this.formSearchResultDTO(searchDocDTO.getPageNumber(), resultHits);
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
            if (this.equalsMetaData(doc, searchDocDTO, user)) {
                filtersIds.add(doc.getId());
            }
        }
        if (searchDocDTO.getPageNumber() == null || searchDocDTO.getPageNumber() < 0) {
            searchDocDTO.setPageNumber(0);
        }
        if (searchDocDTO.getMatchesPerPage() == null || searchDocDTO.getMatchesPerPage() < 0) {
            searchDocDTO.setMatchesPerPage(10);
        }
        QueryBuilder filterBuilder = QueryBuilders.termsQuery("id", filtersIds);
        var searchQuery = new NativeSearchQueryBuilder()
                .withFilter(filterBuilder)
                .withQuery(queryBuilder)
                .withMinScore(minScore)
                .withPageable(PageRequest.of(searchDocDTO.getPageNumber(), searchDocDTO.getMatchesPerPage()))
                .build();
        var searchHits = elasticsearchOperations.search(searchQuery, DocES.class, IndexCoordinates.of("document"));
        searchHits.forEach(hit ->
        {
            var docES = hit.getContent();
            var docMeta = docMetaRepository.findById(docES.getId()).orElseThrow();
            resultHits.add(this.transformDocIntoDocDTO(docMeta));
            System.out.println(hit.getScore());
        });
        // System.out.println(hits.getTotalHits());
        int nextPage = searchDocDTO.getPageNumber() + 1;
        if (searchHits.getTotalHits() <= (long) nextPage * searchDocDTO.getMatchesPerPage()) {
            nextPage = -1;
        }
        return this.formSearchResultDTO(nextPage, resultHits);
    }

    private boolean equalsMetaData(DocMeta doc, SearchDocDTO searchDocDTO, User user) {
        if (searchDocDTO == null) {
            return true;
        }
        if( (searchDocDTO.getDateFrom() == null || searchDocDTO.getDateFrom().before(doc.getDate())) &&
                (searchDocDTO.getDateTo() == null || searchDocDTO.getDateTo().after(doc.getDate())) &&
                (searchDocDTO.getLanguage() == null || searchDocDTO.getLanguage().isEmpty() || searchDocDTO.getLanguage().contains(doc.getLanguage().toString())) &&
                (searchDocDTO.getSources() == null || searchDocDTO.getSources().isEmpty() || searchDocDTO.getSources().contains(doc.getSource().getRussianName())) &&
                (searchDocDTO.getTags() == null || searchDocDTO.getTags().isEmpty() || tagService.getSetOfTagsNames(doc.getTags()).containsAll(searchDocDTO.getTags()))) {
            if (searchDocDTO.getFolders() != null && !searchDocDTO.getFolders().isEmpty() && user != null) {
                for (var packageName : searchDocDTO.getFolders()) {
                    if (packageService.getPackageByName(packageName, user).getDocs().contains(doc)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        return false;
    }

    public void deleteDocById(Long id) {
        var checkDocMeta = docMetaRepository.findById(id);
        if (checkDocMeta.isEmpty()) {
            throw new NoSuchElementException("There is no documents in Postgres with such id!");
        }
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        tagService.removeTagsFromDoc(docMeta);
        packageService.removeDocFromPackages(docMeta);
        docMetaRepository.delete(docMeta);
        // TODO учесть, что ES не понимает @Transactional и в случае ошибки не откатит изменения обратно, поэтому может произойти повреждение данных в ES
        var checkDocEs = docSearchRepository.findById(id);
        if (checkDocEs.isEmpty()) {
            throw new NoSuchElementException("There is no documents in ES with such id!");
        }
        var docES = docSearchRepository.findById(id).orElseThrow();
        docSearchRepository.delete(docES);
    }

    public void rateDocById(Long id, RateDocDTO rateDocDTO) {
        if (rateDocDTO.getClassificationRating() < 1 || rateDocDTO.getClassificationRating() > 5 || rateDocDTO.getSummaryRating() < 1 || rateDocDTO.getSummaryRating() > 5) {
            throw new IllegalArgumentException("Rating's arguments must be in range of 1 to 5!");
        }
        DocMeta doc;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        User user = crosswordUserDetails.getUser();

        doc = docMetaRepository.findById(id).orElseThrow();
        docSearchRepository.findById(id).orElseThrow(); // проверка, что базы данных синхронизированы
        user = userService.loadUserById(user.getId());

        ratingService.createRating(doc, user, rateDocDTO.getSummaryRating(), rateDocDTO.getClassificationRating());
    }

    @Transactional
    public ResponseEntity<HttpStatus> editDocById(Long id, EditDocDTO editDocDTO) {
        var checkDocMeta = docMetaRepository.findById(id);
        if (checkDocMeta.isEmpty()) {
            throw new NoSuchElementException("There is no documents in Postgres with such id!");
        }
        DocMeta docMeta = checkDocMeta.get();

        var checkDocES = docSearchRepository.findById(id);
        if (checkDocES.isEmpty()) {
            throw new NoSuchElementException("There is no documents in ES with such id!");
        }
        DocES docES = checkDocES.get();

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
        List<IndexOperations> indexOpsList = List.of(
                elasticsearchOperations.indexOps(IndexCoordinates.of("document")),
                elasticsearchOperations.indexOps(IndexCoordinates.of("digest_core")),
                elasticsearchOperations.indexOps(IndexCoordinates.of("digest_subscription"))
        );
        for (var indexOps : indexOpsList) {
            if (indexOps.exists()) {
                boolean isDeleted = indexOps.delete();
                if (isDeleted) {
                    System.out.println("Index '" + "document" + "' deleted successfully.");
                } else {
                    throw new RequestRejectedException("Failed to delete index '" + "document" + "'.");
                }
            } else {
                throw new NoSuchElementException("Index '" + "document" + "' does not exist.");
            }
        }
    }

    public void addDocByIdIntoPackageByName(User user, Long docId, String packageName) {
        var check = docMetaRepository.findById(docId);
        if (check.isEmpty()) {
            throw new NoSuchElementException("There is no documents with such id!");
        }
        var doc = check.get();
        packageService.addDocIntoPackageByName(user, doc, packageName);
    }

    public void removeDocByIdFromPackageByName(User user, Long docId, String packageName) {
        var check = docMetaRepository.findById(docId);
        if (check.isEmpty()) {
            throw new NoSuchElementException("There is no documents with such id!");
        }
        var doc = check.get();
        packageService.removeDocFromPackageByName(user, doc, packageName);
    }

    public void addDocToFavouritesById(Long id) {
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        packageService.addDocToFavourites(crosswordUserDetails.getUser(), docMeta);
    }

    public void removeDocFromFavouritesById(Long id) {
        var docMeta = docMetaRepository.findById(id).orElseThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        packageService.removeDocFromFavourites(crosswordUserDetails.getUser(), docMeta);
    }

    @Transactional
    public Long annotateDocById(User user, Long id, CreateUpdateAnnotationDTO createUpdateAnnotationDTO) {
        var docMeta = docMetaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        Annotation annotation = annotationService.createAnnotation(user, docMeta, createUpdateAnnotationDTO);
        docMeta.getAnnotations().add(annotation);
        docMetaRepository.save(docMeta);
        return annotation.getId();
    }

    @Transactional
    public void deleteAnnotationByIdFromDocById(User user, Long docId, Long annotationId) {
        var docMeta = docMetaRepository.findById(docId).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        annotationService.deleteAnnotationByIdFromDoc(user, docMeta, annotationId);
        docMetaRepository.save(docMeta);
    }

    @Transactional
    public Comment commentDocById(User user, Long id, CreateUpdateCommentDTO createUpdateCommentDTO) {
        var docMeta = docMetaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        Comment comment = commentService.commentDoc(user, docMeta, createUpdateCommentDTO);
        docMeta.getComments().add(comment);
        docMetaRepository.save(docMeta);
        return comment;
    }

    @Transactional
    public void deleteCommentByIdFromDocById(User user, Long docId, Long commentId) {
        var docMeta = docMetaRepository.findById(docId).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        commentService.deleteCommentByIdFromDoc(user, docMeta, commentId);
        docMetaRepository.save(docMeta);
    }

    public List<PackagesForDocDTO> getPackagesForDocAndTransformIntoDTO(Long id, User user) {
        var docMeta = docMetaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        List<PackagesForDocDTO> docsPackages = new ArrayList<>();
        for (var usersPackage : packageService.getPackagesForUser(user)) {
            PackagesForDocDTO getPackagesForDocDTO = new PackagesForDocDTO();
            getPackagesForDocDTO.setName(usersPackage.getId().getName());
            getPackagesForDocDTO.setIsIncluded(usersPackage.getDocs().contains(docMeta));
            docsPackages.add(getPackagesForDocDTO);
        }
        return docsPackages;
    }

    public List<Comment> getAllUsersCommentsFromDocById(User user, Long docId) {
        return commentService.getAllUsersCommentsFromDoc(user, docMetaRepository.findById(docId).orElseThrow(
                () -> new NoSuchElementException("There is no documents with such id!")));
    }

    @Transactional
    public Comment updateCommentByIdForDocById(User user, Long docId, Long commentId, CreateUpdateCommentDTO createUpdateCommentDTO) {
        var docMeta = docMetaRepository.findById(docId).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        Comment comment = commentService.updateCommentByIdForDoc(user, docMeta, commentId, createUpdateCommentDTO);
        docMetaRepository.save(docMeta);
        return comment;
    }

    @Transactional
    public void updateAnnotationByIdForDocById(User user, Long docId, Long annotationId, CreateUpdateAnnotationDTO createUpdateAnnotationDTO) {
        var docMeta = docMetaRepository.findById(docId).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!"));
        annotationService.updateAnnotationByIdForDoc(user, docMeta, annotationId, createUpdateAnnotationDTO);
        docMetaRepository.save(docMeta);
    }

    public List<DocMeta> getAllDocsByTemplate(DigestTemplate template) {
        List<DocMeta> docs = new ArrayList<>();
        for (var doc : docMetaRepository.findAllWithTags()) {
            Boolean first = (template.getTags().isEmpty() || tagService.getSetOfTagsNames(doc.getTags()).containsAll(tagService.getSetOfTagsNames(template.getTags())));
            Boolean second = (template.getSources().isEmpty() || template.getSources().contains(doc.getSource()));
            if (first && second) {
                docs.add(doc);
            }
        }
        return docs;
    }

    public String getDocTextByDocId(Long id) {
        return docSearchRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!")).getText();
    }

    public String getDocTitleByDocId(Long id) {
        return docSearchRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no documents with such id!")).getTitle();
    }

    public List<DocDTO> transformDocsIntoDTO(Set<DocMeta> docs) {
        List<DocDTO> docDTOs = new ArrayList<>();
        for (var doc : docs) {
            docDTOs.add(this.transformDocIntoDocDTO(doc));
        }
        return docDTOs;
    }
}
