package com.backend.crosswords.config;

import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.services.TagService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements ApplicationRunner {
    private final UserService userService;
    private final TagService tagService;

    public DataLoader(UserService userService, TagService tagService) {
        this.userService = userService;
        this.tagService = tagService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        userService.createDefaultUsers();
        tagService.createDefaultTags();
    }
}
