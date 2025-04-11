package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.jpa.DigestCoreRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DigestService {
    private final DigestCoreRepository digestCoreRepository;
    private final DigestSubscriptionService subscriptionService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestTemplateService templateService;
    private final DigestRepository digestRepository;
    private final DocService docService;
    private final RestTemplate restTemplate;
    private Queue<DigestTemplate> templates = new LinkedList<>();
    public DigestService(DigestCoreRepository digestCoreRepository, DigestSubscriptionService subscriptionService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService, DigestRepository digestRepository, DocService docService, RestTemplate restTemplate) {
        this.digestCoreRepository = digestCoreRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
        this.digestRepository = digestRepository;
        this.docService = docService;
        this.restTemplate = restTemplate;
    }
    private DigestCore createNewDigestCore(DigestTemplate template) {
        template = templateService.getTemplateFromId(template.getUuid()); // необходимо, чтобы сделать полную загрузку данных, избегаю ленивую
        var docMetas = docService.getAllDocsByTemplate(template);
        DigestCore core = new DigestCore();
        core.setDate(new Timestamp(System.currentTimeMillis()));
        core.setDocs(docMetas);
        core.setTemplate(template);

        StringBuilder docsText = new StringBuilder();
        for (var docMeta : docMetas) {
            docsText.append(docService.getDocTextByDocId(docMeta.getId()));
        }
        core.setText(docsText.toString()); // TODO добавить подключение к сервису создания дайджестов и получать текст от него
        return digestCoreRepository.save(core);
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
        var core = digestCoreRepository.findById(Long.valueOf(ids[0])).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
        var template = core.getTemplate();
        var subscription = subscriptionService.getDigestSubscriptionById(Long.valueOf(ids[1]));
        var rating = core.getRatings();
        var subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
        return null;
    }
}
