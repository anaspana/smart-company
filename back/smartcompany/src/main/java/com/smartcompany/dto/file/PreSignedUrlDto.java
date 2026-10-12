package com.smartcompany.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Presigned S3 URL za upload fajla")
@Data @AllArgsConstructor
public class PreSignedUrlDto {
    @Schema(description = "Presigned PUT URL, važi 15 minuta") private URL url;
    @Schema(description = "S3 ključ, poslati ga nazad u create/update zahtevu") private String filename;
}