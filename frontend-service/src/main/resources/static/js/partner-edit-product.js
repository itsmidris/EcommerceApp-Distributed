let product = null;

let categoryAttributes = [];

let existingAttributeValues = [];

let productImages = [];

document.addEventListener(
    "DOMContentLoaded",
    async () => {

        if (!SellzonAPI.requireRole("SELLER")) {
            return;
        }


        const productId =
            getProductId();


        if (!productId) {

            showAlert(
                "Product ID is missing.",
                "danger"
            );

            return;
        }


        document
            .getElementById("editProductForm")
            .addEventListener(
                "submit",
                handleSubmit
            );


        document
            .getElementById("uploadImagesButton")
            ?.addEventListener(
                "click",
                uploadProductImages
            );

        document
            .getElementById("productImageInput")
            ?.addEventListener(
                "change",
                handleImageSelection
            );

        try {

            await loadProduct(productId);

        } catch (error) {

            console.error(error);

            showAlert(
                error.message ||
                "Unable to load product.",
                "danger"
            );

        }

    }
);


function getProductId() {

    const params =
        new URLSearchParams(
            window.location.search
        );

    return params.get("id");
}


/* =========================================================
   LOAD PRODUCT
   ========================================================= */

async function loadProduct(
    marketplaceProductId
) {

    const {
        response,
        data
    } = await SellzonAPI.json(
        `/api/seller/products/${encodeURIComponent(marketplaceProductId)}`,
        {
            method: "GET"
        }
    );


    if (!response.ok) {

        throw new Error(
            data?.message ||
            "Unable to load product."
        );
    }


    product = data;


    populateProduct(product);


    await loadCategoryAttributes(
        product.categoryId
    );


    await loadExistingAttributeValues(
        product.marketplaceProductId
    );

    await loadProductImages(
        product.marketplaceProductId
    );


    renderAttributes();


    document
        .getElementById("pageLoading")
        .classList.add("d-none");


    document
        .getElementById("editProductForm")
        .classList.remove("d-none");
}


/* =========================================================
   POPULATE BASIC INFORMATION
   ========================================================= */

function populateProduct(product) {

    document.getElementById(
        "marketplaceProductId"
    ).value =
        product.marketplaceProductId || "";


    document.getElementById(
        "sku"
    ).value =
        product.sku || "";


    document.getElementById(
        "brand"
    ).value =
        product.brand || "";


    document.getElementById(
        "categoryName"
    ).value =
        product.categoryName || "";


    document.getElementById(
        "title"
    ).value =
        product.title || "";


    document.getElementById(
        "description"
    ).value =
        product.description || "";


    document.getElementById(
        "mrp"
    ).value =
        product.mrp ?? "";


    document.getElementById(
        "sellingPrice"
    ).value =
        product.sellingPrice ?? "";


    document.getElementById(
        "stockQuantity"
    ).value =
        product.stockQuantity ?? "";
}


/* =========================================================
   CATEGORY ATTRIBUTES
   ========================================================= */

