let inventory = [];
let selectedProductId = null;
let inventoryModal = null;

/* =========================================================
   INVENTORY PAGE INITIALIZATION
   ========================================================= */

document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("SELLER")) return;

    const user = SellzonAPI.getUser();
    const sellerName = document.getElementById("sellerName");

    if (sellerName && user?.email) {
        sellerName.textContent = user.email;
    }

    document.getElementById("searchInput")?.addEventListener("input", renderInventory);
    document.getElementById("stockFilter")?.addEventListener("change", renderInventory);
    document.getElementById("inventoryOperation")?.addEventListener("change", updateReasonTypes);
    document.getElementById("saveInventoryButton")?.addEventListener("click", saveInventory);
    document.getElementById("saveThresholdButton")?.addEventListener("click", saveThreshold);

    inventoryModal = new bootstrap.Modal(document.getElementById("inventoryModal"));

    await loadInventory();
});


/* =========================================================
   LOAD INVENTORY
   ========================================================= */

async function loadInventory() {
    const tbody = document.getElementById("inventoryTableBody");

    try {
        const { response, data } = await SellzonAPI.json("/api/seller/inventory");

        if (!response.ok) {
            throw new Error(data?.message || "Unable to load inventory.");
        }

        inventory = Array.isArray(data) ? data : [];

        updateStats();
        renderInventory();

    } catch (e) {
        console.error(e);

        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5 text-danger">
                    ${esc(e.message)}
                </td>
            </tr>
        `;
    }
}


/* =========================================================
   INVENTORY STATISTICS
   ========================================================= */

function updateStats() {
    document.getElementById("totalProducts").textContent = inventory.length;

        document.getElementById("inStockProducts").textContent =
            inventory.filter(x => x.availableQuantity > 0 && !x.lowStock).length;

        document.getElementById("lowStockProducts").textContent =
            inventory.filter(x => x.lowStock).length;

        document.getElementById("outOfStockProducts").textContent =
            inventory.filter(x => x.outOfStock).length;
}


/* =========================================================
   RENDER INVENTORY TABLE
   ========================================================= */

function renderInventory() {
    const tbody = document.getElementById("inventoryTableBody");
    const empty = document.getElementById("emptyState");

    const query = (document.getElementById("searchInput")?.value || "").trim().toLowerCase();
    const filter = document.getElementById("stockFilter")?.value || "ALL";

    const rows = inventory.filter(x => {
        const text = [x.title, x.brand, x.sku, x.marketplaceProductId]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();

        const q = !query || text.includes(query);

        const f =
            filter === "ALL" ||
            (filter === "IN_STOCK" && x.availableQuantity > 0 && !x.lowStock) ||
            (filter === "LOW" && x.lowStock) ||
            (filter === "OUT" && x.outOfStock);

        return q && f;
    });

    if (!rows.length) {
        tbody.innerHTML = "";
        empty.classList.remove("d-none");
        return;
    }

    empty.classList.add("d-none");

    tbody.innerHTML = rows.map(x => `
        <tr>
            <td>
                <div class="fw-semibold">${esc(x.title)}</div>
                <div class="small text-secondary">${esc(x.brand)}</div>
            </td>

            <td>
                <code>${esc(x.sku)}</code>
            </td>

            <td>
                <div class="fw-semibold">${x.availableQuantity}</div>
                <div class="small text-secondary">Available</div>
            </td>

            <td>
                <div class="fw-semibold">${x.blockedQuantity}</div>
                <div class="small text-secondary">Blocked</div>
            </td>

            <td>${x.lowStockThreshold}</td>

            <td>
                <span class="badge ${stockClass(x)}">${stockLabel(x)}</span>
            </td>

            <td class="text-end">
                <button type="button" class="btn btn-outline-dark btn-sm"
                        onclick="openInventory('${js(x.marketplaceProductId)}')">
                    <i class="bi bi-sliders me-1"></i>
                    Manage
                </button>
            </td>
        </tr>
    `).join("");
}


/* =========================================================
   OPEN INVENTORY MODAL
   ========================================================= */

function openInventory(id) {
    const p = inventory.find(x => x.marketplaceProductId === id);

    if (!p) return;

    selectedProductId = id;

    document.getElementById("inventoryModalProduct").textContent = `${p.title} · ${p.sku}`;
    document.getElementById("currentAvailable").textContent = p.availableQuantity;
    document.getElementById("currentBlocked").textContent = p.blockedQuantity;
    document.getElementById("currentThreshold").textContent = p.lowStockThreshold;
    document.getElementById("inventoryThreshold").value = p.lowStockThreshold;

    /*
     * Default action:
     * ADD + RESTOCK
     */
    document.getElementById("inventoryOperation").value = "ADD";
    document.getElementById("inventoryQuantity").value = "0";

    /*
     * Populate the reason list according to ADD.
     */
    updateReasonTypes();

    document.getElementById("inventoryReason").value = "";

    clearModalAlert();

    loadHistory(id);

    inventoryModal.show();
}


/* =========================================================
   REASON TYPE RULES
   ========================================================= */

/*
 * Business rules:
 *
 * ADD:
 *   RESTOCK
 *   RETURN
 *   MANUAL_CORRECTION
 *   OTHER
 *
 * REMOVE:
 *   DAMAGED
 *   LOST
 *   MANUAL_CORRECTION
 *   OTHER
 *
 * SET:
 *   MANUAL_CORRECTION
 */

const INVENTORY_REASON_TYPES = {
    ADD: [
        { value: "RESTOCK", label: "Restock" },
        { value: "RETURN", label: "Customer Return" },
        { value: "MANUAL_CORRECTION", label: "Manual Correction" },
        { value: "OTHER", label: "Other" }
    ],

    REMOVE: [
        { value: "DAMAGED", label: "Damaged" },
        { value: "LOST", label: "Lost" },
        { value: "MANUAL_CORRECTION", label: "Manual Correction" },
        { value: "OTHER", label: "Other" }
    ],

    SET: [
        { value: "MANUAL_CORRECTION", label: "Manual Correction" }
    ]
};


/*
 * Update reason types according to operation.
 *
 * Can be called in two ways:
 *
 * updateReasonTypes()
 *
 * or
 *
 * updateReasonTypes("ADD", "RESTOCK")
 */
function updateReasonTypes() {

    const operationSelect =
        document.getElementById("inventoryOperation");

    const typeSelect =
        document.getElementById("inventoryType");

    if (!operationSelect || !typeSelect) return;

    const operation = operationSelect.value;

    const currentType = typeSelect.value;

    const availableTypes = {
        ADD: [
            {
                value: "RESTOCK",
                label: "Restock"
            },
            {
                value: "RETURN",
                label: "Customer Return"
            },
            {
                value: "MANUAL_CORRECTION",
                label: "Manual Correction"
            },
            {
                value: "OTHER",
                label: "Other"
            }
        ],

        REMOVE: [
            {
                value: "DAMAGED",
                label: "Damaged"
            },
            {
                value: "LOST",
                label: "Lost"
            },
            {
                value: "MANUAL_CORRECTION",
                label: "Manual Correction"
            },
            {
                value: "OTHER",
                label: "Other"
            }
        ],

        SET: [
            {
                value: "MANUAL_CORRECTION",
                label: "Manual Correction"
            }
        ]
    }[operation] || [];

    typeSelect.innerHTML = "";

    availableTypes.forEach(type => {

        const option =
            document.createElement("option");

        option.value = type.value;
        option.textContent = type.label;

        typeSelect.appendChild(option);
    });

    const stillValid =
        availableTypes.some(
            type => type.value === currentType
        );

    if (stillValid) {
        typeSelect.value = currentType;
    } else if (availableTypes.length > 0) {
        typeSelect.value =
            availableTypes[0].value;
    }
}


/* =========================================================
   LOAD INVENTORY HISTORY
   ========================================================= */

async function loadHistory(id) {
    const c = document.getElementById("inventoryHistory");

    c.innerHTML = `<div class="text-secondary small">Loading history...</div>`;

    try {
        const { response, data } =
            await SellzonAPI.json(`/api/seller/inventory/${encodeURIComponent(id)}/history`);

        if (!response.ok) {
            throw new Error(data?.message || "Unable to load inventory history.");
        }

        if (!data?.length) {
            c.innerHTML = `<div class="text-secondary small">No inventory adjustments yet.</div>`;
            return;
        }

        c.innerHTML = data.map(x => {
            const positive = Number(x.quantityChange) > 0;

            return `
                <div class="inventory-history-row">
                    <div>
                        <div class="fw-semibold">${esc(x.type)}</div>
                        <div class="small text-secondary">
                            ${formatDate(x.createdAt)}
                            ${x.reason ? ` · ${esc(x.reason)}` : ""}
                        </div>
                    </div>

                    <div class="text-end">
                        <div class="${positive ? "text-success" : "text-danger"} fw-semibold">
                            ${positive ? "+" : ""}${x.quantityChange}
                        </div>
                        <div class="small text-secondary">
                            ${x.previousStock} → ${x.newStock}
                        </div>
                    </div>
                </div>
            `;
        }).join("");

    } catch (e) {
        c.innerHTML = `<div class="text-danger small">${esc(e.message)}</div>`;
    }
}


/* =========================================================
   SAVE INVENTORY
   ========================================================= */

async function saveInventory() {
    if (!selectedProductId) return;

    const operation = document.getElementById("inventoryOperation").value;
    const quantity = Number(document.getElementById("inventoryQuantity").value);
    const type = document.getElementById("inventoryType").value;
    const reason = document.getElementById("inventoryReason").value.trim();

    /* -----------------------------------------------------
       Basic quantity validation
       ----------------------------------------------------- */

    if (!Number.isInteger(quantity) || quantity < 0) {
        return showModalAlert(
            "Quantity must be a whole number greater than or equal to 0.",
            "danger"
        );
    }

    /*
     * ADD and REMOVE cannot use quantity 0.
     *
     * SET can use 0.
     *
     * This is important because:
     *
     * SET + MANUAL_CORRECTION + 0
     *
     * is the valid way to intentionally stop selling
     * by reducing sellable inventory to zero.
     */
    if (operation !== "SET" && quantity === 0) {
        return showModalAlert("Quantity must be greater than 0.", "danger");
    }

    /* -----------------------------------------------------
       Validate operation/reason combination on frontend
       ----------------------------------------------------- */

    const validTypes = INVENTORY_REASON_TYPES[operation] || [];
    const validType = validTypes.some(item => item.value === type);

    if (!validType) {
        return showModalAlert(
            "The selected reason type is not valid for this action.",
            "danger"
        );
    }

    /*
     * Extra protection:
     *
     * SET should only be used with MANUAL_CORRECTION.
     */
    if (operation === "SET" && type !== "MANUAL_CORRECTION") {
        return showModalAlert("Set Stock can only use Manual Correction.", "danger");
    }

    const b = document.getElementById("saveInventoryButton");
    b.disabled = true;
    document.getElementById("saveInventorySpinner").classList.remove("d-none");

    clearModalAlert();

    try {
        const { response, data } = await SellzonAPI.json(
            `/api/seller/inventory/${encodeURIComponent(selectedProductId)}`,
            {
                method: "PATCH",
                body: JSON.stringify({
                    operation,
                    quantity,
                    type,
                    reason: reason || null
                })
            }
        );

        if (!response.ok) {
            throw new Error(data?.message || "Unable to update inventory.");
        }

        /*
         * Update local inventory immediately.
         */
        const i = inventory.findIndex(x => x.marketplaceProductId === selectedProductId);

        if (i >= 0) {
            inventory[i] = data;
        }

        updateStats();
        renderInventory();

        /*
         * Update current stock displayed
         * inside the modal.
         */
        // Refresh the inventory values shown in the modal using the actual API DTO fields.
        document.getElementById("currentAvailable").textContent =
            data.availableQuantity ?? 0;
        document.getElementById("currentBlocked").textContent =
            data.blockedQuantity ?? 0;
        document.getElementById("currentThreshold").textContent =
            data.lowStockThreshold ?? 5;
        document.getElementById("inventoryThreshold").value =
            data.lowStockThreshold ?? 5;

        /*
         * Reset quantity and note after successful save.
         */
        document.getElementById("inventoryQuantity").value = "0";
        document.getElementById("inventoryReason").value = "";

        /*
         * Reload history so the latest adjustment
         * immediately appears.
         */
        await loadHistory(selectedProductId);

        showModalAlert("Inventory updated successfully.", "success");

    } catch (e) {
        showModalAlert(e.message, "danger");

    } finally {
        b.disabled = false;
        document.getElementById("saveInventorySpinner").classList.add("d-none");
    }
}


/* =========================================================
   SAVE LOW-STOCK THRESHOLD
   ========================================================= */

async function saveThreshold() {
    if (!selectedProductId) return;

    const threshold = Number(document.getElementById("inventoryThreshold").value);

    if (!Number.isInteger(threshold) || threshold < 0) {
        return showModalAlert(
            "Threshold must be a whole number greater than or equal to 0.",
            "danger"
        );
    }

    const b = document.getElementById("saveThresholdButton");
    b.disabled = true;

    try {
        const { response, data } = await SellzonAPI.json(
            `/api/seller/inventory/${encodeURIComponent(selectedProductId)}/threshold`,
            {
                method: "PATCH",
                body: JSON.stringify({ lowStockThreshold: threshold })
            }
        );

        if (!response.ok) {
            throw new Error(data?.message || "Unable to update threshold.");
        }

        const i = inventory.findIndex(x => x.marketplaceProductId === selectedProductId);

        if (i >= 0) {
            inventory[i].lowStockThreshold = data.lowStockThreshold;
            inventory[i].lowStock =
                inventory[i].availableQuantity > 0 &&
                inventory[i].availableQuantity <= data.lowStockThreshold;
            inventory[i].outOfStock =
                inventory[i].availableQuantity === 0;
        }

        updateStats();
        renderInventory();

        document.getElementById("currentThreshold").textContent = data.lowStockThreshold;

        showModalAlert("Low-stock threshold updated.", "success");

    } catch (e) {
        showModalAlert(e.message, "danger");

    } finally {
        b.disabled = false;
    }
}


/* =========================================================
   STOCK STATUS
   ========================================================= */

function stockLabel(x) {
    return x.outOfStock ? "Out of stock" : x.lowStock ? "Low stock" : "In stock";
}


function stockClass(x) {
    return x.outOfStock ? "text-bg-danger" : x.lowStock ? "text-bg-warning" : "text-bg-success";
}


/* =========================================================
   MODAL ALERT
   ========================================================= */

function showModalAlert(message, type) {
    const a = document.getElementById("modalAlert");
    a.className = `alert alert-${type}`;
    a.textContent = message;
}


function clearModalAlert() {
    const a = document.getElementById("modalAlert");
    a.className = "alert d-none";
    a.textContent = "";
}


/* =========================================================
   DATE FORMAT
   ========================================================= */

function formatDate(value) {
    return value ? new Date(value).toLocaleString("en-IN") : "-";
}


/* =========================================================
   HTML ESCAPING
   ========================================================= */

function esc(value) {
    return String(value ?? "").replace(/[&<>'"]/g, c => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "'": "&#39;",
        '"': "&quot;"
    }[c]));
}


/* =========================================================
   JAVASCRIPT STRING ESCAPING
   ========================================================= */

function js(value) {
    return String(value ?? "")
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'");
}