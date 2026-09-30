let sellerOrders = [];

const STATUS_LABELS = {
    PLACED: "Placed",
    CONFIRMED: "Confirmed",
    PROCESSING: "Processing",
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled"
};

document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("SELLER")) return;

    const user = SellzonAPI.getUser();
    const sellerName = document.getElementById("sellerName");
    if (sellerName && user?.email) sellerName.textContent = user.email;

    document.getElementById("searchInput")?.addEventListener("input", renderOrders);
    document.getElementById("statusFilter")?.addEventListener("change", renderOrders);

    await loadOrders();
});

async function loadOrders() {
    const tbody = document.getElementById("ordersTableBody");

    try {
        const { response, data } = await SellzonAPI.json("/api/seller/orders");
        if (!response.ok) throw new Error(data?.message || "Unable to load orders.");

        sellerOrders = Array.isArray(data) ? data : [];
        updateStats();
        renderOrders();
    } catch (error) {
        console.error("Unable to load seller orders:", error);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-5 text-danger">${esc(error.message)}</td></tr>`;
    }
}

function updateStats() {
    const count = status => sellerOrders.filter(order => order.status === status).length;
    document.getElementById("totalOrders").textContent = sellerOrders.length;
    document.getElementById("placedOrders").textContent = count("PLACED");
    document.getElementById("processingOrders").textContent = count("PROCESSING");
    document.getElementById("shippedOrders").textContent = count("SHIPPED");
}

function renderOrders() {
    const tbody = document.getElementById("ordersTableBody");
    const empty = document.getElementById("emptyState");
    const query = (document.getElementById("searchInput")?.value || "").trim().toLowerCase();
    const status = document.getElementById("statusFilter")?.value || "ALL";

    const filtered = sellerOrders.filter(order => {
        const searchable = [
            order.sellerOrderNumber,
            order.orderNumber,
            order.customerName,
            order.customerPhone
        ].filter(Boolean).join(" ").toLowerCase();

        return (!query || searchable.includes(query)) &&
            (status === "ALL" || order.status === status);
    });

    if (!filtered.length) {
        tbody.innerHTML = "";
        empty.classList.remove("d-none");
        return;
    }

    empty.classList.add("d-none");
    tbody.innerHTML = filtered.map(order => `
        <tr>
            <td>
                <div class="order-number">${esc(order.sellerOrderNumber)}</div>
                <div class="small text-secondary">Customer order: ${esc(order.orderNumber)}</div>
                <div class="small text-secondary">${formatDate(order.createdAt)}</div>
            </td>
            <td class="order-customer">
                <div class="fw-semibold">${esc(order.customerName)}</div>
                <div class="small text-secondary">${esc(order.customerPhone)}</div>
            </td>
            <td>${order.items?.length || 0}</td>
            <td><strong>${money(order.totalAmount)}</strong></td>
            <td>
                <div>${esc(order.paymentMethod)}</div>
                <div class="small text-secondary">${esc(order.paymentStatus)}</div>
            </td>
            <td><span class="badge ${statusClass(order.status)} order-badge">${esc(STATUS_LABELS[order.status] || order.status)}</span></td>
            <td class="text-end">
                <a class="btn btn-outline-dark btn-sm" href="/partner/order-details.html?id=${encodeURIComponent(order.id)}">View / Process</a>
            </td>
        </tr>
    `).join("");
}

function statusClass(status) {
    return {
        PLACED: "text-bg-warning",
        CONFIRMED: "text-bg-primary",
        PROCESSING: "text-bg-info",
        SHIPPED: "text-bg-dark",
        DELIVERED: "text-bg-success",
        CANCELLED: "text-bg-danger"
    }[status] || "text-bg-secondary";
}

function formatDate(value) {
    if (!value) return "-";
    return new Date(value).toLocaleString("en-IN");
}

function money(value) {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(value || 0));
}

function esc(value) {
    return String(value ?? "").replace(/[&<>'"]/g, char => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[char]));
}
