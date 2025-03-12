package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.CreateDigestSubscriptionDTO;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestTemplate;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestTemplateRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class DigestService {
    private final TagService tagService;
    private final DigestTemplateRepository templateRepository;
    private final ModelMapper modelMapper;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestSubscriptionRepository subscriptionRepository;
    private final UserService userService;

    public DigestService(TagService tagService, DigestTemplateRepository templateRepository, ModelMapper modelMapper, DigestSubscriptionSettingsService subscriptionSettingsService, DigestSubscriptionRepository subscriptionRepository, UserService userService) {
        this.tagService = tagService;
        this.templateRepository = templateRepository;
        this.modelMapper = modelMapper;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
    }

    @Transactional
    public void createDigestSubscription(User owner, CreateDigestSubscriptionDTO createDigestSubscriptionDTO) {
        owner = userService.loadUserById(owner.getId());

        var subscription = modelMapper.map(createDigestSubscriptionDTO, DigestSubscription.class);
        subscription.setSendToMail(createDigestSubscriptionDTO.getSubscribeOptions().getSendToMail());
        subscription.setMobileNotifications(createDigestSubscriptionDTO.getSubscribeOptions().getMobileNotifications());
        subscription.setOwner(owner);
        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setSubscribersForSubscription(createDigestSubscriptionDTO.getFollowers(), subscription);

        Set<Tag> tags = tagService.getTagsInNames(createDigestSubscriptionDTO.getTags());
        Set<Source> sources = new HashSet<>();
        for (var source : createDigestSubscriptionDTO.getSources()) {
            sources.add(Source.fromRussianName(source));
        }
        var templateUuid = DigestTemplate.generateUuid(sources, tags);
        if (templateRepository.existsById(templateUuid)) {
            templateRepository.save(new DigestTemplate(sources, tags));
        }
    }
}
