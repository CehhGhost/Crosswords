package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.corpus.dto.CreateDocDTO;
import com.backend.crosswords.corpus.dto.EditDocDTO;
import com.backend.crosswords.corpus.dto.RateDocDTO;
import com.backend.crosswords.corpus.dto.SearchDocDTO;
import com.backend.crosswords.corpus.services.DocService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

// TODO создать роли и набор разрешений
// TODO сделать ручку для проверки набора разрешений
// TODO сделать ручку для проверки владения подпиской
// TODO сделать модель папок документов (при регистрации пользователя тут же создается папка избранное)
// TODO сделать ручку для получения всех папок пользователя, включая отображение присутствия выбранного документа в папках
// TODO ручка для удаления документа из папки

@RestController
@RequestMapping("/documents")
public class DocController {
    private final DocService docService;

    public DocController(DocService docService) {
        this.docService = docService;
    }

    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createDoc(@RequestBody CreateDocDTO createDocDTO) {
        docService.createDoc(createDocDTO);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<?> getAllDocs() {
        try {
            return ResponseEntity.ok(docService.getAllDocs());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Sorry, our database is corrupted (check ES)");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(docService.getDocById(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No documents with such id!");
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> getDocsBySearch(@RequestBody SearchDocDTO searchDocDTO) {
        try {
            return ResponseEntity.ok(docService.searchDocs(searchDocDTO));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No documents with such id!");
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteDocById(@PathVariable Long id) {
        return docService.deleteDocById(id);
    }
    @PatchMapping("/{id}/rate")
    public ResponseEntity<HttpStatus> rateDocById(@PathVariable Long id, @RequestBody RateDocDTO rateDocDTO) {
        return docService.rateDocById(id, rateDocDTO);
    }
    @PutMapping("/{id}/edit")
    public ResponseEntity<HttpStatus> updateDocById(@PathVariable Long id, @RequestBody EditDocDTO editDocDTO) {
        return docService.editDocById(id, editDocDTO);
    }
    // Очистка индекса документов из ES, если не предварительно не почистить доки из Postgre, то дальше возникнут ошибки
    @DeleteMapping("/itself")
    public ResponseEntity<HttpStatus> deleteDocumentsItself() {
        docService.deleteDocumentsItself();
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @PostMapping("/{id}/add_to_favourites")
    public ResponseEntity<?> addDocToFavouritesById(@PathVariable Long id) {
        try {
            docService.addDocToFavouritesById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No document with such id!");
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @PostMapping("/{id}/remove_from_favourites")
    public ResponseEntity<?> removeDocFromFavouritesById(@PathVariable Long id) {
        try {
            docService.removeDocFromFavouritesById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No document with such id!");
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
