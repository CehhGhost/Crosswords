package com.backend.crosswords.config;

import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.services.DocService;
import com.backend.crosswords.corpus.services.TagService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements ApplicationRunner, DisposableBean {
    private final UserService userService;
    private final TagService tagService;
    private final DocService docService;
    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    public DataLoader(UserService userService, TagService tagService, DocService docService) {
        this.userService = userService;
        this.tagService = tagService;
        this.docService = docService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        userService.createDefaultUsers();
        tagService.createDefaultTags();
    }

    @Override
    public void destroy() throws Exception {
        if (ddlAuto.equals("create-drop")) {
            docService.deleteDocumentsItself();
        }
    }
}
