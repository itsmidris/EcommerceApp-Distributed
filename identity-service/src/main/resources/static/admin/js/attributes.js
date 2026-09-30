(() => {
    "use strict";

    const API = "/api/admin/attributes";
    const state = { attributes: [], editingId: null, optionsAttributeId: null, confirmAction: null };

    let attributeModal, optionsModal, confirmModal;

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        if (SellzonAPI.requireRole && !SellzonAPI.requireRole("ADMIN")) return;

        attributeModal = new bootstrap.Modal(document.getElementById("attributeModal"));
        optionsModal = new bootstrap.Modal(document.getElementById("optionsModal"));
        confirmModal = new bootstrap.Modal(document.getElementById("confirmModal"));

        bindEvents();
        loadAdminUser();
        await loadAttributes();
    }

    function bindEvents() {
        document.getElementById("createAttributeButton").addEventListener("click", () => openAttributeModal());
        document.getElementById("attributeForm").addEventListener("submit", saveAttribute);
        document.getElementById("refreshAttributesButton").addEventListener("click", () => loadAttributes());
        document.getElementById("typeFilter").addEventListener("change", loadAttributes);
        document.getElementById("createOptionButton").addEventListener("click", createOption);
        document.getElementById("confirmActionButton").addEventListener("click", runConfirmAction);
        document.getElementById("logoutButton").addEventListener("click", handleLogout);
    }

    function loadAdminUser() {
        const user = SellzonAPI.getUser?.();
        if (!user) return;
        const email = user.email || "Admin";
        document.getElementById("adminEmail").textContent = email;
        document.getElementById("adminAvatar").textContent = email.charAt(0).toUpperCase();
    }

    async function loadAttributes() {
        const type = document.getElementById("typeFilter").value;
        const url = type ? `${API}/type/${type}` : API;
        const tbody = document.getElementById("attributeTableBody");
        tbody.innerHTML = `<tr><td colspan="6"><div class="table-loading"><div class="spinner-border spinner-border-sm"></div> Loading attributes...</div></td></tr>`;

        try {
            const result = await SellzonAPI.json(url);
            if (!result.response.ok) throw await apiError(result.response, result.data);
            state.attributes = result.data || [];
            renderAttributes();
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state text-danger">${esc(e.message)}</div></td></tr>`;
        }
    }

    function renderAttributes() {
        const tbody = document.getElementById("attributeTableBody");
        const all = state.attributes;
        const optionCount = all.filter(a => a.type === "DROPDOWN" || a.type === "MULTI_SELECT").length;

        document.getElementById("totalAttributes").textContent = all.length;
        document.getElementById("optionAttributes").textContent = optionCount;
        document.getElementById("simpleAttributes").textContent = all.length - optionCount;

        if (!all.length) {
            tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state"><i class="bi bi-sliders"></i><strong>No attributes found</strong><span>Create your first reusable attribute.</span></div></td></tr>`;
            return;
        }

        tbody.innerHTML = all.map(a => {
            const optionType = a.type === "DROPDOWN" || a.type === "MULTI_SELECT";
            return `<tr>
                <td><div class="attribute-name-cell"><strong>${esc(a.name)}</strong><small>${esc(a.description || "No description")}</small></div></td>
                <td><code>${esc(a.code)}</code></td>
                <td><span class="type-badge">${esc(a.type)}</span></td>
                <td>${esc(a.unit || "—")}</td>
                <td><span class="status-badge active">${esc(a.status || "ACTIVE")}</span></td>
                <td class="text-end">
                    ${optionType ? `<button class="btn btn-sm btn-light table-action" data-action="options" data-id="${a.id}" title="Manage options"><i class="bi bi-list-check"></i></button>` : ""}
                    <button class="btn btn-sm btn-light table-action" data-action="edit" data-id="${a.id}" title="Edit"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-light table-action danger-action" data-action="deactivate" data-id="${a.id}" title="Deactivate"><i class="bi bi-slash-circle"></i></button>
                </td>
            </tr>`;
        }).join("");

        tbody.querySelectorAll("[data-action]").forEach(b => b.addEventListener("click", handleTableAction));
    }

    async function handleTableAction(e) {
        const id = e.currentTarget.dataset.id;
        const action = e.currentTarget.dataset.action;
        const attribute = state.attributes.find(a => a.id === id);
        if (!attribute) return;

        if (action === "edit") openAttributeModal(attribute);
        if (action === "options") openOptionsModal(attribute);
        if (action === "deactivate") openConfirm(
            "Deactivate Attribute",
            `Deactivate "${attribute.name}"? It will no longer be available for new category mappings.`,
            () => deactivateAttribute(id)
        );
    }

    function openAttributeModal(attribute = null) {
        state.editingId = attribute?.id || null;
        document.getElementById("attributeModalTitle").textContent = attribute ? "Edit Attribute" : "Create Attribute";
        document.getElementById("attributeModalSubtitle").textContent = attribute ? "Update reusable attribute details." : "Create a reusable product attribute.";
        document.getElementById("attributeName").value = attribute?.name || "";
        document.getElementById("attributeCode").value = attribute?.code || "";
        document.getElementById("attributeType").value = attribute?.type || "";
        document.getElementById("attributeDescription").value = attribute?.description || "";
        document.getElementById("attributeUnit").value = attribute?.unit || "";
        document.getElementById("attributeCode").readOnly = Boolean(attribute);
        hideAlert("attributeFormAlert");
        attributeModal.show();
    }

    async function saveAttribute(e) {
        e.preventDefault();
        const button = document.getElementById("saveAttributeButton");
        setLoading(button, true, "Saving...");
        const payload = {
            name: document.getElementById("attributeName").value.trim(),
            code: document.getElementById("attributeCode").value.trim(),
            type: document.getElementById("attributeType").value,
            description: document.getElementById("attributeDescription").value.trim() || null,
            unit: document.getElementById("attributeUnit").value.trim() || null
        };

        try {
            const editing = Boolean(state.editingId);
            const result = await SellzonAPI.json(editing ? `${API}/${state.editingId}` : API, {
                method: editing ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            if (!result.response.ok) throw await apiError(result.response, result.data);
            attributeModal.hide();
            showPageAlert(editing ? "Attribute updated successfully." : "Attribute created successfully.", "success");
            await loadAttributes();
        } catch (e) {
            showAlert("attributeFormAlert", e.message || "Unable to save attribute.");
        } finally {
            setLoading(button, false, "Save Attribute");
        }
    }

    function openOptionsModal(attribute) {
        state.optionsAttributeId = attribute.id;
        document.getElementById("optionsModalTitle").textContent = `${attribute.name} — Options`;
        document.getElementById("optionsModalSubtitle").textContent = `${attribute.type} · ${attribute.code}`;
        document.getElementById("optionValue").value = "";
        document.getElementById("optionCode").value = "";
        hideAlert("optionsAlert");
        optionsModal.show();
        loadOptions();
    }

    async function loadOptions() {
        const list = document.getElementById("optionsList");
        list.innerHTML = `<div class="table-loading"><div class="spinner-border spinner-border-sm"></div> Loading options...</div>`;
        try {
            const result = await SellzonAPI.json(`${API}/${state.optionsAttributeId}/options`);
            if (!result.response.ok) throw await apiError(result.response, result.data);
            const options = result.data || [];
            if (!options.length) {
                list.innerHTML = `<div class="empty-state compact"><i class="bi bi-list-check"></i><strong>No options yet</strong><span>Add the allowed values above.</span></div>`;
                return;
            }
            list.innerHTML = `<div class="option-list">${options.map(o => `
                <div class="option-row">
                    <span class="option-order">${o.displayOrder}</span>
                    <div class="flex-grow-1"><strong>${esc(o.value)}</strong><small><code>${esc(o.code)}</code></small></div>
                    <button class="btn btn-sm btn-light table-action" data-option-edit="${o.id}" data-value="${escAttr(o.value)}"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-light table-action danger-action" data-option-delete="${o.id}" data-value="${escAttr(o.value)}"><i class="bi bi-slash-circle"></i></button>
                </div>`).join("")}</div>`;

            list.querySelectorAll("[data-option-edit]").forEach(b => b.addEventListener("click", () => editOption(b.dataset.optionEdit, b.dataset.value)));
            list.querySelectorAll("[data-option-delete]").forEach(b => b.addEventListener("click", () => openConfirm("Deactivate Option", `Deactivate "${b.dataset.value}"?`, () => deactivateOption(b.dataset.optionDelete))));
        } catch (e) {
            list.innerHTML = `<div class="empty-state text-danger">${esc(e.message)}</div>`;
        }
    }

    async function createOption() {
        const value = document.getElementById("optionValue").value.trim();
        const code = document.getElementById("optionCode").value.trim();
        if (!value || !code) {
            showAlert("optionsAlert", "Option value and code are required.");
            return;
        }
        const button = document.getElementById("createOptionButton");
        setLoading(button, true, "Adding...");
        try {
            const result = await SellzonAPI.json(`${API}/${state.optionsAttributeId}/options`, {
                method: "POST", body: JSON.stringify({ value, code })
            });
            if (!result.response.ok) throw await apiError(result.response, result.data);
            document.getElementById("optionValue").value = "";
            document.getElementById("optionCode").value = "";
            hideAlert("optionsAlert");
            await loadOptions();
        } catch (e) {
            showAlert("optionsAlert", e.message || "Unable to add option.");
        } finally {
            setLoading(button, false, "Add");
        }
    }

    async function editOption(optionId, currentValue) {
        const value = window.prompt("Enter the new option value:", currentValue);
        if (value === null || !value.trim() || value.trim() === currentValue) return;
        try {
            const result = await SellzonAPI.json(`${API}/option/${optionId}`, {
                method: "PUT", body: JSON.stringify({ value: value.trim() })
            });
            if (!result.response.ok) throw await apiError(result.response, result.data);
            await loadOptions();
        } catch (e) {
            showAlert("optionsAlert", e.message || "Unable to update option.");
        }
    }

    async function deactivateOption(optionId) {
        try {
            const result = await SellzonAPI.request(`${API}/option/${optionId}/deactivate`, { method: "PATCH" });
            if (!result.ok) throw new Error(`Request failed with status ${result.status}.`);
            await loadOptions();
        } catch (e) {
            showAlert("optionsAlert", e.message || "Unable to deactivate option.");
        }
    }

    async function deactivateAttribute(id) {
        try {
            const result = await SellzonAPI.request(`${API}/${id}/deactivate`, { method: "PATCH" });
            if (!result.ok) throw new Error(`Request failed with status ${result.status}.`);
            showPageAlert("Attribute deactivated successfully.", "success");
            await loadAttributes();
        } catch (e) {
            showPageAlert(e.message || "Unable to deactivate attribute.", "error");
        }
    }

    function openConfirm(title, message, action) {
        document.getElementById("confirmTitle").textContent = title;
        document.getElementById("confirmMessage").textContent = message;
        state.confirmAction = action;
        confirmModal.show();
    }

    async function runConfirmAction() {
        const action = state.confirmAction;
        if (!action) return;
        const button = document.getElementById("confirmActionButton");
        setLoading(button, true, "Working...");
        try { await action(); confirmModal.hide(); }
        finally { state.confirmAction = null; setLoading(button, false, "Deactivate"); }
    }

    async function handleLogout() {
        try { await SellzonAPI.logout(); }
        catch (_) { SellzonAPI.clearAuth(); window.location.href = "/login.html"; }
    }

    async function apiError(response, data) {
        return new Error(data?.message || data?.error || `Request failed with status ${response.status}.`);
    }

    function showPageAlert(message, type) {
        const el = document.getElementById("pageAlert");
        el.className = `page-alert ${type === "success" ? "success" : "error"}`;
        el.textContent = message;
        clearTimeout(showPageAlert.timer);
        showPageAlert.timer = setTimeout(() => el.classList.add("d-none"), 4500);
    }

    function showAlert(id, message) {
        const el = document.getElementById(id);
        el.className = "form-alert error";
        el.textContent = message;
    }

    function hideAlert(id) {
        const el = document.getElementById(id);
        el.className = "form-alert d-none";
        el.textContent = "";
    }

    function setLoading(button, loading, text) {
        if (!button) return;
        if (loading) {
            button.dataset.original = button.innerHTML;
            button.disabled = true;
            button.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>${esc(text)}`;
        } else {
            button.disabled = false;
            button.innerHTML = button.dataset.original || esc(text);
        }
    }

    function esc(v) {
        return String(v ?? "").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#039;");
    }
    function escAttr(v) { return esc(v); }
})();
