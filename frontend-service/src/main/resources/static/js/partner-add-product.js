let selectedProductImages = [];

document.addEventListener("DOMContentLoaded", () => {

    if (!SellzonAPI.requireRole("SELLER")) {
        return;
    }

    loadLevelFourCategories();

    document.getElementById("categoryId")
        ?.addEventListener("change", onCategoryChange);

    document.getElementById("productForm")
        ?.addEventListener("submit", createProduct);

    document
        .getElementById("productImageInput")
        ?.addEventListener(
            "change",
            handleImageSelection
        );
});

const productState = {
    categoryId: null,
    categoryAttributes: []
};

async function loadLevelFourCategories() {

    const select = document.getElementById("categoryId");

    try {

        const {
            response,
            data
        } = await SellzonAPI.json(
            "/api/seller/categories/level-4",
            {
                method: "GET"
            }
        );

        if (!response.ok) {
            throw new Error(
                data?.message || "Unable to load categories."
            );
        }

        const categories = Array.isArray(data)
            ? data
            : [];

        select.innerHTML = categories.length
            ? `
                <option value="">
                    Select a Level 4 category
                </option>

                ${categories.map(category => `
                    <option value="${category.id}">
                        ${escapeHtml(category.name)}
                    </option>
                `).join("")}
              `
            : `
                <option value="">
                    No active Level 4 categories available
                </option>
              `;

    } catch (error) {

        select.innerHTML = `
            <option value="">
                Unable to load categories
            </option>
        `;

        alertBox(
            error.message,
            "danger"
        );
    }
}

async function onCategoryChange(event) {

    const categoryId = event.target.value;

    productState.categoryId = categoryId || null;

    productState.categoryAttributes = [];

    clearCategoryAttributeAlert();

    if (!categoryId) {

        renderCategoryPlaceholder();

        return;
    }

    await loadCategoryAttributes(categoryId);
}

