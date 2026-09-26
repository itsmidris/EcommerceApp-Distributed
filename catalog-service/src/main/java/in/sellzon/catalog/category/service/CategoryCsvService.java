package in.sellzon.catalog.category.service;

import in.sellzon.catalog.category.dto.response.CategoryImportPreviewDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryCsvService {

    byte[] exportCategories();
    CategoryImportPreviewDTO previewImport(MultipartFile file);
    CategoryImportPreviewDTO importCategories(MultipartFile file);
}
