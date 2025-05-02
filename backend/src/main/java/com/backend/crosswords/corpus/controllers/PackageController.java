package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.corpus.dto.CreatePackageDTO;
import com.backend.crosswords.corpus.dto.DocDTO;
import com.backend.crosswords.corpus.dto.DocsDTO;
import com.backend.crosswords.corpus.dto.FoldersDTO;
import com.backend.crosswords.corpus.models.Package;
import com.backend.crosswords.corpus.services.DocService;
import com.backend.crosswords.corpus.services.PackageService;
import com.backend.crosswords.corpus.services.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/packages")
@Tag(name = "Package controller", description = "Controller for all operations with packages")
public class PackageController {

    private final PackageService packageService;
    private final DocService docService;
    private final PdfService pdfService;

    public PackageController(PackageService packageService, DocService docService, PdfService pdfService) {
        this.packageService = packageService;
        this.docService = docService;
        this.pdfService = pdfService;
    }

    @Operation(summary = "Create a package", description = "This is a simple creating package endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You have successfully created a new package for a certain user"),
            @ApiResponse(responseCode = "400", description = "You are trying to create a package with already existing name for this user or the name for a new package is too long"),
            @ApiResponse(responseCode = "401", description = "You are trying to create a package while not authenticated")
    })
    @PostMapping("/create")
    public ResponseEntity<?> createPackage(@RequestBody CreatePackageDTO createPackageDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            packageService.createPackage(createPackageDTO.getName(), crosswordUserDetails.getUser());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Delete package by name", description = "This endpoint lets you delete a package for a certain user by specified package's name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You have successfully deleted this package for a certain user"),
            @ApiResponse(responseCode = "401", description = "You are trying to delete a package while not authenticated"),
            @ApiResponse(responseCode = "400", description = "You are trying to delete a package, that can't be deleted for this user (ex: favourites)"),
            @ApiResponse(responseCode = "404", description = "This user doesn't have packages with such name")
    })
    @DeleteMapping("/{name}")
    public ResponseEntity<?> deletePackage(@PathVariable String name) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            packageService.deletePackageByName(name, crosswordUserDetails.getUser());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This user doesn't have packages with such name!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Get user's packages", description = "This endpoint lets you get all packages, that a certain user have")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You have get all user's packages", content = @Content(schema = @Schema(implementation = FoldersDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get all user's packages while not authenticated"),
    })
    @GetMapping
    public ResponseEntity<?> getPackagesForUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        List<Package> usersPackages = packageService.getPackagesForUser(crosswordUserDetails.getUser());
        List<String> usersPackagesNames = new ArrayList<>();
        for (var usersPackage : usersPackages) {
            usersPackagesNames.add(usersPackage.getId().getName());
        }
        return ResponseEntity.ok(new FoldersDTO(usersPackagesNames));
    }
    @GetMapping("/{name}/pdf")
    public ResponseEntity<?> getPackagePDF(@PathVariable String name) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            var docs = packageService.getDocsFromPackage(name, crosswordUserDetails.getUser());
            List<DocDTO> docDTOs = docService.transformDocsIntoDTO(docs, false);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents_export.pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfService.generateDocListPdf(docDTOs));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while creating a pdf!");
        }
    }
    @Operation(summary = "Get all user's package's docs", description = "This endpoint lets you get all documents, from a certain user and a certain package")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You have get all user's package's docs", content = @Content(schema = @Schema(implementation = FoldersDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to You have get all user's package's docs while not authenticated"),
            @ApiResponse(responseCode = "404", description = "This user doesn't have packages with such name")
    })
    @GetMapping("/{name}/docs")
    public ResponseEntity<?> getPackagesDocsByName(@PathVariable String name, @RequestParam(name = "include_annotations", required = false) Boolean includeAnnotations) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            var docs = packageService.getDocsFromPackage(name, crosswordUserDetails.getUser());
            List<DocDTO> docDTOs = docService.transformDocsIntoDTO(docs, includeAnnotations);
            return ResponseEntity.ok().body(new DocsDTO(docDTOs));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
