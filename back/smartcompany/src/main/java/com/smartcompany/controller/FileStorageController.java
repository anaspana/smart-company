package com.smartcompany.controller;

import com.smartcompany.dto.file.PreSignedUrlDto;
import com.smartcompany.service.user.FileStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;

@RestController
@RequestMapping("/files")
@Tag(name = "File Storage", description = "Presigned S3 URL-ovi za upload i pregled fajlova")
public class FileStorageController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * GET /api/files/presigned-url/put?filename=photo.jpg&folder=product-images
     *
     * Returns a presigned S3 PUT URL and the generated S3 key.
     * Frontend uses the URL to upload directly to S3, then sends the key
     * as part of the create/update request body.
     *
     * Folders in use:
     *   profile-images  - user profile pictures
     *   product-images  - product images
     *   factory-images  - factory images
     *   warehouse-images - warehouse images
     *   vehicle-images  - delivery vehicle images
     *   company-docs    - company registration documents (PDFs, images)
     */
    @Operation(
        summary = "Zatraži presigned URL za upload",
        description = "Javni endpoint (per SecurityConfig). Vraća presigned S3 PUT URL (važi 15 min) i generisani S3 ključ. " +
                      "Dozvoljeni folderi: profile-images, product-images, factory-images, warehouse-images, vehicle-images, company-docs."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Presigned URL generisan") })
    @SecurityRequirements
    @PutMapping("/presigned-url/put")    public ResponseEntity<PreSignedUrlDto> getUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam("folder") String folder) {
        PreSignedUrlDto dto = fileStorageService.generatePreSignedPutUrl(filename, folder);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/files/presigned-url/get?key=product-images/uuid.jpg
     *
     * Returns a presigned S3 GET URL valid for 15 minutes.
     * Frontend uses this URL to display or download the file.
     */
    @Operation(summary = "Zatraži presigned URL za preuzimanje/prikaz", description = "Javni endpoint (per SecurityConfig). Važi 15 minuta")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Presigned URL generisan") })
    @SecurityRequirements
    @GetMapping("/presigned-url/get")
    public ResponseEntity<String> getDownloadUrl(@RequestParam("key") String key) {
        URL url = fileStorageService.generatePreSignedGetUrl(key);
        return ResponseEntity.ok(url.toString());
    }
}