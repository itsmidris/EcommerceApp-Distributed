(() => {
    "use strict";

    const API_BASE = "/api/admin/categories";

    const state = {
        categories: [],
        listingCategories: [],
        editingCategoryId: null,
        deactivateCategoryId: null,

        // This is the important fix:
        // category IDs remain here while the tree is refreshed.
        expandedCategoryIds: new Set(),

        pendingCsvFile: null,
        csvPreview: null
    };

    const categoryModal = new bootstrap.Modal(
        document.getElementById("categoryModal")
    );

    const deactivateModal = new bootstrap.Modal(
        document.getElementById("deactivateModal")
    );

    const csvImportModal = new bootstrap.Modal(
        document.getElementById("csvImportModal")
    );

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        if (typeof SellzonAPI !== "undefined"
            && SellzonAPI.requireRole
            && !SellzonAPI.requireRole("ADMIN")) {
            return;
        }

        bindEvents();
        loadAdminUser();
        await loadCategories();
    }

    function bindEvents() {
        document.getElementById("createCategoryButton")
            .addEventListener("click", () => openCreateModal(null));

        document.getElementById("categoryForm")
            .addEventListener("submit", handleCategorySubmit);

        document.getElementById("refreshCategoriesButton")
            .addEventListener("click", () => loadCategories(true));

        document.getElementById("confirmDeactivateButton")
            .addEventListener("click", handleDeactivate);

        document.getElementById("logoutButton")
            .addEventListener("click", handleLogout);

        document.getElementById("exportCsvButton")
            .addEventListener("click", exportCsv);

        document.getElementById("importCsvButton")
            .addEventListener("click", openCsvModal);

        document.getElementById("csvFile")
            .addEventListener("change", handleCsvFileSelected);

        document.getElementById("previewCsvButton")
            .addEventListener("click", previewCsv);

        document.getElementById("applyCsvButton")
            .addEventListener("click", applyCsv);
    }

    function loadAdminUser() {
        const user = SellzonAPI.getUser?.();

        if (!user) {
            return;
        }

        const email = user.email || "Admin";
        document.getElementById("adminEmail").textContent = email;
        document.getElementById("adminAvatar").textContent =
            email.charAt(0).toUpperCase();
    }

    async function loadCategories(showLoading = false) {
        const tree = document.getElementById("categoryTree");

        if (showLoading) {
            tree.innerHTML = `
                <div class="tree-loading">
                    <div class="spinner-border spinner-border-sm"></div>
                    Refreshing categories...
                </div>
            `;
        }

        try {
            const [treeResult, level4Result] = await Promise.all([
                SellzonAPI.json(`${API_BASE}/tree`),
                SellzonAPI.json(`${API_BASE}/level-4`)
            ]);

            if (!treeResult.response.ok) {
                throw await buildApiError(
                    treeResult.response,
                    treeResult.data
                );
            }

            if (!level4Result.response.ok) {
                throw await buildApiError(
                    level4Result.response,
                    level4Result.data
                );
            }

            state.categories = treeResult.data || [];
            state.listingCategories = level4Result.data || [];

            updateSummary();
            renderTree();

        } catch (error) {
            tree.innerHTML = `
                <div class="tree-loading text-danger">
                    ${escapeHtml(error.message || "Unable to load categories")}
                </div>
            `;
        }
    }

    function updateSummary() {
        const total = countNodes(state.categories);

        document.getElementById("totalCategories").textContent = total;
        document.getElementById("topCategories").textContent =
            state.categories.length;
        document.getElementById("listingCategories").textContent =
            state.listingCategories.length;
    }

    function countNodes(nodes) {
        return (nodes || []).reduce(
            (count, node) => count + 1 + countNodes(node.children),
            0
        );
    }

    function renderTree() {
        const container = document.getElementById("categoryTree");

        if (!state.categories.length) {
            container.innerHTML = `
                <div class="tree-loading">
                    No categories found.
                </div>
            `;
            return;
        }

        container.innerHTML = state.categories
            .map(category => renderNode(category))
            .join("");
    }

    function renderNode(category) {
        const hasChildren =
            Array.isArray(category.children)
            && category.children.length > 0;

        const isExpanded =
            hasChildren && state.expandedCategoryIds.has(category.id);

        const levelNumber = getLevelNumber(category.level);

        return `
            <div class="tree-node" data-category-id="${category.id}">
                <div class="tree-row">
                    ${
                        hasChildren
                            ? `
                                <button class="tree-toggle"
                                        type="button"
                                        data-action="toggle"
                                        data-id="${category.id}"
                                        title="${isExpanded ? "Collapse" : "Expand"}">
                                    <i class="bi bi-chevron-${isExpanded ? "down" : "right"}"></i>
                                </button>
                            `
                            : `<span class="tree-spacer"></span>`
                    }

                    <span class="tree-name">
                        ${escapeHtml(category.name)}
                    </span>

                    <span class="tree-level">
                        L${levelNumber}
                    </span>

                    <div class="tree-actions">
                        ${
                            levelNumber < 4
                                ? `
                                    <button class="tree-action"
                                            type="button"
                                            data-action="add-child"
                                            data-id="${category.id}"
                                            title="Add child">
                                        <i class="bi bi-plus-lg"></i>
                                    </button>
                                `
                                : ""
                        }

                        <button class="tree-action"
                                type="button"
                                data-action="edit"
                                data-id="${category.id}"
                                title="Edit">
                            <i class="bi bi-pencil"></i>
                        </button>

                        <button class="tree-action"
                                type="button"
                                data-action="deactivate"
                                data-id="${category.id}"
                                title="Deactivate">
                            <i class="bi bi-slash-circle"></i>
                        </button>
                    </div>
                </div>

                ${
                    hasChildren
                        ? `
                            <div class="tree-children ${isExpanded ? "" : "collapsed"}">
                                ${category.children.map(renderNode).join("")}
                            </div>
                        `
                        : ""
                }
            </div>
        `;
    }

    document.getElementById("categoryTree")
        ?.addEventListener("click", handleTreeClick);

    function handleTreeClick(event) {
        const button = event.target.closest("[data-action]");

        if (!button) {
            return;
        }

        const action = button.dataset.action;
        const id = button.dataset.id;

        if (action === "toggle") {
            toggleCategory(id);
        }

        if (action === "add-child") {
            openCreateModal(id);
        }

        if (action === "edit") {
            openEditModal(id);
        }

        if (action === "deactivate") {
            openDeactivateModal(id);
        }
    }

    function toggleCategory(id) {
        if (state.expandedCategoryIds.has(id)) {
            state.expandedCategoryIds.delete(id);
        } else {
            state.expandedCategoryIds.add(id);
        }

        renderTree();
    }

    function openCreateModal(parentId) {
        state.editingCategoryId = null;

        document.getElementById("categoryModalTitle")
            .textContent = "Create Category";

        document.getElementById("categoryModalSubtitle")
            .textContent = parentId
                ? "Add a child category."
                : "Add a new Level 1 category.";

        document.getElementById("categoryName").value = "";
        document.getElementById("categoryDescription").value = "";

        populateParentDropdown(parentId);

        hideFormAlert();

        categoryModal.show();

        setTimeout(() =>
            document.getElementById("categoryName").focus(),
            250
        );
    }

    function openEditModal(categoryId) {
        const category = findCategory(state.categories, categoryId);

        if (!category) {
            return;
        }

        state.editingCategoryId = categoryId;

        document.getElementById("categoryModalTitle")
            .textContent = "Edit Category";

        document.getElementById("categoryModalSubtitle")
            .textContent = "Update category details or move it.";

        document.getElementById("categoryName").value =
            category.name || "";

        document.getElementById("categoryDescription").value =
            category.description || "";

        populateParentDropdown(
            category.parentId || "",
            categoryId
        );

        hideFormAlert();
        categoryModal.show();
    }

    function populateParentDropdown(selectedParentId, excludeId = null) {
        const select = document.getElementById("categoryParent");

        const options = [
            `<option value="">No Parent (Level 1)</option>`
        ];

        flattenCategories(state.categories).forEach(category => {
            if (category.id === excludeId) {
                return;
            }

            if (getLevelNumber(category.level) >= 4) {
                return;
            }

            options.push(`
                <option value="${category.id}"
                        ${category.id === selectedParentId ? "selected" : ""}>
                    ${escapeHtml(getCategoryPath(category))}
                </option>
            `);
        });

        select.innerHTML = options.join("");

        if (selectedParentId) {
            select.value = selectedParentId;
        }
    }

    async function handleCategorySubmit(event) {
        event.preventDefault();

        const saveButton =
            document.getElementById("saveCategoryButton");

        setButtonLoading(saveButton, true, "Saving...");

        const payload = {
            name: document.getElementById("categoryName").value.trim(),
            description:
                document.getElementById("categoryDescription").value.trim()
                || null,
            parentId:
                document.getElementById("categoryParent").value || null
        };

        try {
            const isEdit = Boolean(state.editingCategoryId);

            const url = isEdit
                ? `${API_BASE}/${state.editingCategoryId}`
                : API_BASE;

            const result = await SellzonAPI.json(url, {
                method: isEdit ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });

            if (!result.response.ok) {
                throw await buildApiError(result.response, result.data);
            }

            const savedCategory = result.data;

            /*
             * Important UX behavior:
             * if we create/edit a category, make sure the relevant
             * path is visible after refresh.
             */
            preservePathAsExpanded(savedCategory);

            categoryModal.hide();

            showPageAlert(
                isEdit
                    ? "Category updated successfully."
                    : "Category created successfully.",
                "success"
            );

            await loadCategories(false);

        } catch (error) {
            showFormAlert(error.message || "Unable to save category.");
        } finally {
            setButtonLoading(saveButton, false, "Save Category");
        }
    }

    function preservePathAsExpanded(category) {
        if (!category) {
            return;
        }

        if (category.parentId) {
            state.expandedCategoryIds.add(category.parentId);
        }

        /*
         * We cannot always know the complete ancestor chain from the
         * response, so existing expanded state is preserved. The
         * parent is additionally opened so a newly created child is
         * immediately visible.
         */
    }

    function openDeactivateModal(categoryId) {
        const category = findCategory(state.categories, categoryId);

        if (!category) {
            return;
        }

        state.deactivateCategoryId = categoryId;

        document.getElementById("deactivateMessage").textContent =
            `Deactivate "${category.name}"? This category will no longer `
            + `be available for active category selection.`;

        deactivateModal.show();
    }

    async function handleDeactivate() {
        if (!state.deactivateCategoryId) {
            return;
        }

        const button =
            document.getElementById("confirmDeactivateButton");

        setButtonLoading(button, true, "Deactivating...");

        try {
            const result = await SellzonAPI.json(
                `${API_BASE}/${state.deactivateCategoryId}/deactivate`,
                { method: "PATCH" }
            );

            if (!result.response.ok) {
                throw await buildApiError(
                    result.response,
                    result.data
                );
            }

            deactivateModal.hide();

            showPageAlert(
                "Category deactivated successfully.",
                "success"
            );

            await loadCategories(false);

        } catch (error) {
            showPageAlert(
                error.message || "Unable to deactivate category.",
                "error"
            );
        } finally {
            setButtonLoading(button, false, "Deactivate");
            state.deactivateCategoryId = null;
        }
    }

    async function exportCsv() {
        const button = document.getElementById("exportCsvButton");
        setButtonLoading(button, true, "Exporting...");

        try {
            const response = await SellzonAPI.request(
                `${API_BASE}/csv/export`,
                { method: "GET" }
            );

            if (!response.ok) {
                let message = "Unable to export categories.";

                try {
                    const data = await response.json();
                    message = data.message || message;
                } catch (_) {
                    // Keep fallback message.
                }

                throw new Error(message);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const anchor = document.createElement("a");

            anchor.href = url;
            anchor.download = "sellzon-categories.csv";
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();

            window.URL.revokeObjectURL(url);

            showPageAlert(
                "Category CSV exported successfully.",
                "success"
            );

        } catch (error) {
            showPageAlert(
                error.message || "Unable to export categories.",
                "error"
            );
        } finally {
            setButtonLoading(button, false, "Export CSV");
        }
    }

    function openCsvModal() {
        state.pendingCsvFile = null;
        state.csvPreview = null;

        document.getElementById("csvFile").value = "";
        document.getElementById("csvPreview").classList.add("d-none");
        document.getElementById("csvPreview").innerHTML = "";
        document.getElementById("applyCsvButton").disabled = true;

        csvImportModal.show();
    }

    function handleCsvFileSelected(event) {
        state.pendingCsvFile = event.target.files[0] || null;
        state.csvPreview = null;

        document.getElementById("csvPreview").classList.add("d-none");
        document.getElementById("csvPreview").innerHTML = "";
        document.getElementById("applyCsvButton").disabled = true;
    }

    async function previewCsv() {
        if (!state.pendingCsvFile) {
            showCsvPreviewMessage(
                "Please select a .csv file first.",
                true
            );
            return;
        }

        const button =
            document.getElementById("previewCsvButton");

        setButtonLoading(button, true, "Validating...");

        try {
            const formData = new FormData();
            formData.append("file", state.pendingCsvFile);

            const response = await SellzonAPI.request(
                `${API_BASE}/csv/preview`,
                {
                    method: "POST",
                    body: formData
                }
            );

            const data = await parseJsonOrError(response);

            if (!response.ok) {
                throw new Error(
                    data?.message || "Unable to preview CSV."
                );
            }

            state.csvPreview = data;
            renderCsvPreview(data);

        } catch (error) {
            showCsvPreviewMessage(
                error.message || "Unable to preview CSV.",
                true
            );
        } finally {
            setButtonLoading(button, false, "Preview Changes");
        }
    }

    async function applyCsv() {
        if (!state.pendingCsvFile || !state.csvPreview) {
            return;
        }

        if (Array.isArray(state.csvPreview.errors)
            && state.csvPreview.errors.length) {
            return;
        }

        const button = document.getElementById("applyCsvButton");
        setButtonLoading(button, true, "Applying...");

        try {
            const formData = new FormData();
            formData.append("file", state.pendingCsvFile);

            const response = await SellzonAPI.request(
                `${API_BASE}/csv/import`,
                {
                    method: "POST",
                    body: formData
                }
            );

            const data = await parseJsonOrError(response);

            if (!response.ok) {
                throw new Error(
                    data?.message || "Unable to import CSV."
                );
            }

            if (data.errors?.length) {
                renderCsvPreview(data);
                throw new Error(
                    "Import was not applied because validation errors remain."
                );
            }

            csvImportModal.hide();

            showPageAlert(
                `CSV import completed: ${data.createCount} created, `
                + `${data.updateCount} updated, `
                + `${data.unchangedCount} unchanged.`,
                "success"
            );

            /*
             * Do NOT clear expandedCategoryIds here.
             * Bulk import should preserve the admin's current tree view.
             */
            await loadCategories(false);

        } catch (error) {
            showPageAlert(
                error.message || "Unable to import CSV.",
                "error"
            );
        } finally {
            setButtonLoading(button, false, "Apply Changes");
        }
    }

    function renderCsvPreview(data) {
        const container = document.getElementById("csvPreview");
        container.classList.remove("d-none");

        const errors = Array.isArray(data.errors)
            ? data.errors
            : [];

        const changes = Array.isArray(data.changes)
            ? data.changes
            : [];

        container.innerHTML = `
            <div class="preview-summary">
                <div class="preview-stat">
                    <span>Create</span>
                    <strong>${data.createCount || 0}</strong>
                </div>
                <div class="preview-stat">
                    <span>Update</span>
                    <strong>${data.updateCount || 0}</strong>
                </div>
                <div class="preview-stat">
                    <span>Unchanged</span>
                    <strong>${data.unchangedCount || 0}</strong>
                </div>
            </div>

            ${
                errors.length
                    ? `
                        <div class="preview-error">
                            <strong>${errors.length} validation issue(s)</strong>
                            ${errors.map(error => `
                                <div>
                                    Row ${error.rowNumber || "-"}:
                                    ${escapeHtml(error.message)}
                                </div>
                            `).join("")}
                        </div>
                    `
                    : ""
            }

            ${
                changes.length
                    ? changes.map(change => `
                        <div class="preview-change">
                            <strong>${escapeHtml(change.action)}</strong>
                            <span>${escapeHtml(change.path || "")}</span>
                        </div>
                    `).join("")
                    : `
                        <div class="p-3 text-muted small">
                            No changes detected.
                        </div>
                    `
            }
        `;

        document.getElementById("applyCsvButton").disabled =
            errors.length > 0
            || (data.createCount + data.updateCount) === 0;
    }

    function showCsvPreviewMessage(message, error = false) {
        const container = document.getElementById("csvPreview");
        container.classList.remove("d-none");

        container.innerHTML = `
            <div class="${error ? "preview-error" : "p-3"}">
                ${escapeHtml(message)}
            </div>
        `;
    }

    async function parseJsonOrError(response) {
        const contentType =
            response.headers.get("content-type") || "";

        if (contentType.includes("application/json")) {
            return await response.json();
        }

        return null;
    }

    async function buildApiError(response, data) {
        if (data?.message) {
            return new Error(data.message);
        }

        if (data?.error) {
            return new Error(data.error);
        }

        return new Error(
            `Request failed with status ${response.status}.`
        );
    }

    async function handleLogout() {
        try {
            await SellzonAPI.logout();
        } catch (_) {
            SellzonAPI.clearAuth();
            window.location.href = "/login.html";
        }
    }

    function findCategory(nodes, id) {
        for (const node of nodes || []) {
            if (node.id === id) {
                return node;
            }

            const found = findCategory(node.children, id);

            if (found) {
                return found;
            }
        }

        return null;
    }

    function flattenCategories(nodes, result = []) {
        for (const node of nodes || []) {
            result.push(node);
            flattenCategories(node.children, result);
        }

        return result;
    }

    function getCategoryPath(category) {
        const names = [category.name];
        let parent = findCategory(state.categories, category.parentId);

        while (parent) {
            names.unshift(parent.name);
            parent = findCategory(state.categories, parent.parentId);
        }

        return names.join(" > ");
    }

    function getLevelNumber(level) {
        if (typeof level === "number") {
            return level;
        }

        const match = String(level || "").match(/(\d+)/);
        return match ? Number(match[1]) : 0;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function setButtonLoading(button, loading, text) {
        if (!button) {
            return;
        }

        if (loading) {
            button.dataset.originalText = button.innerHTML;
            button.disabled = true;
            button.innerHTML = `
                <span class="spinner-border spinner-border-sm me-1"></span>
                ${escapeHtml(text)}
            `;
        } else {
            button.disabled = false;
            button.innerHTML =
                button.dataset.originalText || escapeHtml(text);
        }
    }

    function showPageAlert(message, type) {
        const alert = document.getElementById("pageAlert");

        alert.className =
            `page-alert ${type === "success" ? "success" : "error"}`;

        alert.textContent = message;

        window.clearTimeout(showPageAlert.timer);

        showPageAlert.timer = window.setTimeout(() => {
            alert.classList.add("d-none");
        }, 4500);
    }

    function showFormAlert(message) {
        const alert = document.getElementById("categoryFormAlert");

        alert.className = "form-alert error";
        alert.textContent = message;
    }

    function hideFormAlert() {
        const alert = document.getElementById("categoryFormAlert");

        alert.className = "form-alert d-none";
        alert.textContent = "";
    }
})();