async function loadCategoryAttributes(
    categoryId
) {

    const {
        response,
        data
    } = await SellzonAPI.json(
        `/api/seller/categories/${encodeURIComponent(categoryId)}/attributes`,
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


    categoryAttributes =
        Array.isArray(data)
            ? data
            : [];


    document
        .getElementById(
            "attributesLoading"
        )
        .classList.add("d-none");
}


async function loadExistingAttributeValues(
    marketplaceProductId
) {

    const {
        response,
        data
    } = await SellzonAPI.json(
        `/api/seller/products/${encodeURIComponent(marketplaceProductId)}/attributes`,
        {
            method: "GET"
        }
    );


    if (!response.ok) {

        throw new Error(
            data?.message ||
            "Unable to load product attributes."
        );
    }


    existingAttributeValues =
        Array.isArray(data)
            ? data
            : [];
}


/* =========================================================
   RENDER ATTRIBUTES
   ========================================================= */

function renderAttributes() {

    const container =
        document.getElementById(
            "categoryAttributesContainer"
        );


    if (!categoryAttributes.length) {

        container.innerHTML = `
            <div class="text-secondary py-3">
                No category-specific attributes configured.
            </div>
        `;

        return;
    }


    container.innerHTML =
        categoryAttributes
            .map(attribute =>
                renderAttribute(attribute)
            )
            .join("");


    populateExistingValues();
}


function renderAttribute(attribute) {

    const id =
        escapeHtml(
            attribute.categoryAttributeId
        );


    const name =
        escapeHtml(
            attribute.attributeName
        );


    const type =
        attribute.attributeType;


    const required =
        attribute.required === true;


    const requiredMark =
        required
            ? `<span class="text-danger">*</span>`
            : "";


    const unit =
        attribute.unit
            ? escapeHtml(attribute.unit)
            : "";


    if (type === "DROPDOWN") {

        return `
            <div class="mb-4">

                <label class="form-label">
                    ${name}
                    ${requiredMark}
                </label>

                <select
                    class="form-select"
                    data-attribute-input
                    data-category-attribute-id="${id}"
                    data-attribute-type="DROPDOWN"
                >

                    <option value="">
                        Select ${name}
                    </option>

                    ${(attribute.options || [])
                        .map(option => `
                            <option
                                value="${escapeHtml(option.id)}"
                            >
                                ${escapeHtml(option.value)}
                            </option>
                        `)
                        .join("")
                    }

                </select>

                ${
                    unit
                        ? `
                            <div class="form-text">
                                ${unit}
                            </div>
                          `
                        : ""
                }

                <div class="form-text">
                    ${required ? "Required" : "Optional"}
                </div>

            </div>
        `;
    }


    if (type === "MULTI_SELECT") {

        return `
            <div class="mb-4">

                <label class="form-label">
                    ${name}
                    ${requiredMark}
                </label>

                <div class="border rounded p-3">

                    ${(attribute.options || [])
                        .map(option => `
                            <div class="form-check mb-2">

                                <input
                                    class="form-check-input"
                                    type="checkbox"
                                    value="${escapeHtml(option.id)}"
                                    data-attribute-option
                                    data-category-attribute-id="${id}"
                                >

                                <label class="form-check-label">
                                    ${escapeHtml(option.value)}
                                </label>

                            </div>
                        `)
                        .join("")
                    }

                </div>

                <div class="form-text">
                    ${required ? "Required" : "Optional"}
                </div>

            </div>
        `;
    }


    if (type === "BOOLEAN") {

        return `
            <div class="mb-4">

                <div class="form-check">

                    <input
                        class="form-check-input"
                        type="checkbox"
                        data-attribute-input
                        data-category-attribute-id="${id}"
                        data-attribute-type="BOOLEAN"
                    >

                    <label class="form-check-label">
                        ${name}
                        ${requiredMark}
                    </label>

                </div>

            </div>
        `;
    }


    let inputType = "text";

    let step = "";


    if (type === "NUMBER") {

        inputType = "number";
        step = 'step="1"';

    } else if (type === "DECIMAL") {

        inputType = "number";
        step = 'step="0.000001"';
    }


    return `
        <div class="mb-4">

            <label class="form-label">

                ${name}

                ${requiredMark}

            </label>


            <div class="input-group">

                <input
                    type="${inputType}"
                    class="form-control"
                    data-attribute-input
                    data-category-attribute-id="${id}"
                    data-attribute-type="${type}"
                    ${step}
                >

                ${
                    unit
                        ? `
                            <span class="input-group-text">
                                ${unit}
                            </span>
                          `
                        : ""
                }

            </div>


            <div class="form-text">

                ${required ? "Required" : "Optional"}

            </div>

        </div>
    `;
}


/* =========================================================
   POPULATE EXISTING ATTRIBUTE VALUES
   ========================================================= */

function populateExistingValues() {

    existingAttributeValues.forEach(
        value => {

            const categoryAttributeId =
                value.categoryAttributeId;


            const type =
                value.attributeType;


            if (type === "MULTI_SELECT") {

                const optionIds =
                    value.selectedOptionIds || [];


                optionIds.forEach(
                    optionId => {

                        const checkbox =
                            document.querySelector(
                                `[data-attribute-option][data-category-attribute-id="${categoryAttributeId}"][value="${optionId}"]`
                            );


                        if (checkbox) {

                            checkbox.checked = true;

                        }

                    }
                );


                return;
            }


            if (type === "DROPDOWN") {

                const select =
                    document.querySelector(
                        `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                    );


                if (select) {

                    select.value =
                        value.selectedOptionId || "";

                }


                return;
            }


            if (type === "BOOLEAN") {

                const checkbox =
                    document.querySelector(
                        `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                    );


                if (checkbox) {

                    checkbox.checked =
                        value.valueBoolean === true;
                }


                return;
            }


            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${categoryAttributeId}"]`
                );


            if (!input) {
                return;
            }


            if (type === "NUMBER") {

                input.value =
                    value.valueNumber ?? "";

            } else if (type === "DECIMAL") {

                input.value =
                    value.valueDecimal ?? "";

            } else {

                input.value =
                    value.valueText ?? "";
            }

        }
    );
}


