let inventory = [];
let selectedProductId = null;
let inventoryModal = null;

document.addEventListener("DOMContentLoaded", async () => {
  if (!SellzonAPI.requireRole("SELLER")) return;
  const user = SellzonAPI.getUser();
  const sellerName = document.getElementById("sellerName");
  if (sellerName && user?.email) sellerName.textContent = user.email;
  document.getElementById("searchInput")?.addEventListener("input", renderInventory);
  document.getElementById("stockFilter")?.addEventListener("change", renderInventory);
  document.getElementById("saveInventoryButton")?.addEventListener("click", saveInventory);
  document.getElementById("saveThresholdButton")?.addEventListener("click", saveThreshold);
  inventoryModal = new bootstrap.Modal(document.getElementById("inventoryModal"));
  await loadInventory();
});

async function loadInventory() {
  const tbody = document.getElementById("inventoryTableBody");
  try {
    const { response, data } = await SellzonAPI.json("/api/seller/inventory");
    if (!response.ok) throw new Error(data?.message || "Unable to load inventory.");
    inventory = Array.isArray(data) ? data : [];
    updateStats();
    renderInventory();
  } catch (e) {
    console.error(e);
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-danger">${esc(e.message)}</td></tr>`;
  }
}

function updateStats() {
  document.getElementById("totalProducts").textContent = inventory.length;
  document.getElementById("inStockProducts").textContent = inventory.filter(x => x.stockQuantity > 0 && !x.lowStock).length;
  document.getElementById("lowStockProducts").textContent = inventory.filter(x => x.lowStock).length;
  document.getElementById("outOfStockProducts").textContent = inventory.filter(x => x.outOfStock).length;
}

