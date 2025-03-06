package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.services.DocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Doc controller", description = "Controller for all operations with documents")
public class DocController {
    private final DocService docService;

    public DocController(DocService docService) {
        this.docService = docService;
    }

    @Operation(summary = "Create a document", description = "This is a simple creating document endpoint")
    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createDoc(@RequestBody CreateDocDTO createDocDTO) {
        docService.createDoc(createDocDTO);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Get all docs", description = "This endpoint gets you all documents with all possibly required information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searched documents",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocDTO.class)))),
            @ApiResponse(responseCode = "503", description = "If this endpoint cant find a document by id, then something is wrong with synchronizing Postgres and ES," +
                    "maybe use DELETE /documents/itself and delete all documents from Postgres," +
                    "because now all endpoints, corresponded with docs, may return errors and it could cause a lot of troubles")
    })
    @GetMapping
    public ResponseEntity<?> getAllDocs() {
        try {
            return ResponseEntity.ok(docService.getAllDocs());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Sorry, our database is corrupted (check ES)");
        }
    }

    @Operation(summary = "Get doc by id", description = "This endpoint gets you a document by its specified id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searched document", content = @Content(schema = @Schema(implementation = DocDTO.class))),
            @ApiResponse(responseCode = "404", description = "There is no document with such id!")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(docService.getDocById(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No documents with such id!");
        }
    }

    @Operation(summary = "Get docs by search", description = "This endpoint gets you all documents by your filtration's and search's arguments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searched documents",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Can appear dew to wrong id, while searching by id")
    })
    @PostMapping("/search")
    public ResponseEntity<?> getDocsBySearch(@RequestBody SearchDocDTO searchDocDTO) {
        try {
            return ResponseEntity.ok(docService.searchDocs(searchDocDTO));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No documents with such id!");
        }
    }
    @Operation(summary = "Delete doc by id", description = "This endpoint deletes document by id and destroys all connections with other models")
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteDocById(@PathVariable Long id) {
        return docService.deleteDocById(id);
    }
    @Operation(summary = "Rate doc by id", description = "This endpoint lets you rate document, parametres can be null or numbers from 1 to 5, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully rated a document"),
            @ApiResponse(responseCode = "401", description = "You are trying to rate a doc while not authenticated")
    })
    @PatchMapping("/{id}/rate")
    public ResponseEntity<HttpStatus> rateDocById(@PathVariable Long id, @RequestBody RateDocDTO rateDocDTO) {
        return docService.rateDocById(id, rateDocDTO);
    }
    @Operation(summary = "Edit doc by id", description = "This endpoint lets you rate document, parametres can be null or numbers from 1 to 5")
    @PutMapping("/{id}/edit")
    public ResponseEntity<HttpStatus> updateDocById(@PathVariable Long id, @RequestBody EditDocDTO editDocDTO) {
        return docService.editDocById(id, editDocDTO);
    }
    // Очистка индекса документов из ES, если предварительно не почистить доки из Postgres, то дальше возникнут ошибки
    @Operation(summary = "Delete index from ES", description = "This endpoint lets clean the document's index from ES, if you do not clean the docs from Postgres at the same time, then errors may occur")
    @DeleteMapping("/itself")
    public ResponseEntity<HttpStatus> deleteDocumentsItself() {
        docService.deleteDocumentsItself();
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Add doc to favourites", description = "This endpoint lets you add doc to favourites, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully added a document to favourites"),
            @ApiResponse(responseCode = "401", description = "You are trying to add a doc while not authenticated")
    })
    @PostMapping("/{id}/add_to_favourites")
    public ResponseEntity<?> addDocToFavouritesById(@PathVariable Long id) {
        try {
            docService.addDocToFavouritesById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No document with such id!");
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Remove doc from favourites", description = "This endpoint lets you remove doc from favourites, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully removed a document from favourites"),
            @ApiResponse(responseCode = "401", description = "You are trying to remove a doc while not authenticated")
    })
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
