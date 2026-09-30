(() => {
    "use strict";

    const CAT = "/api/admin/categories";
    const MAP = "/api/admin/category-attributes";
    const ATTR = "/api/admin/attributes";

    const state = {
        categories: [],
        attributes: [],
        currentMappings: [],
        categoryId: null,
        editingId: null,
        confirmAction: null
    };

    let mappingModal = null;
    let confirmModal = null;


    // =========================================================
    // INITIALIZATION
    // =========================================================

    function start() {
        init().catch(error => {
            console.error("Category Attributes initialization failed:", error);

            const alert = document.getElementById("pageAlert");

            if (alert) {
                alert.className = "page-alert error";
                alert.textContent =
                    error.message || "Unable to initialize page.";
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }


    async function init() {

        if (
            typeof SellzonAPI === "undefined"
            || !SellzonAPI.requireRole
        ) {
            throw new Error("Sellzon API is not loaded.");
        }

        if (!SellzonAPI.requireRole("ADMIN")) {
            return;
        }

        // Bootstrap modals
        const mappingModalElement =
            document.getElementById("mappingModal");

        const confirmModalElement =
            document.getElementById("confirmModal");

        if (mappingModalElement) {
            mappingModal =
                new bootstrap.Modal(mappingModalElement);
        }

        if (confirmModalElement) {
            confirmModal =
                new bootstrap.Modal(confirmModalElement);
        }

        bindEvents();

        loadUser();

        /*
         * Load categories and attributes together.
         */
        await Promise.all([
            loadCategories(),
            loadAttributes()
        ]);
    }


    // =========================================================
    // EVENT BINDINGS
    // =========================================================

    function bindEvents() {

        const categorySelect =
            document.getElementById("categorySelect");

        const refreshButton =
            document.getElementById("refreshMappingsButton");

        const mappingForm =
            document.getElementById("mappingForm");

        const assignButton =
            document.getElementById("addMappingButton");

        const logoutButton =
            document.getElementById("logoutButton");

        const confirmButton =
            document.getElementById("confirmButton");


        if (categorySelect) {
            categorySelect.addEventListener(
                "change",
                onCategoryChange
            );
        }

        if (refreshButton) {
            refreshButton.addEventListener(
                "click",
                loadMappings
            );
        }

        if (mappingForm) {
            mappingForm.addEventListener(
                "submit",
                saveMapping
            );
        }

        if (assignButton) {
            assignButton.addEventListener(
                "click",
                openCreate
            );
        }

        if (logoutButton) {
            logoutButton.addEventListener(
                "click",
                logout
            );
        }

        if (confirmButton) {
            confirmButton.addEventListener(
                "click",
                handleConfirmDeactivate
            );
        }
    }


    // =========================================================
    // USER
    // =========================================================

    function loadUser() {

        const user =
            SellzonAPI.getUser?.();

        if (!user) {
            return;
        }

        const email =
            user.email || "Admin";

        const emailElement =
            document.getElementById("adminEmail");

        const avatarElement =
            document.getElementById("adminAvatar");

        if (emailElement) {
            emailElement.textContent = email;
        }

        if (avatarElement) {
            avatarElement.textContent =
                email.charAt(0).toUpperCase();
        }
    }


    // =========================================================
    // CATEGORIES
    // =========================================================

    async function loadCategories() {

        try {

            const result =
                await SellzonAPI.json(
                    `${CAT}/level-4`
                );

            if (!result.response.ok) {
                throw await err(
                    result.response,
                    result.data
                );
            }

            state.categories =
                Array.isArray(result.data)
                    ? result.data
                    : [];

            renderCategories();

        } catch (error) {

            console.error(
                "Unable to load categories:",
                error
            );

            show(
                "Unable to load Level 4 categories: "
                + error.message,
                "error"
            );
        }
    }


    function renderCategories() {

        const select =
            document.getElementById(
                "categorySelect"
            );

        if (!select) {
            return;
        }

        select.innerHTML =
            `<option value="">
                Select a Level 4 category
            </option>`
            +
            state.categories
                .map(category => `
                    <option value="${escAttr(category.id)}">
                        ${esc(category.name)}
                    </option>
                `)
                .join("");


        /*
         * Restore currently selected category after
         * attributes/categories refresh.
         */
        if (state.categoryId) {

            select.value =
                state.categoryId;

            if (select.value === state.categoryId) {
                onCategoryChange();
            }
        }
    }


    async function onCategoryChange() {

        const select =
            document.getElementById(
                "categorySelect"
            );

        state.categoryId =
            select?.value || null;

        const category =
            state.categories.find(
                item => item.id === state.categoryId
            );

        const path =
            document.getElementById(
                "categoryPath"
            );

        if (path) {

            path.textContent =
                category
                    ? `Level 4 · ${category.name}`
                    : "";
        }

        state.currentMappings = [];

        if (state.categoryId) {

            await loadMappings();

        } else {

            const body =
                document.getElementById(
                    "mappingTableBody"
                );

            if (body) {

                body.innerHTML = `
                    <tr>
                        <td colspan="7">
                            <div class="table-loading">
                                Select a category.
                            </div>
                        </td>
                    </tr>
                `;
            }
        }
    }


    // =========================================================
    // ATTRIBUTES
    // =========================================================

    async function loadAttributes() {

        try {

            const result =
                await SellzonAPI.json(
                    ATTR
                );

            if (!result.response.ok) {
                throw await err(
                    result.response,
                    result.data
                );
            }

            state.attributes =
                Array.isArray(result.data)
                    ? result.data
                    : [];

        } catch (error) {

            console.error(
                "Unable to load attributes:",
                error
            );

            show(
                "Unable to load attributes: "
                + error.message,
                "error"
            );
        }
    }


    // =========================================================
    // MAPPINGS
    // =========================================================

    async function loadMappings() {

        if (!state.categoryId) {
            return;
        }

        const body =
            document.getElementById(
                "mappingTableBody"
            );

        if (body) {

            body.innerHTML = `
                <tr>
                    <td colspan="7">
                        <div class="table-loading">
                            <div class="spinner-border spinner-border-sm"></div>
                            Loading mappings...
                        </div>
                    </td>
                </tr>
            `;
        }

        try {

            const result =
                await SellzonAPI.json(
                    `${MAP}/category/${state.categoryId}`
                );

            if (!result.response.ok) {

                throw await err(
                    result.response,
                    result.data
                );
            }

            state.currentMappings =
                Array.isArray(result.data)
                    ? result.data
                    : [];

            renderMappings(
                state.currentMappings
            );

        } catch (error) {

            console.error(
                "Unable to load mappings:",
                error
            );

            if (body) {

                body.innerHTML = `
                    <tr>
                        <td colspan="7">
                            <div class="empty-state text-danger">
                                ${esc(error.message)}
                            </div>
                        </td>
                    </tr>
                `;
            }
        }
    }


    function renderMappings(items) {

        const body =
            document.getElementById(
                "mappingTableBody"
            );

        if (!body) {
            return;
        }


        /*
         * Keep the attribute selector synchronized
         * with currently assigned mappings.
         */
        refreshAttributeOptions();


        if (!items.length) {

            body.innerHTML = `
                <tr>
                    <td colspan="7">

                        <div class="empty-state">

                            <i class="bi bi-sliders"></i>

                            <strong>
                                No attributes assigned
                            </strong>

                            <span>
                                Assign reusable attributes
                                to this listing category.
                            </span>

                        </div>

                    </td>
                </tr>
            `;

            return;
        }


        body.innerHTML =
            items
                .map(item => `
                    <tr>

                        <td>
                            <div class="attribute-name-cell">

                                <strong>
                                    ${esc(item.attributeName)}
                                </strong>

                                <small>

                                    <code>
                                        ${esc(item.attributeCode)}
                                    </code>

                                    ${
                                        item.unit
                                            ? ` · ${esc(item.unit)}`
                                            : ""
                                    }

                                </small>

                            </div>
                        </td>

                        <td>
                            <span class="type-badge">
                                ${esc(item.attributeType)}
                            </span>
                        </td>

                        <td>
                            ${flag(item.required)}
                        </td>

                        <td>
                            ${flag(item.filterable)}
                        </td>

                        <td>
                            ${flag(item.searchable)}
                        </td>

                        <td>
                            ${flag(item.variantAttribute)}
                        </td>

                        <td class="text-end">

                            <button
                                class="btn btn-sm btn-light table-action"
                                type="button"
                                data-edit="${escAttr(item.id)}"
                                title="Edit configuration">

                                <i class="bi bi-pencil"></i>

                            </button>

                            <button
                                class="btn btn-sm btn-light table-action danger-action"
                                type="button"
                                data-delete="${escAttr(item.id)}"
                                data-name="${escAttr(item.attributeName)}"
                                title="Deactivate mapping">

                                <i class="bi bi-slash-circle"></i>

                            </button>

                        </td>

                    </tr>
                `)
                .join("");


        /*
         * Edit buttons
         */
        body
            .querySelectorAll("[data-edit]")
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () => {

                        const item =
                            items.find(
                                x =>
                                    x.id ===
                                    button.dataset.edit
                            );

                        openEdit(item);
                    }
                );
            });


        /*
         * Deactivate buttons
         */
        body
            .querySelectorAll("[data-delete]")
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () => {

                        confirmDeactivate(
                            button.dataset.delete,
                            button.dataset.name
                        );
                    }
                );
            });
    }


    function refreshAttributeOptions() {

        const select =
            document.getElementById(
                "attributeSelect"
            );

        if (!select) {
            return;
        }

        const mappedIds =
            new Set(
                state.currentMappings.map(
                    item => item.attributeId
                )
            );

        const available =
            state.attributes.filter(
                attribute =>
                    !mappedIds.has(attribute.id)
            );


        select.innerHTML =
            `<option value="">
                Select attribute
            </option>`
            +
            available
                .map(attribute => `
                    <option value="${escAttr(attribute.id)}">
                        ${esc(attribute.name)}
                        ·
                        ${esc(attribute.type)}
                    </option>
                `)
                .join("");
    }


    // =========================================================
    // CREATE MAPPING
    // =========================================================

    function openCreate() {

        if (!state.categoryId) {

            show(
                "Please select a Level 4 category first.",
                "error"
            );

            return;
        }

        state.editingId = null;


        document.getElementById(
            "mappingTitle"
        ).textContent =
            "Assign Attribute";


        document.getElementById(
            "saveMappingButton"
        ).textContent =
            "Assign Attribute";


        document.getElementById(
            "attributePickerWrap"
        ).classList.remove("d-none");


        /*
         * Reset form
         */
        document.getElementById(
            "mappingForm"
        ).reset();


        resetChecks();

        hideAlert("mappingAlert");


        /*
         * IMPORTANT:
         * Refresh the available attributes every time
         * the modal opens.
         *
         * Therefore after assigning DPI, the next modal
         * will still allow Connectivity, Colour, etc.
         */
        refreshAttributeOptions();


        if (mappingModal) {
            mappingModal.show();
        }
    }


    // =========================================================
    // EDIT MAPPING
    // =========================================================

    function openEdit(item) {

        if (!item) {
            return;
        }

        state.editingId =
            item.id;


        document.getElementById(
            "mappingTitle"
        ).textContent =
            `Configure ${item.attributeName}`;


        document.getElementById(
            "saveMappingButton"
        ).textContent =
            "Save Configuration";


        document.getElementById(
            "attributePickerWrap"
        ).classList.add("d-none");


        document.getElementById(
            "required"
        ).checked =
            item.required === true;


        document.getElementById(
            "filterable"
        ).checked =
            item.filterable === true;


        document.getElementById(
            "searchable"
        ).checked =
            item.searchable === true;


        document.getElementById(
            "variantAttribute"
        ).checked =
            item.variantAttribute === true;


        hideAlert("mappingAlert");


        if (mappingModal) {
            mappingModal.show();
        }
    }


    function resetChecks() {

        [
            "required",
            "filterable",
            "searchable",
            "variantAttribute"
        ]
            .forEach(id => {

                const element =
                    document.getElementById(id);

                if (element) {
                    element.checked = false;
                }
            });
    }


    // =========================================================
    // SAVE MAPPING
    // =========================================================

    async function saveMapping(event) {

        event.preventDefault();

        const button =
            document.getElementById(
                "saveMappingButton"
            );

        setLoading(
            button,
            true,
            "Saving..."
        );


        const payload = {

            required:
                document.getElementById(
                    "required"
                ).checked,

            filterable:
                document.getElementById(
                    "filterable"
                ).checked,

            searchable:
                document.getElementById(
                    "searchable"
                ).checked,

            variantAttribute:
                document.getElementById(
                    "variantAttribute"
                ).checked
        };


        try {

            const editing =
                Boolean(state.editingId);

            let result;


            if (editing) {

                result =
                    await SellzonAPI.json(
                        `${MAP}/${state.editingId}`,
                        {
                            method: "PUT",
                            body: JSON.stringify(payload)
                        }
                    );

            } else {

                const attributeId =
                    document.getElementById(
                        "attributeSelect"
                    ).value;


                if (!attributeId) {

                    throw new Error(
                        "Please select an attribute."
                    );
                }


                if (!state.categoryId) {

                    throw new Error(
                        "Please select a category."
                    );
                }


                result =
                    await SellzonAPI.json(
                        `${MAP}/category/${state.categoryId}`,
                        {
                            method: "POST",
                            body: JSON.stringify({
                                ...payload,
                                attributeId
                            })
                        }
                    );
            }


            if (!result.response.ok) {

                throw await err(
                    result.response,
                    result.data
                );
            }


            if (mappingModal) {
                mappingModal.hide();
            }


            show(
                editing
                    ? "Attribute configuration updated."
                    : "Attribute assigned successfully.",
                "success"
            );


            /*
             * Reload mappings.
             *
             * This updates currentMappings and therefore
             * removes the newly assigned attribute from
             * the next Assign Attribute dropdown.
             */
            await loadMappings();


        } catch (error) {

            showAlert(
                "mappingAlert",
                error.message ||
                "Unable to save mapping."
            );

        } finally {

            setLoading(
                button,
                false,
                state.editingId
                    ? "Save Configuration"
                    : "Assign Attribute"
            );
        }
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    function confirmDeactivate(id, name) {

        state.confirmAction =
            async () => {

                const response =
                    await SellzonAPI.request(
                        `${MAP}/${id}/deactivate`,
                        {
                            method: "PATCH"
                        }
                    );


                if (!response.ok) {

                    throw new Error(
                        `Request failed with status ${response.status}.`
                    );
                }


                show(
                    "Attribute mapping deactivated.",
                    "success"
                );


                await loadMappings();
            };


        const message =
            document.getElementById(
                "confirmMessage"
            );

        if (message) {

            message.textContent =
                `Deactivate "${name}" from this category?`;
        }


        if (confirmModal) {
            confirmModal.show();
        }
    }


    async function handleConfirmDeactivate() {

        const button =
            document.getElementById(
                "confirmButton"
            );


        setLoading(
            button,
            true,
            "Deactivating..."
        );


        try {

            if (state.confirmAction) {

                await state.confirmAction();
            }


            if (confirmModal) {
                confirmModal.hide();
            }

        } catch (error) {

            show(
                error.message ||
                "Unable to deactivate mapping.",
                "error"
            );

        } finally {

            state.confirmAction = null;

            setLoading(
                button,
                false,
                "Deactivate"
            );
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    async function logout() {

        try {

            await SellzonAPI.logout();

        } catch (error) {

            console.warn(
                "Logout request failed:",
                error
            );

            SellzonAPI.clearAuth();

        } finally {

            /*
             * Admin portal login path.
             */
            window.location.replace(
                "/admin/login.html"
            );
        }
    }


    // =========================================================
    // HELPERS
    // =========================================================

    async function err(response, data) {

        return new Error(
            data?.message
            ||
            data?.error
            ||
            `Request failed with status ${response.status}.`
        );
    }


    function show(message, type) {

        const element =
            document.getElementById(
                "pageAlert"
            );

        if (!element) {
            return;
        }


        element.className =
            `page-alert ${
                type === "success"
                    ? "success"
                    : "error"
            }`;


        element.textContent =
            message;


        clearTimeout(show.timer);


        show.timer =
            setTimeout(
                () => {

                    element.classList.add(
                        "d-none"
                    );

                },
                4500
            );
    }


    function showAlert(id, message) {

        const element =
            document.getElementById(id);

        if (!element) {
            return;
        }


        element.className =
            "form-alert error";


        element.textContent =
            message;
    }


    function hideAlert(id) {

        const element =
            document.getElementById(id);

        if (!element) {
            return;
        }


        element.className =
            "form-alert d-none";


        element.textContent =
            "";
    }


    function setLoading(button, loading, text) {

        if (!button) {
            return;
        }


        if (loading) {

            button.dataset.old =
                button.innerHTML;

            button.disabled =
                true;

            button.innerHTML = `
                <span class="spinner-border spinner-border-sm me-1"></span>
                ${esc(text)}
            `;

        } else {

            button.disabled =
                false;

            button.innerHTML =
                button.dataset.old
                ||
                esc(text);
        }
    }


    function flag(value) {

        return value
            ? `<span class="flag yes">
                    <i class="bi bi-check2"></i>
               </span>`
            : `<span class="flag no">—</span>`;
    }


    function esc(value) {

        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }


    function escAttr(value) {
        return esc(value);
    }

})();