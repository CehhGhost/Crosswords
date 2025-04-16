package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.elasticsearch.DigestCoreSearchRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestCoreRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestRepository;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
    private final DocService docService;
    private final DigestCoreSearchRepository coreSearchRepository;
    private final DigestRatingService ratingService;

    private final UserService userService;
    private final RestTemplate restTemplate;
    private final TagService tagService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final Queue<DigestTemplate> templates = new LinkedList<>();
    public DigestService(DigestCoreRepository digestCoreRepository, DigestSubscriptionService subscriptionService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService, DigestRepository digestRepository, DocService docService, DigestCoreSearchRepository coreSearchRepository, DigestRatingService ratingService, UserService userService, RestTemplate restTemplate, TagService tagService, ElasticsearchOperations elasticsearchOperations) {
        this.coreRepository = digestCoreRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
        this.digestRepository = digestRepository;
        this.docService = docService;
        this.coreSearchRepository = coreSearchRepository;
        this.ratingService = ratingService;
        this.userService = userService;
        this.restTemplate = restTemplate;
        this.tagService = tagService;
        this.elasticsearchOperations = elasticsearchOperations;
    }
    private DigestCore createNewDigestCore(DigestTemplate template) {
        template = templateService.getTemplateFromId(template.getUuid()); // необходимо, чтобы сделать полную загрузку данных, избегаю ленивую
        var docMetas = docService.getAllDocsByTemplate(template);
        DigestCore core = new DigestCore();
        core.setDate(new Timestamp(System.currentTimeMillis()));
        core.setDocs(docMetas);
        core.setTemplate(template);

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
        while (!templates.isEmpty()) {
            var template = templates.poll();
            var core = this.createNewDigestCore(template);
            for (var subscription : subscriptionService.getAllDigestSubscriptionsByTemplate(template)) {
                Digest digest = new Digest(new DigestId(core.getId(), subscription.getId()), core, subscription);
                digests.add(digest);
            }
        }
        digestRepository.saveAll(digests);
    }

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduledDigestCreation() {
        System.out.println("The start of creating digests");
        templates.addAll(templateService.getAllTemplates());
        this.createNewDigests();
        System.out.println("The end of creating digests");
    }

    private Digest getDigestById(String digestId) {
        var ids = digestId.split("#");
        return digestRepository.findById(new DigestId(Long.valueOf(ids[0]), Long.valueOf(ids[1]))).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
    }

    public CertainDigestDTO getDigestById(String digestId, User user) {
        CertainDigestDTO certainDigestDTO = new CertainDigestDTO();
        var digest = this.getDigestById(digestId);
        var core = digest.getCore();
        var coreES = coreSearchRepository.findById(core.getId()).orElseThrow(() -> new NoSuchElementException("There is no digest cores with such id!"));
        var subscription = digest.getSubscription();
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

        certainDigestDTO.setTitle(subscription.getTitle());

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

    public DigestsDTO getAllDigests(User user) {
        var digests = digestRepository.findAll();
        return this.transformDigestsIntoDigestsDTO(digests, user);
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

    public DigestsDTO getDigestsBySearch(String searchBody, Timestamp dateFrom, Timestamp dateTo, List<String> tags, List<String> sources, Boolean subscribe_only, User user) {
        if (dateFrom != null && dateTo != null && dateFrom.after(dateTo)) {
            var nothing = dateFrom;
            dateFrom = dateTo;
            dateTo = nothing;
        }
        List<Digest> digests = new ArrayList<>();
        if (searchBody != null) {
            List<DigestSubscription> subscriptions = subscriptionService.getAllDigestSubscriptionsBySearchTerm(searchBody);
            for (var digest : digestRepository.findAllBySubscriptionIn(subscriptions)) {
                if (this.equalsMetaData(dateFrom, dateTo, tags, sources, subscribe_only, digest, user)) {
                    digests.add(digest);
                }
            }
        } else {
            for (var digest : digestRepository.findAll()) {
                if (this.equalsMetaData(dateFrom, dateTo, tags, sources, subscribe_only, digest, user)) {
                    digests.add(digest);
                }
            }
        }
        return this.transformDigestsIntoDigestsDTO(digests, user);
    }
    private DigestsDTO transformDigestsIntoDigestsDTO(List<Digest> digests, User user) {
        DigestsDTO digestsDTO = new DigestsDTO();
        digestsDTO.setIsAuthed(user != null); // TODO может ли незарегистрированный пользователь поиск по дайджестам?
        for (var digest : digests) {
            var core = digest.getCore();
            var coreES = coreSearchRepository.findById(core.getId()).orElseThrow(() -> new NoSuchElementException("There is no digest cores with such id!"));
            var subscription = digest.getSubscription();
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

            digestDTO.setId(digest.getCore().getId().toString() + "#" + digest.getSubscription().getId().toString());

            digestDTO.setTitle(subscription.getTitle());

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
            if (subscribe_only) {
                second = subscriptionSettings != null;
            }
        }
        return first && second;
    }

    public void updateDigestSubscriptionSettingsForUserByDigestId(String id, DigestSubscriptionSettingsDTO subscriptionSettingsDTO, User user) {
        var digest = this.getDigestById(id);
        var subscription = digest.getSubscription();
        subscriptionSettingsService.updateDigestSubscriptionSettingsForUser(subscription, user, subscriptionSettingsDTO);
    }
}
