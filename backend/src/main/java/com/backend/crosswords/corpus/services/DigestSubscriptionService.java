package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.CreateDigestSubscriptionDTO;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DigestSubscriptionService {
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final DigestSubscriptionRepository subscriptionRepository;
    private final TagService tagService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestTemplateService templateService;

    public DigestSubscriptionService(UserService userService, ModelMapper modelMapper, DigestSubscriptionRepository subscriptionRepository, TagService tagService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.tagService = tagService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
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
        templateService.createTemplateBySourcesAndTags(sources, tags);
    }

    public DigestSubscription getDigestSubscriptionById(Long id) {
        return subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
    }
}