/* =========================================================
   COLLECT ATTRIBUTE VALUES
   ========================================================= */

function collectAttributeValues() {

    return categoryAttributes.map(
        attribute => {

            const id =
                attribute.categoryAttributeId;


            const type =
                attribute.attributeType;


            if (type === "MULTI_SELECT") {

                const checked =
                    document.querySelectorAll(
                        `[data-attribute-option][data-category-attribute-id="${id}"]:checked`
                    );


                return {

                    categoryAttributeId: id,

                    optionIds:
                        Array.from(checked)
                            .map(input => input.value)

                };
            }


            if (type === "DROPDOWN") {

                const select =
                    document.querySelector(
                        `[data-attribute-input][data-category-attribute-id="${id}"]`
                    );


                return {

                    categoryAttributeId: id,

                    optionIds:
                        select?.value
                            ? [select.value]
                            : []

                };
            }


            if (type === "BOOLEAN") {

                const checkbox =
                    document.querySelector(
                        `[data-attribute-input][data-category-attribute-id="${id}"]`
                    );


                return {

                    categoryAttributeId: id,

                    value:
                        checkbox?.checked
                            ? "true"
                            : "false"

                };
            }


            const input =
                document.querySelector(
                    `[data-attribute-input][data-category-attribute-id="${id}"]`
                );


            return {

                categoryAttributeId: id,

                value:
                    input?.value.trim() || ""

            };
        }
    );
}


/* =========================================================
   VALIDATE BASIC PRODUCT
   ========================================================= */

function validateBasicProduct() {

    const title =
        document.getElementById(
            "title"
        ).value.trim();


    const sellingPrice =
        Number(
            document.getElementById(
                "sellingPrice"
            ).value
        );


    const stockQuantity =
        Number(
            document.getElementById(
                "stockQuantity"
            ).value
        );


    const mrp =
        Number(
            document.getElementById(
                "mrp"
            ).value
        );


    if (!title) {

        return {
            valid: false,
            message: "Product title is required."
        };
    }


    if (
        !Number.isFinite(sellingPrice) ||
        sellingPrice <= 0
    ) {

        return {
            valid: false,
            message:
                "Selling price must be greater than 0."
        };
    }


    if (
        Number.isFinite(mrp) &&
        sellingPrice > mrp
    ) {

        return {
            valid: false,
            message:
                "Selling price cannot be greater than MRP."
        };
    }


    if (
        !Number.isInteger(stockQuantity) ||
        stockQuantity < 0
    ) {

        return {
            valid: false,
            message:
                "Stock quantity must be a whole number greater than or equal to 0."
        };
    }


    return {
        valid: true
    };
}


/* =========================================================
   VALIDATE REQUIRED CATEGORY ATTRIBUTES
   ========================================================= */

function validateRequiredAttributes(
    values
) {

    for (const attribute of categoryAttributes) {

        if (!attribute.required) {
            continue;
        }


        const submitted =
            values.find(
                value =>
                    value.categoryAttributeId ===
                    attribute.categoryAttributeId
            );


        if (!submitted) {

            return {
                valid: false,
                message:
                    `${attribute.attributeName} is required.`
            };
        }


        const type =
            attribute.attributeType;


        /*
         * BOOLEAN
         *
         * false is a valid value.
         */

        if (type === "BOOLEAN") {
            continue;
        }


        const hasText =
            typeof submitted.value === "string" &&
            submitted.value.trim().length > 0;


        const hasOptions =
            Array.isArray(
                submitted.optionIds
            ) &&
            submitted.optionIds.length > 0;


        if (!hasText && !hasOptions) {

            return {
                valid: false,
                message:
                    `${attribute.attributeName} is required.`
            };
        }
    }


    return {
        valid: true
    };
}


/* =========================================================
   SUBMIT
   ========================================================= */