async function loadCategoryAttributes(categoryId) {

    const container =
        document.getElementById(
            "categoryAttributesContainer"
        );

    const loading =
        document.getElementById(
            "categoryAttributesLoading"
        );

    try {

        loading.classList.remove("d-none");

        container.innerHTML = "";

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/categories/${categoryId}/attributes`,
            {
                method: "GET"
            }
        );

        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to load category attributes."
            );
        }

        const attributes = Array.isArray(data)
            ? data
            : [];

        productState.categoryAttributes = attributes;

        renderCategoryAttributes(attributes);

    } catch (error) {

        productState.categoryAttributes = [];

        container.innerHTML = "";

        showCategoryAttributeAlert(
            error.message
        );

    } finally {

        loading.classList.add("d-none");
    }
}

function renderCategoryAttributes(attributes) {

    const container =
        document.getElementById(
            "categoryAttributesContainer"
        );

    if (!attributes.length) {

        container.innerHTML = `
            <div class="product-schema-placeholder">

                <i class="bi bi-sliders2 fs-3"></i>

                <div class="mt-2 fw-semibold">
                    No attributes configured
                </div>

                <div class="small text-secondary">
                    No active attributes have been configured
                    for this category yet.
                </div>

            </div>
        `;

        return;
    }

    container.innerHTML = attributes
        .map(attribute => renderAttribute(attribute))
        .join("");
}

function renderAttribute(attribute) {

    const attributeId =
        escapeHtml(attribute.categoryAttributeId);

    const name =
        escapeHtml(attribute.attributeName);

    const code =
        escapeHtml(attribute.attributeCode);

    const type =
        attribute.attributeType;

    const unit =
        attribute.unit
            ? escapeHtml(attribute.unit)
            : "";

    const required =
        attribute.required === true;

    const requiredMark =
        required
            ? `<span class="text-danger ms-1">*</span>`
            : "";

    const requiredText =
        required
            ? `<div class="form-text">Required</div>`
            : `<div class="form-text">Optional</div>`;

    if (type === "TEXT") {

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <label class="form-label">
                    ${name}${requiredMark}
                </label>

                <input
                    type="text"
                    class="form-control"
                    data-attribute-input
                    data-category-attribute-id="${attributeId}"
                    data-attribute-type="${type}"
                    maxlength="5000"
                    ${required ? "required" : ""}
                >

                ${requiredText}

            </div>
        `;
    }

    if (type === "NUMBER") {

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <label class="form-label">
                    ${name}${requiredMark}
                </label>

                <div class="input-group">

                    <input
                        type="number"
                        class="form-control"
                        data-attribute-input
                        data-category-attribute-id="${attributeId}"
                        data-attribute-type="${type}"
                        step="1"
                        ${required ? "required" : ""}
                    >

                    ${
                        unit
                            ? `<span class="input-group-text">${unit}</span>`
                            : ""
                    }

                </div>

                ${requiredText}

            </div>
        `;
    }

    if (type === "DECIMAL") {

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <label class="form-label">
                    ${name}${requiredMark}
                </label>

                <div class="input-group">

                    <input
                        type="number"
                        class="form-control"
                        data-attribute-input
                        data-category-attribute-id="${attributeId}"
                        data-attribute-type="${type}"
                        step="0.01"
                        ${required ? "required" : ""}
                    >

                    ${
                        unit
                            ? `<span class="input-group-text">${unit}</span>`
                            : ""
                    }

                </div>

                ${requiredText}

            </div>
        `;
    }

    if (type === "BOOLEAN") {

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <div class="form-check form-switch">

                    <input
                        class="form-check-input"
                        type="checkbox"
                        role="switch"
                        id="attribute-${attributeId}"
                        data-attribute-input
                        data-category-attribute-id="${attributeId}"
                        data-attribute-type="${type}"
                    >

                    <label
                        class="form-check-label"
                        for="attribute-${attributeId}">

                        ${name}${requiredMark}

                    </label>

                </div>

                ${requiredText}

            </div>
        `;
    }

    if (type === "DROPDOWN") {

        const options =
            Array.isArray(attribute.options)
                ? attribute.options
                : [];

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <label class="form-label">
                    ${name}${requiredMark}
                </label>

                <select
                    class="form-select"
                    data-attribute-input
                    data-category-attribute-id="${attributeId}"
                    data-attribute-type="${type}"
                    ${required ? "required" : ""}>

                    <option value="">
                        Select ${name}
                    </option>

                    ${options.map(option => `
                        <option value="${escapeHtml(option.id)}">
                            ${escapeHtml(option.value)}
                        </option>
                    `).join("")}

                </select>

                ${requiredText}

            </div>
        `;
    }

    if (type === "MULTI_SELECT") {

        const options =
            Array.isArray(attribute.options)
                ? attribute.options
                : [];

        return `
            <div class="mb-4 category-attribute"
                 data-category-attribute-id="${attributeId}">

                <label class="form-label d-block">
                    ${name}${requiredMark}
                </label>

                <div class="border rounded p-3">

                    ${
                        options.length
                            ? options.map(option => `
                                <div class="form-check mb-2">

                                    <input
                                        class="form-check-input"
                                        type="checkbox"
                                        id="option-${escapeHtml(option.id)}"
                                        value="${escapeHtml(option.id)}"
                                        data-attribute-option
                                        data-category-attribute-id="${attributeId}"
                                        data-attribute-type="${type}"
                                    >

                                    <label
                                        class="form-check-label"
                                        for="option-${escapeHtml(option.id)}">

                                        ${escapeHtml(option.value)}

                                    </label>

                                </div>
                            `).join("")
                            : `
                                <div class="text-secondary small">
                                    No options configured.
                                </div>
                              `
                    }

                </div>

                ${requiredText}

            </div>
        `;
    }

    return `
        <div class="alert alert-warning">

            Unable to render attribute:
            <strong>${name}</strong>

            <div class="small mt-1">
                Unsupported attribute type: ${escapeHtml(type)}
            </div>

        </div>
    `;
}

function renderCategoryPlaceholder() {

    const container =
        document.getElementById(
            "categoryAttributesContainer"
        );

    container.innerHTML = `
        <div class="product-schema-placeholder">

            <i class="bi bi-sliders2 fs-3"></i>

            <div class="mt-2 fw-semibold">
                Select a Level 4 category
            </div>

            <div class="small text-secondary">
                Category-specific attributes will appear here automatically.
            </div>

        </div>
    `;
}

