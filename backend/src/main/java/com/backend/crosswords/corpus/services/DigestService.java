package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.dto.DigestDTO;
import com.backend.crosswords.corpus.dto.DigestsDTO;
import com.backend.crosswords.corpus.dto.GetSubscribeOptionsDTO;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.elasticsearch.DigestCoreSearchRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestCoreRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestRepository;
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
    private final RestTemplate restTemplate;
    private final Queue<DigestTemplate> templates = new LinkedList<>();
    public DigestService(DigestCoreRepository digestCoreRepository, DigestSubscriptionService subscriptionService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService, DigestRepository digestRepository, DocService docService, DigestCoreSearchRepository coreSearchRepository, DigestRatingService ratingService, RestTemplate restTemplate) {
        this.coreRepository = digestCoreRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
        this.digestRepository = digestRepository;
        this.docService = docService;
        this.coreSearchRepository = coreSearchRepository;
        this.ratingService = ratingService;
        this.restTemplate = restTemplate;
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

    // TODO доделать
    public Object getDigestById(String digestId, User user) {
        var ids = digestId.split("#");
        var core = coreRepository.findById(Long.valueOf(ids[0])).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
        var template = core.getTemplate();
        var subscription = subscriptionService.getDigestSubscriptionById(Long.valueOf(ids[1]));
        var rating = core.getRatings();
        var subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
        return null;
    }

    public DigestsDTO getAllDigests(User user) {
        var digests = digestRepository.findAll();
        DigestsDTO digestsDTO = new DigestsDTO();
        for (var digest : digests) {
            var core = digest.getCore();
            var coreES = coreSearchRepository.findById(core.getId()).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
            var subscription = digest.getSubscription();
            var template = templateService.getTemplateFromId(core.getTemplate().getUuid());
            DigestSubscriptionSettings subscriptionSettings;
            try {
                subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
            } catch (NoSuchElementException e) {
                subscriptionSettings = null;
            }

            DigestDTO digestDTO = new DigestDTO();

            digestDTO.setId(digest.getCore().getId().toString() + digest.getSubscription().getId().toString());

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
}