async function handleSubmit(event) {

    event.preventDefault();


    const basicValidation =
        validateBasicProduct();


    if (!basicValidation.valid) {

        showAlert(
            basicValidation.message,
            "danger"
        );

        return;
    }


    const attributeValues =
        collectAttributeValues();


    const attributeValidation =
        validateRequiredAttributes(
            attributeValues
        );


    if (!attributeValidation.valid) {

        showAlert(
            attributeValidation.message,
            "danger"
        );

        return;
    }


    setLoading(true);


    try {

        /*
         * STEP 1
         *
         * Update only:
         * title
         * description
         * sellingPrice
         * stockQuantity
         */

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/products/${encodeURIComponent(product.marketplaceProductId)}`,
            {
                method: "PUT",

                body: JSON.stringify({

                    title:
                        document
                            .getElementById("title")
                            .value
                            .trim(),

                    description:
                        document
                            .getElementById("description")
                            .value
                            .trim() || null,

                    sellingPrice:
                        Number(
                            document
                                .getElementById(
                                    "sellingPrice"
                                )
                                .value
                        ),

                    stockQuantity:
                        Number(
                            document
                                .getElementById(
                                    "stockQuantity"
                                )
                                .value
                        )

                })
            }
        );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to update product."
            );
        }


        /*
         * STEP 2
         *
         * Update category-specific attributes.
         */

        await saveAttributeValues(
            attributeValues
        );


        showAlert(
            "Product updated successfully.",
            "success"
        );


        setTimeout(
            () => {

                window.location.replace(
                    "/partner/products.html"
                );

            },
            700
        );


    } catch (error) {

        console.error(error);

        showAlert(
            error.message ||
            "Unable to update product.",
            "danger"
        );


    } finally {

        setLoading(false);
    }
}


/* =========================================================
   SAVE ATTRIBUTE VALUES
   ========================================================= */

async function saveAttributeValues(
    values
) {

    for (const value of values) {

        const existing =
            existingAttributeValues.find(
                item =>
                    item.categoryAttributeId ===
                    value.categoryAttributeId
            );


        const isEmpty =
            isAttributeValueEmpty(value);


        /*
         * Existing value was cleared.
         *
         * Delete it.
         */

        if (existing && isEmpty) {

            const {
                response,
                data
            } = await SellzonAPI.json(
                `/api/seller/products/${encodeURIComponent(product.marketplaceProductId)}/attributes/${existing.id}`,
                {
                    method: "DELETE"
                }
            );


            if (!response.ok) {

                throw new Error(
                    data?.message ||
                    `Unable to remove ${existing.attributeName || "attribute"}.`
                );
            }


            continue;
        }


        /*
         * Optional attribute was never filled.
         */

        if (!existing && isEmpty) {

            continue;
        }


        /*
         * Existing attribute → UPDATE
         */

        if (existing) {

            const {
                response,
                data
            } = await SellzonAPI.json(
                `/api/seller/products/${encodeURIComponent(product.marketplaceProductId)}/attributes/${existing.id}`,
                {
                    method: "PUT",

                    body: JSON.stringify(value)
                }
            );


            if (!response.ok) {

                throw new Error(
                    data?.message ||
                    "Unable to update product attribute."
                );
            }


            continue;
        }


        /*
         * New attribute → CREATE
         */

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/products/${encodeURIComponent(product.marketplaceProductId)}/attributes`,
            {
                method: "POST",

                body: JSON.stringify(value)
            }
        );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to save product attribute."
            );
        }
    }
}


/* =========================================================
   EMPTY ATTRIBUTE CHECK
   ========================================================= */

function isAttributeValueEmpty(
    value
) {

    if (
        Array.isArray(value.optionIds)
    ) {

        return value.optionIds.length === 0;
    }


    /*
     * Boolean false is NOT empty.
     */

    if (value.value === "false") {
        return false;
    }


    return !value.value ||
           !value.value.trim();
}


/* =========================================================
   LOADING
   ========================================================= */

function setLoading(
    loading
) {

    const button =
        document.getElementById(
            "saveProductButton"
        );


    if (!button) {
        return;
    }


    button.disabled = loading;


    button.innerHTML =
        loading
            ? `
                <span
                    class="spinner-border spinner-border-sm me-1"
                ></span>

                Saving...
              `
            : `
                <i class="bi bi-check-lg me-1"></i>

                Save Changes
              `;
}


/* =========================================================
   ALERT
   ========================================================= */