async function createProduct(e) {

    e.preventDefault();

    clearAlert();

    const categoryId =
        document.getElementById("categoryId").value;

    const sku =
        document.getElementById("sku").value.trim();

    const title =
        document.getElementById("title").value.trim();

    const description =
        document.getElementById("description").value.trim();

    const brand =
        document.getElementById("brand").value.trim();

    const mrp =
        document.getElementById("mrp").value;

    const sellingPrice =
        document.getElementById("sellingPrice").value;

    const stockQuantity =
        document.getElementById("stockQuantity").value;

    if (
        !categoryId ||
        !sku ||
        !title ||
        !brand ||
        mrp === "" ||
        sellingPrice === "" ||
        stockQuantity === ""
    ) {

        return alertBox(
            "Please fill in all required fields.",
            "danger"
        );
    }


    if (Number(mrp) <= 0 ||
        Number(sellingPrice) <= 0) {

        return alertBox(
            "MRP and selling price must be greater than 0.",
            "danger"
        );
    }


    if (Number(sellingPrice) > Number(mrp)) {

        return alertBox(
            "Selling price cannot be greater than MRP.",
            "danger"
        );
    }


    if (
        Number(stockQuantity) < 0 ||
        !Number.isInteger(Number(stockQuantity))
    ) {

        return alertBox(
            "Stock quantity must be a whole number greater than or equal to 0.",
            "danger"
        );
    }

    const requiredValidation = validateRequiredCategoryAttributes();

    if (!requiredValidation.valid) {
        return alertBox(
            requiredValidation.message,
            "danger"
        );
    }

    const attributeValues = collectCategoryAttributeValues();

    console.log("Collected category attributes:", attributeValues);

    setLoading(true);

    try {

        const {
            response,
            data
        } = await SellzonAPI.json(
            "/api/seller/products",
            {
                method: "POST",

                body: JSON.stringify({
                    categoryId,
                    sku,
                    title,
                    description: description || null,
                    brand,
                    mrp: Number(mrp),
                    sellingPrice: Number(sellingPrice),
                    attributes: attributeValues
                })
            }
        );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to create product."
            );
        }

        // Inventory Service is the source of truth for stock.
        // Catalog is created with stock = 0 and stock is initialized here.
        const inventoryResult = await SellzonAPI.json(
            `/api/seller/inventory/${encodeURIComponent(data.marketplaceProductId)}`,
            {
                method: "PATCH",
                body: JSON.stringify({
                    operation: "SET",
                    quantity: Number(stockQuantity),
                    type: "RESTOCK",
                    reason: "Initial inventory"
                })
            }
        );

        if (!inventoryResult.response.ok) {
            throw new Error(
                inventoryResult.data?.message ||
                "Product was created, but inventory initialization failed. The product remains out of stock."
            );
        }

        const imageUploadResult =
            await uploadSelectedProductImages(
                data.marketplaceProductId
            );

        if (!imageUploadResult.success) {

            alertBox(
                `Product created successfully, but these images could not be uploaded: ${imageUploadResult.failedFiles.join(", ")}. You can upload them from Edit Product.`,
                "warning"
            );

            return;
        }

        alertBox(
            `Product created successfully. Marketplace Product ID: ${data.marketplaceProductId}`,
            "success"
        );

        setTimeout(() => {
            window.location.replace(
                "/partner/products.html"
            );
        }, 900);


    } catch (error) {

        alertBox(
            error.message,
            "danger"
        );

    } finally {

        setLoading(false);
    }
}

