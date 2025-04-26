package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.elasticsearch.DigestCoreSearchRepository;
import com.backend.crosswords.corpus.repositories.elasticsearch.DigestSearchRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestCoreRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestRepository;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.IdsQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DigestService {
    private final DigestCoreRepository coreRepository;
    private final DigestSubscriptionService subscriptionService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestTemplateService templateService;
    private final DigestRepository digestRepository;
    private final DigestSearchRepository digestSearchRepository;
    private final DocService docService;
    private final DigestCoreSearchRepository coreSearchRepository;
    private final DigestRatingService ratingService;
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final TagService tagService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final Queue<DigestTemplate> templates = new LinkedList<>();
    public DigestService(DigestCoreRepository digestCoreRepository, DigestSubscriptionService subscriptionService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService, DigestRepository digestRepository, DigestSearchRepository digestSearchRepository, DocService docService, DigestCoreSearchRepository coreSearchRepository, DigestRatingService ratingService, UserService userService, RestTemplate restTemplate, TagService tagService, ElasticsearchOperations elasticsearchOperations) {
        this.coreRepository = digestCoreRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
        this.digestRepository = digestRepository;
        this.digestSearchRepository = digestSearchRepository;
        this.docService = docService;
        this.coreSearchRepository = coreSearchRepository;
        this.ratingService = ratingService;
        this.userService = userService;
        this.restTemplate = restTemplate;
        this.tagService = tagService;
        this.elasticsearchOperations = elasticsearchOperations;
    }
    @Transactional
    protected DigestCore createNewDigestCore(DigestTemplate template) {
        template = templateService.getTemplateFromId(template.getUuid()); // необходимо, чтобы сделать полную загрузку данных, избегаю ленивую
        var docMetas = docService.getAllDocsByTemplate(template);
        DigestCore core = new DigestCore();
        core.setDate(new Timestamp(System.currentTimeMillis()));
        core.setDocs(docMetas);
        core.setTemplate(template);

        core = coreRepository.save(core);
        docService.setCoreForDocs(core, docMetas);
        core = coreRepository.save(core);

        StringBuilder docsText = new StringBuilder();
        for (var docMeta : docMetas) {
            docsText.append(docService.getDocTextByDocId(docMeta.getId()));
        }
        DigestCoreES digestCoreES = new DigestCoreES(core.getId(), docsText.toString()); // TODO добавить подключение к сервису создания дайджестов и получать текст от него
        coreSearchRepository.save(digestCoreES);

        return core;
    }
    private void createNewDigests() {
        List<Digest> digests = new ArrayList<>();
        List<DigestES> digestsES = new ArrayList<>();
        while (!templates.isEmpty()) {
            var template = templates.poll();
            var core = this.createNewDigestCore(template);
            for (var subscription : subscriptionService.getAllDigestSubscriptionsByTemplate(template)) {
                var coreId = core.getId();
                var subscriptionId = subscription.getId();
                Digest digest = new Digest(new DigestId(coreId, subscriptionId), core, subscription);
                digests.add(digest);
                String digestESId = coreId + "-" + subscriptionId;
                digestsES.add(new DigestES(digestESId, subscriptionService.getDigestSubscriptionsTitle(subscriptionId), core.getDate()));
            }
        }
        digestRepository.saveAll(digests);
        digestSearchRepository.saveAll(digestsES);
    }

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduledDigestCreation() {
        System.out.println("The start of creating digests");
        templates.addAll(templateService.getAllTemplates());
        this.createNewDigests();
        System.out.println("The end of creating digests");
    }

    public Digest getDigestById(String digestId) {
        var ids = digestId.split("-");
        DigestId id;
        try {
            id = new DigestId(Long.valueOf(ids[0]), Long.valueOf(ids[1]));
        } catch (Exception e) {
            throw new NoSuchElementException("There is no digests with such id!");
        }
        return digestRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
    }

    public CertainDigestDTO getDigestByIdAndTransformIntoDTO(String digestId, User user) {
        CertainDigestDTO certainDigestDTO = new CertainDigestDTO();
        var digest = this.getDigestById(digestId);
        var core = digest.getCore();
        var coreES = coreSearchRepository.findById(core.getId()).orElseThrow(() -> new NoSuchElementException("There is no digest cores with such id!"));
        var subscription = digest.getSubscription();
        var subscriptionES = subscriptionService.getDigestSubscriptionESById(subscription.getId());
        var template = templateService.getTemplateFromId(core.getTemplate().getUuid());
        DigestSubscriptionSettings subscriptionSettings;

        if (user != null) {
            try {
                subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
            } catch (NoSuchElementException e) {
                subscriptionSettings = null;
            }
            certainDigestDTO.setIsAuthed(true);
        } else {
            subscriptionSettings = null;
            certainDigestDTO.setIsAuthed(null);
        }


        certainDigestDTO.setId(digestId);

        certainDigestDTO.setTitle(subscriptionES.getTitle());

        certainDigestDTO.setAverageRating(ratingService.getCoresAverageRating(core));
        certainDigestDTO.setUserRating(ratingService.getCoresUsersRating(core, user));

        for (var source : template.getSources()) {
            certainDigestDTO.getSources().add(source.getRussianName());
        }
        for (var tag : template.getTags()) {
            certainDigestDTO.getTags().add(tag.getName());
        }

        certainDigestDTO.setDescription(subscription.getDescription());
        certainDigestDTO.setText(coreES.getText());

        certainDigestDTO.setDate(core.getDate());

        certainDigestDTO.setIsPublic(subscription.getIsPublic());

        String ownersUsername = subscription.getOwner().getUsername();
        certainDigestDTO.setIsOwner(user == null || Objects.equals(user.getUsername(), ownersUsername));
        certainDigestDTO.setOwner(ownersUsername);

        for (var doc : core.getDocs()) {
            BasedOnDTO basedOnDTO = new BasedOnDTO(doc.getId(), docService.getDocTitleByDocId(doc.getId()), doc.getUrl());
            certainDigestDTO.getBasedOn().add(basedOnDTO);
        }
        if (subscriptionSettings == null) {
            certainDigestDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(false, false, false));
        } else {
            certainDigestDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(subscriptionSettings.getSendToMail(), subscriptionSettings.getMobileNotifications(), true));
        }

        return certainDigestDTO;
    }

    public DigestsDTO getAllDigests(User user, Integer pageNumber, Integer matchesPerPage) {
        pageNumber = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        matchesPerPage = matchesPerPage == null || matchesPerPage <= 0 ? 20 : matchesPerPage;
        Sort sort = Sort.by(Sort.Direction.DESC, "core.date");
        Pageable pageable = PageRequest.of(pageNumber, matchesPerPage, sort);
        Slice<Digest> digestPage = digestRepository.findAll(pageable);
        int nextPage = digestPage.hasNext() ? pageNumber + 1 : -1;
        return this.transformDigestsIntoDigestsDTO(digestPage.getContent(), user, nextPage);
    }

    public void rateDigestCoreByDigestId(String id, Integer digestCoreRating, User user) {
        if (digestCoreRating != null && (digestCoreRating < 1 || digestCoreRating > 5)) {
            throw new IllegalArgumentException("Rating's arguments must be in range of 1 to 5!");
        }
        var digest = this.getDigestById(id);
        var core = digest.getCore();
        user = userService.loadUserById(user.getId());
        ratingService.createRating(core, user, digestCoreRating);
    }

    private List<String> filterDigestsAndGetESIds(Collection<Digest> searchResult, Timestamp dateFrom, Timestamp dateTo, List<String> tags, List<String> sources, Boolean subscribeOnly, User user) {
        List<String> digests = new ArrayList<>();
        for (var digest : searchResult) {
            if (this.equalsMetaData(dateFrom, dateTo, tags, sources, subscribeOnly, digest, user)) {
                String digestESId = digest.getCore().getId() + "-" + digest.getSubscription().getId();
                digests.add(digestESId);
            }
        }
        return digests;
    }

    public DigestsDTO getDigestsBySearchAndTransformIntoDTO(String searchBody, Timestamp dateFrom, Timestamp dateTo, List<String> tags, List<String> sources, Boolean subscribeOnly, User user, Integer pageNumber, Integer matchesPerPage) {
        pageNumber = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        matchesPerPage = matchesPerPage == null || matchesPerPage <= 0 ? 10 : matchesPerPage;
        if (dateFrom != null && dateTo != null && dateFrom.after(dateTo)) {
            var nothing = dateFrom;
            dateFrom = dateTo;
            dateTo = nothing;
        }
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();
        var filtersIds = this.filterDigestsAndGetESIds(digestRepository.findAll(), dateFrom, dateTo, tags, sources, subscribeOnly, user);
        IdsQueryBuilder filterBuilder = QueryBuilders.idsQuery().addIds(filtersIds.toArray(new String[0]));
        List<Digest> digests;
        if (searchBody != null) {
            QueryBuilder queryBuilder = QueryBuilders.matchPhraseQuery("title", searchBody);
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                    .must(filterBuilder)
                    .must(queryBuilder);
            searchQueryBuilder.withFilter(boolQuery);
        } else {
            searchQueryBuilder.withFilter(filterBuilder);
        }
        var searchQuery = searchQueryBuilder
                .withPageable(PageRequest.of(pageNumber, matchesPerPage))
                .withSort(Sort.by(Sort.Order.desc("date")))
                .build();
        var searchHits = elasticsearchOperations.search(searchQuery, DigestES.class, IndexCoordinates.of("digest"));
        digests = new ArrayList<>();
        searchHits.forEach(hit ->
        {
            var digestES = hit.getContent();
            var ids = digestES.getId().split("-");
            var id = new DigestId(Long.valueOf(ids[0]), Long.valueOf(ids[1]));
            var digest = digestRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
            digests.add(digest);
        });
        int nextPage = pageNumber + 1;
        if (searchHits.getTotalHits() <= (long) nextPage * matchesPerPage) {
            nextPage = -1;
        }
        return this.transformDigestsIntoDigestsDTO(digests, user, nextPage);
    }
    private DigestsDTO transformDigestsIntoDigestsDTO(List<Digest> digests, User user, Integer nextPage) {
        DigestsDTO digestsDTO = new DigestsDTO();
        digestsDTO.setIsAuthed(user != null);
        digestsDTO.setNextPage(nextPage);
        for (var digest : digests) {
            var core = digest.getCore();
            var coreES = coreSearchRepository.findById(core.getId()).orElseThrow(() -> new NoSuchElementException("There is no digest cores with such id!"));
            var subscription = digest.getSubscription();
            var subscriptionES = subscriptionService.getDigestSubscriptionESById(subscription.getId());
            var template = templateService.getTemplateFromId(core.getTemplate().getUuid());

            DigestSubscriptionSettings subscriptionSettings;
            if (user != null) {
                try {
                    subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
                } catch (NoSuchElementException e) {
                    subscriptionSettings = null;
                }
            } else {
                subscriptionSettings = null;
            }

            DigestDTO digestDTO = new DigestDTO();

            digestDTO.setId(digest.getCore().getId().toString() + "-" + digest.getSubscription().getId().toString());

            digestDTO.setTitle(subscriptionES.getTitle());

            digestDTO.setAverageRating(ratingService.getCoresAverageRating(core));
            digestDTO.setUserRating(ratingService.getCoresUsersRating(core, user));

            for (var source : template.getSources()) {
                digestDTO.getSources().add(source.getRussianName());
            }
            for (var tag : template.getTags()) {
                digestDTO.getTags().add(tag.getName());
            }

            digestDTO.setDescription(subscription.getDescription());
            digestDTO.setText(coreES.getText());

            digestDTO.setDate(core.getDate());

            digestDTO.setIsPublic(subscription.getIsPublic());

            String ownersUsername = subscription.getOwner().getUsername();
            digestDTO.setIsOwner(user == null || Objects.equals(user.getUsername(), ownersUsername));
            digestDTO.setOwner(ownersUsername);

            for (var doc : core.getDocs()) {
                digestDTO.getUrls().add(doc.getUrl());
            }
            if (subscriptionSettings == null) {
                digestDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(false, false, false));
            } else {
                digestDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(subscriptionSettings.getSendToMail(), subscriptionSettings.getMobileNotifications(), true));
            }
            digestsDTO.getDigests().add(digestDTO);
        }
        return digestsDTO;
    }
    private boolean equalsMetaData(Timestamp dateFrom, Timestamp dateTo, List<String> tags, List<String> sources, Boolean subscribe_only, Digest digest, User user) {
        var core = digest.getCore();
        var subscription = digest.getSubscription();
        var template = templateService.getTemplateFromId(core.getTemplate().getUuid());
        var digestDate = core.getDate();
        Set<String> digestSources = new HashSet<>();
        for (var source : template.getSources()) {
            digestSources.add(source.getRussianName());
        }
        boolean first = ((dateFrom == null || dateFrom.before(digestDate)) &&
                (dateTo == null || dateTo.after(digestDate)) &&
                (sources == null || sources.isEmpty() || digestSources.containsAll(sources)) &&
                (tags == null || tags.isEmpty() || tagService.getSetOfTagsNames(template.getTags()).containsAll(tags)));

        DigestSubscriptionSettings subscriptionSettings;
        if (user != null) {
            try {
                subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
            } catch (NoSuchElementException e) {
                subscriptionSettings = null;
            }
        } else {
            subscriptionSettings = null;
        }

        boolean second = false;
        if (subscription.getIsPublic()) {
            if (subscribe_only) {
                second = subscriptionSettings != null;
            } else {
                second = true;
            }
        } else {
            second = subscriptionSettings != null;
        }
        return first && second;
    }

    public void updateDigestSubscriptionSettingsForUserByDigestId(String id, DigestSubscriptionSettingsDTO subscriptionSettingsDTO, User user) {
        var digest = this.getDigestById(id);
        var subscription = digest.getSubscription();
        var delete = subscriptionSettingsService.updateDigestSubscriptionSettingsForUser(subscription, user, subscriptionSettingsDTO);
        subscriptionService.checkDigestSubscriptionDeletion(delete, subscription, user);
    }

    public List<String> getAllDigestSubscriptionsUsersExceptOwner(String id) {
        var digest = this.getDigestById(id);
        var subscription = digest.getSubscription();
        return subscriptionSettingsService.getAllDigestSubscriptionsUsersUsernamesExceptOwner(subscription);
    }

    public void changeDigestSubscriptionsOwnerByDigestId(User user, String id, String owner) throws IllegalAccessException {
        var digest = this.getDigestById(id);
        var subscriptionId = digest.getSubscription().getId();
        subscriptionService.changeDigestSubscriptionsOwner(user, subscriptionId, owner);
    }
}