function showAlert(
    message,
    type
) {

    const alert =
        document.getElementById(
            "editProductAlert"
        );


    alert.className =
        `alert alert-${type}`;


    alert.textContent =
        message;


    alert.classList.remove(
        "d-none"
    );


    alert.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


/* =========================================================
   HTML ESCAPE
   ========================================================= */

function escapeHtml(
    value
) {

    return String(
        value ?? ""
    )
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

/* =========================================================
   PRODUCT IMAGES
   ========================================================= */

async function loadProductImages(marketplaceProductId) {

    const container =
        document.getElementById(
            "productImagesContainer"
        );

    const loading =
        document.getElementById(
            "imagesLoading"
        );

    const empty =
        document.getElementById(
            "imagesEmpty"
        );

    try {

        loading.classList.remove("d-none");

        container.classList.add("d-none");

        empty.classList.add("d-none");

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/products/${encodeURIComponent(
                marketplaceProductId
            )}/images`,
            {
                method: "GET"
            }
        );

        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to load product images."
            );
        }

        productImages =
            Array.isArray(data)
                ? data
                : [];

        renderProductImages();

    } catch (error) {

        console.error(
            "Unable to load product images:",
            error
        );

        showImageAlert(
            error.message ||
            "Unable to load product images.",
            "danger"
        );

    } finally {

        loading.classList.add("d-none");
    }
}

function renderProductImages() {

    const container =
        document.getElementById(
            "productImagesContainer"
        );

    const empty =
        document.getElementById(
            "imagesEmpty"
        );

    const count =
        document.getElementById(
            "imageCount"
        );


    count.textContent =
        `${productImages.length} / 10`;


    if (!productImages.length) {

        container.innerHTML = "";

        container.classList.add("d-none");

        empty.classList.remove("d-none");

        return;
    }


    empty.classList.add("d-none");

    container.classList.remove("d-none");


    container.innerHTML =
        productImages
            .map(image =>
                renderProductImage(image)
            )
            .join("");
}

function renderProductImage(image) {

    const imageId =
        escapeHtml(image.id);

    const imageUrl =
        escapeHtml(SellzonAPI.catalogAssetUrl(image.imageUrl));

    const fileName =
        escapeHtml(
            image.originalFileName || "Product image"
        );

    const primary =
        image.primaryImage === true;


    return `
        <div class="col-12 col-sm-6 col-md-4 col-lg-3">

            <div class="card h-100 shadow-sm">

                <div
                    class="position-relative bg-light"
                    style="height: 220px;"
                >

                    <img
                        src="${imageUrl}"
                        alt="${fileName}"
                        class="w-100 h-100"
                        style="object-fit: contain;"
                        loading="lazy"
                    >


                    ${
                        primary
                            ? `
                                <span
                                    class="position-absolute top-0 start-0 m-2 badge text-bg-primary"
                                >
                                    <i class="bi bi-star-fill me-1"></i>
                                    Primary
                                </span>
                              `
                            : ""
                    }

                </div>


                <div class="card-body">

                    <div
                        class="small fw-semibold text-truncate"
                        title="${fileName}"
                    >
                        ${fileName}
                    </div>


                    <div class="small text-secondary mt-1">
                        ${formatFileSize(image.fileSize)}
                    </div>


                    <div class="d-flex gap-2 mt-3">

                        ${
                            primary
                                ? `
                                    <button
                                        type="button"
                                        class="btn btn-sm btn-light border flex-grow-1"
                                        disabled
                                    >
                                        <i class="bi bi-star-fill me-1"></i>
                                        Primary
                                    </button>
                                  `
                                : `
                                    <button
                                        type="button"
                                        class="btn btn-sm btn-outline-primary flex-grow-1"
                                        onclick="setPrimaryImage('${escapeHtml(image.id)}')"
                                    >
                                        <i class="bi bi-star me-1"></i>
                                        Set Primary
                                    </button>
                                  `
                        }


                        <button
                            type="button"
                            class="btn btn-sm btn-outline-danger"
                            title="Delete image"
                            onclick="deleteProductImage('${escapeHtml(image.id)}')"
                        >

                            <i class="bi bi-trash"></i>

                        </button>

                    </div>

                </div>

            </div>

        </div>
    `;
}

async function handleImageSelection(event) {

    const input = event.target;

    const files =
        Array.from(
            input.files || []
        );

    if (!files.length) {
        return;
    }

    if (
        productImages.length + files.length > 10
    ) {

        showImageAlert(
            `You can upload only ${10 - productImages.length} more image(s).`,
            "danger"
        );

        input.value = "";

        return;
    }

    for (const file of files) {

        const valid =
            await validateImageFile(file);

        if (!valid) {

            input.value = "";

            return;
        }
    }

    showImageAlert(
        `${files.length} image${files.length > 1 ? "s" : ""} selected and ready to upload.`,
        "success"
    );
}

async function uploadProductImages() {

    const input =
        document.getElementById(
            "productImageInput"
        );

    const button =
        document.getElementById(
            "uploadImagesButton"
        );

    const spinner =
        document.getElementById(
            "uploadImagesSpinner"
        );


    const files =
        Array.from(
            input.files || []
        );


    if (!files.length) {

        showImageAlert(
            "Please select at least one image.",
            "danger"
        );

        return;
    }


    if (
        productImages.length + files.length > 10
    ) {

        showImageAlert(
            `You can upload only ${10 - productImages.length} more image(s).`,
            "danger"
        );

        return;
    }


    for (const file of files) {

        const valid =
            await validateImageFile(file);

        if (!valid) {
            return;
        }
    }


    button.disabled = true;

    spinner.classList.remove("d-none");


    try {

        for (const file of files) {

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
                    product.marketplaceProductId
                )}/images`,
                {
                    method: "POST",
                    body: formData
                }
            );


            if (!response.ok) {

                throw new Error(
                    data?.message ||
                    `Unable to upload ${file.name}.`
                );
            }
        }


        input.value = "";


        await loadProductImages(
            product.marketplaceProductId
        );


        showImageAlert(
            "Images uploaded successfully.",
            "success"
        );


    } catch (error) {

        console.error(
            "Image upload failed:",
            error
        );

        showImageAlert(
            error.message ||
            "Unable to upload images.",
            "danger"
        );

    } finally {

        button.disabled = false;

        spinner.classList.add("d-none");
    }
}