function collectCategoryAttributeValues() {

    const values = [];

    const attributes =
        productState.categoryAttributes;


    attributes.forEach(attribute => {

        const categoryAttributeId =
            attribute.categoryAttributeId;

        const type =
            attribute.attributeType;

        if (type === "MULTI_SELECT") {

            const checked =
                document.querySelectorAll(
                    `[data-attribute-option][data-category-attribute-id="${categoryAttributeId}"]:checked`
                );

            values.push({
                categoryAttributeId,
                optionIds: Array.from(checked)
                    .map(input => input.value)
            });

            return;
        }

        if (type === "BOOLEAN") {

            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                );

            values.push({
                categoryAttributeId,
                value: input
                    ? String(input.checked)
                    : "false"
            });

            return;
        }

        if (type === "DROPDOWN") {

            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                );

            values.push({
                categoryAttributeId,
                optionIds:
                    input && input.value
                        ? [input.value]
                        : []
            });

            return;
        }


        const input =
            document.querySelector(
                `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
            );

        values.push({
            categoryAttributeId,
            value: input
                ? input.value.trim()
                : ""
        });

    });


    return values;
}

function setLoading(value) {

    const button =
        document.getElementById(
            "createProductBtn"
        );

    const spinner =
        document.getElementById(
            "createProductSpinner"
        );

    button.disabled = value;

    spinner.classList.toggle(
        "d-none",
        !value
    );
}

function alertBox(message, type) {

    const alert =
        document.getElementById(
            "productFormAlert"
        );

    alert.className =
        `alert alert-${type}`;

    alert.textContent =
        message;
}


function clearAlert() {

    const alert =
        document.getElementById(
            "productFormAlert"
        );

    alert.className =
        "alert d-none";

    alert.textContent =
        "";
}


function showCategoryAttributeAlert(message) {

    const alert =
        document.getElementById(
            "categoryAttributesAlert"
        );

    alert.className =
        "alert alert-danger";

    alert.textContent =
        message;
}


function clearCategoryAttributeAlert() {

    const alert =
        document.getElementById(
            "categoryAttributesAlert"
        );

    alert.className =
        "alert alert-danger d-none";

    alert.textContent =
        "";
}

function escapeHtml(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function validateRequiredCategoryAttributes() {

    const attributes =
        productState.categoryAttributes;

    for (const attribute of attributes) {

        if (!attribute.required) {
            continue;
        }

        const categoryAttributeId = attribute.categoryAttributeId;

        const type = attribute.attributeType;


        if (type === "MULTI_SELECT") {

            const checked =
                document.querySelectorAll(
                    `[data-attribute-option][data-category-attribute-id="${categoryAttributeId}"]:checked`
                );

            if (checked.length === 0) {

                return {
                    valid: false,
                    message:
                        `${attribute.attributeName} is required.`
                };
            }

            continue;
        }

        if (type === "BOOLEAN") {

            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                );

            if (!input) {

                return {
                    valid: false,
                    message:
                        `${attribute.attributeName} is required.`
                };
            }

            continue;
        }

        if (type === "DROPDOWN") {

            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                );

            if (!input || !input.value) {

                return {
                    valid: false,
                    message:
                        `${attribute.attributeName} is required.`
                };
            }

            continue;
        }

        const input =
            document.querySelector(
                `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
            );

        if (!input || !input.value.trim()) {

            return {
                valid: false,
                message:
                    `${attribute.attributeName} is required.`
            };
        }
    }

    return {
        valid: true,
        message: ""
    };
}

/* =========================================================
   PRODUCT IMAGE SELECTION
   ========================================================= */

async function handleImageSelection(event) {

    const files =
        Array.from(event.target.files || []);

    if (!files.length) {
        return;
    }

    const remainingSlots =
        10 - selectedProductImages.length;

    if (files.length > remainingSlots) {

        showImageAlert(
            `You can select only ${remainingSlots} more image(s).`,
            "danger"
        );

        event.target.value = "";

        return;
    }

    for (const file of files) {

        const valid =
            await validateSelectedImage(file);

        if (!valid) {
            event.target.value = "";
            return;
        }
    }

    selectedProductImages.push(...files);

    event.target.value = "";

    renderSelectedImages();
}

async function validateSelectedImage(file) {

    const allowedTypes = [
        "image/jpeg",
        "image/png",
        "image/webp"
    ];

    if (!allowedTypes.includes(file.type)) {

        showImageAlert(
            `${file.name}: Only JPG, PNG and WebP images are allowed.`,
            "danger"
        );

        return false;
    }

    const maxSize =
        5 * 1024 * 1024;

    if (file.size > maxSize) {

        showImageAlert(
            `${file.name}: Image size cannot exceed 5 MB.`,
            "danger"
        );

        return false;
    }

    const dimensions = await getImageDimensions(file);

        if (!dimensions) {

            showImageAlert(
                `${file.name}: Unable to read image dimensions. Please upload a valid image.`,
                "danger"
            );

            return false;
        }

        const {
            width,
            height
        } = dimensions;

        if (width !== height) {

            showImageAlert(
                `${file.name}: Image must be square. Required aspect ratio is 1:1.`,
                "danger"
            );

            return false;
        }

        if (width < 400 || height < 400) {

            showImageAlert(
                `${file.name}: Image dimensions must be at least 400x400 pixels.`,
                "danger"
            );

            return false;
        }

        if (width > 1600 || height > 1600) {

            showImageAlert(
                `${file.name}: Image dimensions cannot exceed 1600x1600 pixels.`,
                "danger"
            );

            return false;
        }

    return true;
}

