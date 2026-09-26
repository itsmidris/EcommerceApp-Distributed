package in.sellzon.catalog.category.controller;

import in.sellzon.catalog.category.dto.response.CategoryImportPreviewDTO;
import in.sellzon.catalog.category.service.CategoryCsvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/categories/csv")
@RequiredArgsConstructor
public class CategoryCsvController {

    private final CategoryCsvService categoryCsvService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCategories() {
        byte[] file = categoryCsvService.exportCategories();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.parseMediaType("text/csv"));

        headers.setContentDisposition(ContentDisposition.attachment().filename("sellzon-categories.csv").build());

        return ResponseEntity.ok().headers(headers).body(file);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryImportPreviewDTO> previewImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(categoryCsvService.previewImport(file));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryImportPreviewDTO> importCategories(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(categoryCsvService.importCategories(file));
    }
}