function renderInventory() {
  const tbody = document.getElementById("inventoryTableBody"),
    empty = document.getElementById("emptyState"),
    query = (document.getElementById("searchInput")?.value || "").trim().toLowerCase(),
    filter = document.getElementById("stockFilter")?.value || "ALL";
  const rows = inventory.filter(x => {
    const text = [x.title, x.brand, x.sku, x.marketplaceProductId].filter(Boolean).join(" ").toLowerCase();
    const q = !query || text.includes(query);
    const f = filter === "ALL" ||
      (filter === "IN_STOCK" && x.stockQuantity > 0 && !x.lowStock) ||
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
  tbody.innerHTML = rows.map(x => `<tr><td><div class="fw-semibold">${esc(x.title)}</div><div class="small text-secondary">${esc(x.brand)}</div></td><td><code>${esc(x.sku)}</code></td><td><span class="fw-semibold">${x.stockQuantity}</span></td><td>${x.lowStockThreshold}</td><td><span class="badge ${stockClass(x)}">${stockLabel(x)}</span></td><td class="text-end"><button type="button" class="btn btn-outline-dark btn-sm" onclick="openInventory('${js(x.marketplaceProductId)}')"><i class="bi bi-sliders me-1"></i>Manage</button></td></tr>`).join("");
}

function openInventory(id) {
  const p = inventory.find(x => x.marketplaceProductId === id);
  if (!p) return;
  selectedProductId = id;
  document.getElementById("inventoryModalProduct").textContent = `${p.title} · ${p.sku}`;
  document.getElementById("currentStock").textContent = p.stockQuantity;
  document.getElementById("currentThreshold").textContent = p.lowStockThreshold;
  document.getElementById("inventoryThreshold").value = p.lowStockThreshold;
  document.getElementById("inventoryOperation").value = "ADD";
  document.getElementById("inventoryQuantity").value = "0";
  document.getElementById("inventoryType").value = "RESTOCK";
  document.getElementById("inventoryReason").value = "";
  clearModalAlert();
  loadHistory(id);
  inventoryModal.show();
}

async function loadHistory(id) {
  const c = document.getElementById("inventoryHistory");
  c.innerHTML = `<div class="text-secondary small">Loading history...</div>`;
  try {
    const { response, data } = await SellzonAPI.json(`/api/seller/inventory/${encodeURIComponent(id)}/history`);
    if (!response.ok) throw new Error(data?.message || "Unable to load inventory history.");
    if (!data?.length) {
      c.innerHTML = `<div class="text-secondary small">No inventory adjustments yet.</div>`;
      return;
    }
    c.innerHTML = data.map(x => {
      const positive = Number(x.quantityChange) > 0;
      return `<div class="inventory-history-row"><div><div class="fw-semibold">${esc(x.type)}</div><div class="small text-secondary">${formatDate(x.createdAt)}${x.reason ? ` · ${esc(x.reason)}` : ""}</div></div><div class="text-end"><div class="${positive ? "text-success" : "text-danger"} fw-semibold">${positive ? "+" : ""}${x.quantityChange}</div><div class="small text-secondary">${x.previousStock} → ${x.newStock}</div></div></div>`;
    }).join("");
  } catch (e) {
    c.innerHTML = `<div class="text-danger small">${esc(e.message)}</div>`;
  }
}

async function saveInventory() {
  if (!selectedProductId) return;
  const operation = document.getElementById("inventoryOperation").value,
    quantity = Number(document.getElementById("inventoryQuantity").value),
    type = document.getElementById("inventoryType").value,
    reason = document.getElementById("inventoryReason").value.trim();
  if (!Number.isInteger(quantity) || quantity < 0) return showModalAlert("Quantity must be a whole number greater than or equal to 0.", "danger");
  if (operation !== "SET" && quantity === 0) return showModalAlert("Quantity must be greater than 0.", "danger");
  const b = document.getElementById("saveInventoryButton");
  b.disabled = true;
  document.getElementById("saveInventorySpinner").classList.remove("d-none");
  clearModalAlert();
  try {
    const { response, data } = await SellzonAPI.json(`/api/seller/inventory/${encodeURIComponent(selectedProductId)}`, {
      method: "PATCH",
      body: JSON.stringify({ operation, quantity, type, reason: reason || null })
    });
    if (!response.ok) throw new Error(data?.message || "Unable to update inventory.");
    const i = inventory.findIndex(x => x.marketplaceProductId === selectedProductId);
    if (i >= 0) inventory[i] = data;
    updateStats();
    renderInventory();
    document.getElementById("currentStock").textContent = data.stockQuantity;
    document.getElementById("inventoryQuantity").value = "0";
    document.getElementById("inventoryReason").value = "";
    await loadHistory(selectedProductId);
    showModalAlert("Inventory updated successfully.", "success");
  } catch (e) {
    showModalAlert(e.message, "danger");
  } finally {
    b.disabled = false;
    document.getElementById("saveInventorySpinner").classList.add("d-none");
  }
}

async function saveThreshold() {
  if (!selectedProductId) return;
  const threshold = Number(document.getElementById("inventoryThreshold").value);
  if (!Number.isInteger(threshold) || threshold < 0) return showModalAlert("Threshold must be a whole number greater than or equal to 0.", "danger");
  const b = document.getElementById("saveThresholdButton");
  b.disabled = true;
  try {
    const { response, data } = await SellzonAPI.json(`/api/seller/inventory/${encodeURIComponent(selectedProductId)}/threshold`, {
      method: "PATCH",
      body: JSON.stringify({ lowStockThreshold: threshold })
    });
    if (!response.ok) throw new Error(data?.message || "Unable to update threshold.");
    const i = inventory.findIndex(x => x.marketplaceProductId === selectedProductId);
    if (i >= 0) {
      inventory[i].lowStockThreshold = data.lowStockThreshold;
      inventory[i].lowStock = inventory[i].stockQuantity > 0 && inventory[i].stockQuantity <= data.lowStockThreshold;
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

function stockLabel(x) {
  return x.outOfStock ? "Out of stock" : x.lowStock ? "Low stock" : "In stock";
}

function stockClass(x) {
  return x.outOfStock ? "text-bg-danger" : x.lowStock ? "text-bg-warning" : "text-bg-success";
}

function showModalAlert(m, t) {
  const a = document.getElementById("modalAlert");
  a.className = `alert alert-${t}`;
  a.textContent = m;
}

function clearModalAlert() {
  const a = document.getElementById("modalAlert");
  a.className = "alert d-none";
  a.textContent = "";
}

function formatDate(v) {
  return v ? new Date(v).toLocaleString("en-IN") : "-";
}

function esc(v) {
  return String(v ?? "").replace(/[&<>'"]/g, c => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "'": "&#39;",
    '"': "&quot;"
  }[c]));
}

function js(v) {
  return String(v ?? "").replaceAll("\\", "\\\\").replaceAll("'", "\\'");
}