function getImageDimensions(file) {

    return new Promise(resolve => {

        const objectUrl = URL.createObjectURL(file);

        const image = new Image();

        image.onload = () => {

            const dimensions = {
                width: image.naturalWidth,
                height: image.naturalHeight
            };

            URL.revokeObjectURL(objectUrl);

            resolve(dimensions);
        };

        image.onerror = () => {

            URL.revokeObjectURL(objectUrl);

            resolve(null);
        };

        image.src = objectUrl;
    });
}

function renderSelectedImages() {

    const container =
        document.getElementById(
            "selectedImagesContainer"
        );

    const count =
        document.getElementById(
            "selectedImageCount"
        );

    count.textContent =
        `${selectedProductImages.length} / 10`;

    if (!selectedProductImages.length) {

        container.innerHTML = "";

        return;
    }

    container.innerHTML =
        selectedProductImages
            .map(
                (file, index) =>
                    renderSelectedImage(
                        file,
                        index
                    )
            )
            .join("");
}

function renderSelectedImage(
    file,
    index
) {

    const objectUrl =
        URL.createObjectURL(file);

    return `
        <div class="col-6 col-md-4 col-lg-3">

            <div class="card h-100">

                <div
                        class="position-relative bg-light"
                        style="height:180px;"
                >

                    <img
                            src="${objectUrl}"
                            alt="${escapeHtml(file.name)}"
                            class="w-100 h-100"
                            style="object-fit:contain;"
                    >

                    ${
                        index === 0
                            ? `
                                <span
                                    class="position-absolute top-0 start-0 m-2 badge text-bg-primary"
                                >
                                    Primary
                                </span>
                              `
                            : ""
                    }

                </div>


                <div class="card-body p-2">

                    <div
                            class="small fw-semibold text-truncate"
                            title="${escapeHtml(file.name)}"
                    >
                        ${escapeHtml(file.name)}
                    </div>

                    <div class="small text-secondary">
                        ${formatImageSize(file.size)}
                    </div>

                    <button
                            type="button"
                            class="btn btn-sm btn-outline-danger w-100 mt-2"
                            onclick="removeSelectedImage(${index})"
                    >
                        <i class="bi bi-trash me-1"></i>
                        Remove
                    </button>

                </div>

            </div>

        </div>
    `;
}

function removeSelectedImage(index) {

    if (
        index < 0 ||
        index >= selectedProductImages.length
    ) {
        return;
    }

    selectedProductImages.splice(
        index,
        1
    );

    renderSelectedImages();
}

function formatImageSize(bytes) {

    if (bytes < 1024) {
        return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

async function uploadSelectedProductImages(
    marketplaceProductId
) {

    if (
        !marketplaceProductId ||
        !selectedProductImages.length
    ) {
        return {
            success: true,
            failedFiles: []
        };
    }

    const button =
        document.getElementById(
            "createProductBtn"
        );

    button.disabled = true;

    const failedFiles = [];

    for (
        let index = 0;
        index < selectedProductImages.length;
        index++
    ) {

        const file =
            selectedProductImages[index];

        button.innerHTML = `
            <span
                class="spinner-border spinner-border-sm me-1"
            ></span>

            Uploading image ${index + 1}
            of ${selectedProductImages.length}...
        `;

        try {

            /*
             * Validate again immediately before upload.
             * Backend also validates independently.
             */

            const valid =
                await validateSelectedImage(file);

            if (!valid) {

                failedFiles.push(
                    file.name
                );

                continue;
            }

            const formData =
                new FormData();

            formData.append(
                "file",
                file
            );

            const {
                response,
                data
            } = await SellzonAPI.json(
                `/api/seller/products/${encodeURIComponent(
                    marketplaceProductId
                )}/images`,
                {
                    method: "POST",
                    body: formData
                }
            );

            if (!response.ok) {

                failedFiles.push(
                    file.name
                );

                console.error(
                    `Unable to upload ${file.name}:`,
                    data
                );

                continue;
            }

        } catch (error) {

            failedFiles.push(
                file.name
            );

            console.error(
                `Unable to upload ${file.name}:`,
                error
            );
        }
    }

    button.disabled = false;

    if (failedFiles.length) {

        return {
            success: false,
            failedFiles
        };
    }

    return {
        success: true,
        failedFiles: []
    };
}