package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.models.Package;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

// TODO сделать ручку для проверки владения подпиской
// TODO сделать ручку для получения всех папок пользователя, включая отображение присутствия выбранного документа в папках
// TODO просто проверка настройки CI/CD

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
    public ResponseEntity<?> createDoc(@RequestBody CreateDocDTO createDocDTO) {
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
            @ApiResponse(responseCode = "404", description = "May appear dew to wrong id, while searching by id")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully deleted the document"),
            @ApiResponse(responseCode = "401", description = "You are trying to delete a document while unauthorized"),
            @ApiResponse(responseCode = "403", description = "You don't have enough rights to delete documents"),
            @ApiResponse(responseCode = "404", description = "There is no documents with such id")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocById(@PathVariable Long id) {
        try {
            docService.deleteDocById(id);
        } catch (NoSuchElementException e) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Rate doc by id", description = "This endpoint lets you rate a document, parameters can be null or numbers from 1 to 5, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully rated a document"),
            @ApiResponse(responseCode = "404", description = "There is no documents with such id")
    })
    @PatchMapping("/{id}/rate")
    public ResponseEntity<?> rateDocById(@PathVariable Long id, @RequestBody RateDocDTO rateDocDTO) {
        try {
            docService.rateDocById(id, rateDocDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There is no documents with such id!");
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Edit doc by id", description = "This endpoint lets you edit a document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully updated the document"),
            @ApiResponse(responseCode = "401", description = "You are trying to updated a document while unauthorized"),
            @ApiResponse(responseCode = "403", description = "You don't have enough rights to updated documents"),
            @ApiResponse(responseCode = "404", description = "There is no documents with such id")
    })
    @PutMapping("/{id}/edit")
    public ResponseEntity<?> updateDocById(@PathVariable Long id, @RequestBody EditDocDTO editDocDTO) {
        try {
            docService.editDocById(id, editDocDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    // Очистка индекса документов из ES, если предварительно не почистить доки из Postgres, то дальше возникнут ошибки
    @Operation(summary = "Delete index from ES", description = "This endpoint lets clean the documents' index from ES, if you do not clean the docs from Postgres at the same time, then errors may occur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully deleted the documents' index"),
            @ApiResponse(responseCode = "404", description = "You are trying to delete index, that doesn't exist (maybe it has been already deleted)"),
            @ApiResponse(responseCode = "500", description = "This error means, that something is wrong with ES itself, it has an index, but couldn't delete it")
    })
    @DeleteMapping("/ES/itself")
    public ResponseEntity<?> deleteDocumentsItself() {
        try {
            docService.deleteDocumentsItself();
        } catch (NoSuchElementException e) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RequestRejectedException e) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Add doc into the package", description = "This endpoint lets add document by its id into the user's package, specified by its name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully added a document into the package"),
            @ApiResponse(responseCode = "401", description = "You are trying to add a doc while not authenticated"),
            @ApiResponse(responseCode = "404", description = "This user doesn't have packages with such name or the document's id is incorrect")
    })
    @PostMapping("/{docId}/put_into/{packageName}")
    public ResponseEntity<?> addDocIntoPackage(@PathVariable Long docId, @PathVariable String packageName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            docService.addDocByIdIntoPackageByName(crosswordUserDetails.getUser(), docId, packageName);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @PostMapping("/{docId}/remove_from/{packageName}")
    public ResponseEntity<?> removeFromPackage(@PathVariable Long docId, @PathVariable String packageName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            docService.removeDocByIdFromPackageByName(crosswordUserDetails.getUser(), docId, packageName);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Add doc to favourites", description = "This endpoint lets you add doc to favourites, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully added a document to favourites"),
            @ApiResponse(responseCode = "401", description = "You are trying to add a doc while not authenticated"),
            @ApiResponse(responseCode = "404", description = "This user doesn't have packages with such name or the document's id is incorrect")
    })
    @PostMapping("/{id}/add_to_favourites")
    public ResponseEntity<?> addDocToFavouritesById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            docService.addDocByIdIntoPackageByName(crosswordUserDetails.getUser(), id, Package.favouritesName);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            docService.removeDocByIdFromPackageByName(crosswordUserDetails.getUser(), id, Package.favouritesName);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
