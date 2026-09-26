package in.sellzon.catalog.category.service.impl;

import in.sellzon.catalog.category.dto.request.CreateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.request.UpdateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.response.CategoryImportChangeDTO;
import in.sellzon.catalog.category.dto.response.CategoryImportIssueDTO;
import in.sellzon.catalog.category.dto.response.CategoryImportPreviewDTO;
import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.category.repository.CategoryRepository;
import in.sellzon.catalog.category.service.CategoryCsvService;
import in.sellzon.catalog.category.service.CategoryService;
import in.sellzon.catalog.common.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryCsvServiceImpl implements CategoryCsvService {

    private static final List<String> HEADERS = List.of(
            "#", "Category ID", "Level 1", "Level 2",
            "Level 3", "Level 4", "Description", "Status"
    );

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCategories() {

        List<Category> categories = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == Status.ACTIVE)
                .sorted(Comparator
                        .comparingInt((Category c) -> c.getLevel().getLevelNumber())
                        .thenComparing(this::buildPathKey,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append("\r\n");

        int row = 1;

        for (Category category : categories) {

            List<String> path = buildPath(category);

            csv.append(row).append(",");
            csv.append(csvEscape(category.getId().toString())).append(",");
            csv.append(csvEscape(getLevel(path, 0))).append(",");
            csv.append(csvEscape(getLevel(path, 1))).append(",");
            csv.append(csvEscape(getLevel(path, 2))).append(",");
            csv.append(csvEscape(getLevel(path, 3))).append(",");

            csv.append(csvEscape(category.getDescription())).append(",");
            csv.append(csvEscape(category.getStatus().name())).append("\r\n");

            row++;
        }

        // UTF-8 BOM makes the CSV open correctly in Excel for non-English names.
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];

        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);

        return result;
    }

    @Override
    public CategoryImportPreviewDTO previewImport(MultipartFile file) {

        ParsedImport parsed = parseAndValidate(file);

        if (!parsed.errors().isEmpty()) {
            return previewWithErrors(parsed);
        }

        return buildPreview(parsed.rows());
    }

    @Override
    @Transactional
    public CategoryImportPreviewDTO importCategories(MultipartFile file) {

        ParsedImport parsed = parseAndValidate(file);

        if (!parsed.errors().isEmpty()) {
            return previewWithErrors(parsed);
        }

        CategoryImportPreviewDTO preview = buildPreview(parsed.rows());

        if (!preview.errors().isEmpty()) {
            return preview;
        }

        List<ImportRow> rows = new ArrayList<>(parsed.rows());

        rows.sort(
                Comparator.comparingInt(ImportRow::levelNumber)
                        .thenComparing(
                                ImportRow::pathKey,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        Map<String, Category> categoriesByPath = new HashMap<>();

        for (Category category : categoryRepository.findAll()) {
            if (category.getStatus() == Status.ACTIVE) {
                categoriesByPath.put(buildPathKey(category), category);
            }
        }

        for (ImportRow row : rows) {

            Category parent = null;

            if (row.levelNumber() > 1) {
                parent = categoriesByPath.get(row.parentPathKey());

                if (parent == null) {
                    throw new IllegalArgumentException(
                            "Unable to resolve parent category for CSV row "
                                    + row.rowNumber()
                    );
                }
            }

            Category existing = null;

            if (row.categoryId() != null) {
                existing = categoryRepository
                        .findByIdAndStatus(
                                row.categoryId(),
                                Status.ACTIVE
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Category ID does not exist or is inactive for CSV row "
                                                + row.rowNumber()
                                )
                        );
            }

            if (existing == null) {

                var created = categoryService.createCategory(
                        new CreateCategoryRequestDTO(
                                row.name(),
                                row.description(),
                                parent == null ? null : parent.getId()
                        )
                );

                Category saved = categoryRepository.findById(created.id())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Created category could not be reloaded"
                                )
                        );

                categoriesByPath.put(row.pathKey(), saved);

            } else {

                UUID parentId =
                        parent == null ? null : parent.getId();

                var updated = categoryService.updateCategory(
                        existing.getId(),
                        new UpdateCategoryRequestDTO(
                                row.name(),
                                row.description(),
                                parentId
                        )
                );

                Category saved = categoryRepository.findById(updated.id())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Updated category could not be reloaded"
                                )
                        );

                categoriesByPath.values().removeIf(
                        category -> category.getId().equals(saved.getId())
                );

                categoriesByPath.put(row.pathKey(), saved);
            }
        }

        return new CategoryImportPreviewDTO(
                preview.totalRows(),
                preview.createCount(),
                preview.updateCount(),
                preview.unchangedCount(),
                List.of(),
                preview.changes()
        );
    }

    private CategoryImportPreviewDTO previewWithErrors(
            ParsedImport parsed
    ) {
        return new CategoryImportPreviewDTO(
                parsed.rows().size(),
                0,
                0,
                0,
                parsed.errors(),
                List.of()
        );
    }

    private CategoryImportPreviewDTO buildPreview(
            List<ImportRow> rows
    ) {

        Map<UUID, Category> existingById =
                categoryRepository.findAll()
                        .stream()
                        .filter(c -> c.getStatus() == Status.ACTIVE)
                        .collect(Collectors.toMap(
                                Category::getId,
                                c -> c
                        ));

        Map<String, Category> existingByPath =
                existingById.values()
                        .stream()
                        .collect(Collectors.toMap(
                                this::buildPathKey,
                                c -> c,
                                (a, b) -> a
                        ));

        Set<String> csvPaths = rows.stream()
                .map(ImportRow::pathKey)
                .collect(Collectors.toSet());

        List<CategoryImportIssueDTO> errors = new ArrayList<>();
        List<CategoryImportChangeDTO> changes = new ArrayList<>();

        int creates = 0;
        int updates = 0;
        int unchanged = 0;

        for (ImportRow row : rows) {

            if (row.levelNumber() > 1
                    && !csvPaths.contains(row.parentPathKey())) {

                errors.add(new CategoryImportIssueDTO(
                        row.rowNumber(),
                        "Missing parent category in CSV: "
                                + row.parentPathKey()
                ));
                continue;
            }

            if (row.categoryId() == null) {

                Category samePath = existingByPath.get(row.pathKey());

                if (samePath == null) {

                    creates++;

                    changes.add(new CategoryImportChangeDTO(
                            row.rowNumber(),
                            "CREATE",
                            null,
                            row.pathDisplay(),
                            "New category"
                    ));

                } else {

                    errors.add(new CategoryImportIssueDTO(
                            row.rowNumber(),
                            "A category already exists at this path. "
                                    + "Use its Category ID to update it."
                    ));
                }

                continue;
            }

            Category existing = existingById.get(row.categoryId());

            if (existing == null) {

                errors.add(new CategoryImportIssueDTO(
                        row.rowNumber(),
                        "Category ID does not exist or is inactive"
                ));
                continue;
            }

            Category desiredParent = null;

            if (row.levelNumber() > 1) {
                desiredParent = existingByPath.get(row.parentPathKey());

                if (desiredParent == null) {

                    errors.add(new CategoryImportIssueDTO(
                            row.rowNumber(),
                            "Parent category does not currently exist: "
                                    + row.parentPathKey()
                    ));
                    continue;
                }
            }

            UUID currentParentId =
                    existing.getParent() == null
                            ? null
                            : existing.getParent().getId();

            UUID desiredParentId =
                    desiredParent == null
                            ? null
                            : desiredParent.getId();

            boolean same =
                    Objects.equals(
                            currentParentId,
                            desiredParentId
                    )
                            && existing.getName().equals(row.name())
                            && Objects.equals(
                            normalize(existing.getDescription()),
                            normalize(row.description())
                    );

            if (same) {

                unchanged++;

                changes.add(new CategoryImportChangeDTO(
                        row.rowNumber(),
                        "UNCHANGED",
                        existing.getId(),
                        row.pathDisplay(),
                        "No changes"
                ));

            } else {

                updates++;

                changes.add(new CategoryImportChangeDTO(
                        row.rowNumber(),
                        "UPDATE",
                        existing.getId(),
                        row.pathDisplay(),
                        "Existing category will be updated"
                ));
            }
        }

        return new CategoryImportPreviewDTO(
                rows.size(),
                creates,
                updates,
                unchanged,
                errors,
                changes
        );
    }

    private ParsedImport parseAndValidate(
            MultipartFile file
    ) {

        List<ImportRow> rows = new ArrayList<>();
        List<CategoryImportIssueDTO> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add(new CategoryImportIssueDTO(
                    0,
                    "Please upload a non-empty CSV file"
            ));
            return new ParsedImport(rows, errors);
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {

            errors.add(new CategoryImportIssueDTO(
                    0,
                    "Only .csv files are supported"
            ));
            return new ParsedImport(rows, errors);
        }

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            String header = reader.readLine();

            if (header == null) {
                errors.add(new CategoryImportIssueDTO(
                        1,
                        "CSV file is empty"
                ));
                return new ParsedImport(rows, errors);
            }

            header = removeBom(header);

            List<String> headers = parseCsvLine(header);
            validateHeaders(headers, errors);

            if (!errors.isEmpty()) {
                return new ParsedImport(rows, errors);
            }

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {

                    List<String> columns = parseCsvLine(line);

                    if (columns.size() != HEADERS.size()) {
                        throw new IllegalArgumentException(
                                "Expected " + HEADERS.size()
                                        + " columns but found "
                                        + columns.size()
                        );
                    }

                    rows.add(parseRow(columns, rowNumber));

                } catch (IllegalArgumentException exception) {

                    errors.add(new CategoryImportIssueDTO(
                            rowNumber,
                            exception.getMessage()
                    ));
                }
            }

            validateDuplicateRows(rows, errors);
            validateParentPaths(rows, errors);

        } catch (IOException exception) {

            errors.add(new CategoryImportIssueDTO(
                    0,
                    "Unable to read CSV file: "
                            + exception.getMessage()
            ));
        }

        return new ParsedImport(rows, errors);
    }

    private ImportRow parseRow(
            List<String> columns,
            int rowNumber
    ) {

        UUID categoryId = null;

        String id = columns.get(1).trim();

        if (!id.isBlank()) {
            try {
                categoryId = UUID.fromString(id);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Category ID must be a valid UUID"
                );
            }
        }

        List<String> levels = new ArrayList<>();

        for (int index = 2; index <= 5; index++) {
            levels.add(normalizeName(columns.get(index)));
        }

        int levelNumber = 0;
        boolean blankSeen = false;

        for (String level : levels) {

            if (level.isBlank()) {
                blankSeen = true;
            } else {

                if (blankSeen) {
                    throw new IllegalArgumentException(
                            "Category levels cannot skip an intermediate level"
                    );
                }

                levelNumber++;
            }
        }

        if (levelNumber < 1 || levelNumber > 4) {
            throw new IllegalArgumentException(
                    "A category must contain Level 1 through Level 4 without gaps"
            );
        }

        String name = levels.get(levelNumber - 1);

        if (name.length() > 150) {
            throw new IllegalArgumentException(
                    "Category name cannot exceed 150 characters"
            );
        }

        String description = columns.get(6).trim();

        if (description.length() > 500) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters"
            );
        }

        String pathKey = String.join(
                " > ",
                levels.subList(0, levelNumber)
        );

        String parentPathKey =
                levelNumber == 1
                        ? null
                        : String.join(
                        " > ",
                        levels.subList(0, levelNumber - 1)
                );

        return new ImportRow(
                rowNumber,
                categoryId,
                levelNumber,
                name,
                description.isBlank() ? null : description,
                pathKey,
                parentPathKey,
                pathKey
        );
    }

    private void validateDuplicateRows(
            List<ImportRow> rows,
            List<CategoryImportIssueDTO> errors
    ) {

        Set<String> paths = new HashSet<>();
        Set<UUID> ids = new HashSet<>();

        for (ImportRow row : rows) {

            if (!paths.add(row.pathKey())) {
                errors.add(new CategoryImportIssueDTO(
                        row.rowNumber(),
                        "Duplicate category path in CSV"
                ));
            }

            if (row.categoryId() != null
                    && !ids.add(row.categoryId())) {

                errors.add(new CategoryImportIssueDTO(
                        row.rowNumber(),
                        "Duplicate Category ID in CSV"
                ));
            }
        }
    }

    private void validateParentPaths(
            List<ImportRow> rows,
            List<CategoryImportIssueDTO> errors
    ) {

        Set<String> paths = rows.stream()
                .map(ImportRow::pathKey)
                .collect(Collectors.toSet());

        for (ImportRow row : rows) {

            if (row.levelNumber() > 1
                    && !paths.contains(row.parentPathKey())) {

                errors.add(new CategoryImportIssueDTO(
                        row.rowNumber(),
                        "Missing parent row for path: "
                                + row.parentPathKey()
                ));
            }
        }
    }

    private void validateHeaders(
            List<String> headers,
            List<CategoryImportIssueDTO> errors
    ) {

        if (headers.size() != HEADERS.size()) {

            errors.add(new CategoryImportIssueDTO(
                    1,
                    "Invalid CSV header. Expected "
                            + HEADERS.size()
                            + " columns."
            ));
            return;
        }

        for (int i = 0; i < HEADERS.size(); i++) {

            if (!HEADERS.get(i)
                    .equalsIgnoreCase(headers.get(i).trim())) {

                errors.add(new CategoryImportIssueDTO(
                        1,
                        "Invalid header at column "
                                + (i + 1)
                                + ". Expected '"
                                + HEADERS.get(i)
                                + "'"
                ));
            }
        }
    }

    private List<String> parseCsvLine(String line) {

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                if (quoted
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    current.append('"');
                    i++;

                } else {
                    quoted = !quoted;
                }

            } else if (c == ',' && !quoted) {

                values.add(current.toString());
                current.setLength(0);

            } else {

                current.append(c);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException(
                    "Unclosed quoted CSV value"
            );
        }

        values.add(current.toString());

        return values;
    }

    private String csvEscape(String value) {

        if (value == null) {
            return "";
        }

        boolean quote =
                value.contains(",")
                        || value.contains("\"")
                        || value.contains("\n")
                        || value.contains("\r");

        if (!quote) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private List<String> buildPath(Category category) {

        LinkedList<String> path = new LinkedList<>();

        Category current = category;

        while (current != null) {
            path.addFirst(current.getName());
            current = current.getParent();
        }

        return path;
    }

    private String buildPathKey(Category category) {
        return String.join(" > ", buildPath(category));
    }

    private String getLevel(List<String> path, int index) {
        return index < path.size() ? path.get(index) : "";
    }

    private String normalizeName(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String removeBom(String value) {

        if (value != null
                && !value.isEmpty()
                && value.charAt(0) == '\uFEFF') {

            return value.substring(1);
        }

        return value;
    }

    private record ParsedImport(
            List<ImportRow> rows,
            List<CategoryImportIssueDTO> errors
    ) {
    }

    private record ImportRow(
            int rowNumber,
            UUID categoryId,
            int levelNumber,
            String name,
            String description,
            String pathKey,
            String parentPathKey,
            String pathDisplay
    ) {
    }
}