async function validateImageFile(file) {

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

        const objectUrl =
            URL.createObjectURL(file);


        const image =
            new Image();


        image.onload = () => {

            const dimensions = {

                width:
                    image.naturalWidth,

                height:
                    image.naturalHeight
            };


            URL.revokeObjectURL(
                objectUrl
            );


            resolve(
                dimensions
            );
        };


        image.onerror = () => {

            URL.revokeObjectURL(
                objectUrl
            );


            resolve(null);
        };


        image.src =
            objectUrl;
    });
}

async function setPrimaryImage(imageId) {

    if (!imageId || !product) {
        return;
    }


    const confirmed =
        confirm(
            "Set this image as the primary product image?"
        );


    if (!confirmed) {
        return;
    }


    try {

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/products/${encodeURIComponent(
                product.marketplaceProductId
            )}/images/${encodeURIComponent(
                imageId
            )}/primary`,
            {
                method: "PATCH"
            }
        );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to set primary image."
            );
        }


        await loadProductImages(
            product.marketplaceProductId
        );


        showImageAlert(
            "Primary image updated successfully.",
            "success"
        );


    } catch (error) {

        console.error(error);

        showImageAlert(
            error.message ||
            "Unable to set primary image.",
            "danger"
        );
    }
}

async function deleteProductImage(imageId) {

    if (!imageId || !product) {
        return;
    }


    const image =
        productImages.find(
            item =>
                String(item.id) === String(imageId)
        );


    if (!image) {
        return;
    }


    const message =
        image.primaryImage
            ? "This is the primary image. If you delete it, another image will automatically become primary.\n\nContinue?"
            : "Are you sure you want to delete this image?";


    if (!confirm(message)) {
        return;
    }


    try {

        const {
            response,
            data
        } = await SellzonAPI.json(
            `/api/seller/products/${encodeURIComponent(
                product.marketplaceProductId
            )}/images/${encodeURIComponent(
                imageId
            )}`,
            {
                method: "DELETE"
            }
        );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to delete image."
            );
        }


        await loadProductImages(
            product.marketplaceProductId
        );


        showImageAlert(
            "Image deleted successfully.",
            "success"
        );


    } catch (error) {

        console.error(error);

        showImageAlert(
            error.message ||
            "Unable to delete image.",
            "danger"
        );
    }
}

function showImageAlert(message, type) {

    const alert =
        document.getElementById(
            "imageAlert"
        );


    alert.className =
        `alert alert-${type}`;


    alert.textContent =
        message;


    alert.classList.remove(
        "d-none"
    );
}

function formatFileSize(bytes) {

    const size =
        Number(bytes);


    if (!Number.isFinite(size) || size <= 0) {
        return "Unknown size";
    }


    if (size < 1024) {
        return `${size} B`;
    }


    if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)} KB`;
    }


    return `${(size / (1024 * 1024)).toFixed(2)} MB`;
}