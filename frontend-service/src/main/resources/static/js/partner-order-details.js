const SELLER_STATUS_FLOW = ["PLACED", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED"];
const SELLER_STATUS_LABELS = {
    PLACED: "Placed",
    CONFIRMED: "Confirmed",
    PROCESSING: "Processing",
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled"
};

let currentSellerOrder = null;


document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("SELLER")) return;

    const user = SellzonAPI.getUser();
    const sellerName = document.getElementById("sellerName");
    if (sellerName && user?.email) sellerName.textContent = user.email;

    const id = new URLSearchParams(location.search).get("id");
    const root = document.getElementById("orderRoot");

    if (!id) {
        root.innerHTML = '<div class="alert alert-danger">Seller order ID is required.</div>';
        return;
    }

    await loadOrder(id);
});

async function loadOrder(id) {
    const root = document.getElementById("orderRoot");

    try {
        const { response, data } = await SellzonAPI.json(`/api/seller/orders/${encodeURIComponent(id)}`);
        if (!response.ok) throw new Error(data?.message || "Order not found.");

        currentSellerOrder = data;
        renderOrder(data);
    } catch (error) {
        console.error(error);
        root.innerHTML = `<div class="alert alert-danger">${esc(error.message)}</div>`;
    }
}

function renderOrder(order) {
    const root = document.getElementById("orderRoot");
    const address = order.shippingAddress;

    root.innerHTML = `
        <div class="seller-order-header mb-4">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                    <div class="small text-secondary mb-1">Seller order</div>
                    <h1 class="h3 mb-1">${esc(order.sellerOrderNumber)}</h1>
                    <div class="text-secondary">Customer order: ${esc(order.orderNumber)}</div>
                    <div class="small text-secondary mt-1">Placed ${formatDate(order.createdAt)}</div>
                </div>
                <div class="text-md-end">
                    <span class="badge ${statusClass(order.status)} px-3 py-2">${esc(SELLER_STATUS_LABELS[order.status] || order.status)}</span>
                    <div class="small text-secondary mt-2">Payment: ${esc(order.paymentMethod)} · ${esc(order.paymentStatus)}</div>
                </div>
            </div>
            ${renderTimeline(order.status)}
        </div>

        <div id="actionAlert" class="alert d-none"></div>

        <div class="row g-4">
            <div class="col-xl-8">
                <div class="seller-order-card mb-4">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div><h2 class="h5 mb-1">Items to process</h2><div class="small text-secondary">Only your products from this customer order are shown.</div></div>
                            <span class="badge text-bg-light border">${order.items.length} item${order.items.length === 1 ? "" : "s"}</span>
                        </div>
                        ${order.items.map(item => `
                            <div class="order-item-row">
                                <div class="d-flex justify-content-between gap-3">
                                    <div>
                                        <div class="fw-semibold">${esc(item.productTitle)}</div>
                                        <div class="small text-secondary">SKU: ${esc(item.sku)}</div>
                                        <div class="small text-secondary">Marketplace ID: ${esc(item.marketplaceProductId)}</div>
                                    </div>
                                    <div class="text-end">
                                        <div>${money(item.unitPrice)} × ${item.quantity}</div>
                                        <strong>${money(item.totalPrice)}</strong>
                                    </div>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                </div>

                <div class="order-address-card">
                    <h2 class="h5">Delivery address</h2>
                    <div class="mt-3">
                        <div class="fw-semibold">${esc(address.recipientName)}</div>
                        <div>${esc(address.addressLine1)}</div>
                        ${address.addressLine2 ? `<div>${esc(address.addressLine2)}</div>` : ""}
                        ${address.landmark ? `<div>Near ${esc(address.landmark)}</div>` : ""}
                        <div>${esc(address.city)}, ${esc(address.state)} - ${esc(address.postalCode)}</div>
                        <div>${esc(address.country)}</div>
                        <div class="mt-2"><i class="bi bi-telephone me-1"></i>${esc(address.phoneNumber)}</div>
                    </div>
                </div>
            </div>

            <div class="col-xl-4">
                <div class="order-summary-card mb-4">
                    <h2 class="h5">Order summary</h2>
                    <div class="d-flex justify-content-between mt-3"><span>Subtotal</span><span>${money(order.subtotal)}</span></div>
                    <div class="d-flex justify-content-between mt-2"><span>Shipping</span><span>${money(order.shippingAmount)}</span></div>
                    <div class="d-flex justify-content-between mt-2"><span>Discount</span><span>${money(order.discountAmount)}</span></div>
                    <hr>
                    <div class="d-flex justify-content-between fw-bold fs-5"><span>Your total</span><span>${money(order.totalAmount)}</span></div>
                    <div class="small text-secondary mt-3">Customer payment: ${esc(order.paymentMethod)} · ${esc(order.paymentStatus)}</div>
                </div>

                <div class="order-summary-card">
                    <h2 class="h5">Process order</h2>
                    <p class="small text-secondary">Move the order through the allowed fulfillment stages. The server validates every transition.</p>
                    <div class="order-action-group">${actionButtons(order.status)}</div>
                </div>
            </div>
        </div>
    `;

    document.querySelectorAll("[data-order-status]").forEach(button => {
        button.addEventListener("click", () => updateStatus(button.dataset.orderStatus));
    });
}

function actionButtons(status) {
    const buttons = [];

    const next = {
        PLACED: ["CONFIRMED"],
        CONFIRMED: ["PROCESSING"],
        PROCESSING: ["SHIPPED"],
        SHIPPED: ["DELIVERED"],
        DELIVERED: [],
        CANCELLED: []
    }[status] || [];

    next.forEach(target => {
        buttons.push(`<button type="button" class="btn btn-dark" data-order-status="${target}"><i class="bi bi-arrow-right me-1"></i>${esc(SELLER_STATUS_LABELS[target])}</button>`);
    });

    if (["PLACED", "CONFIRMED", "PROCESSING"].includes(status)) {
        buttons.push(`<button type="button" class="btn btn-outline-danger" data-order-status="CANCELLED"><i class="bi bi-x-circle me-1"></i>Cancel order</button>`);
    }

    if (!buttons.length) {
        return `<div class="small text-secondary">No further processing action is available.</div>`;
    }

    return buttons.join("");
}

async function updateStatus(targetStatus) {
    if (!currentSellerOrder) return;

    if (targetStatus === "CANCELLED" && !window.confirm("Cancel this seller order? This action cannot be reversed.")) {
        return;
    }

    const buttons = [...document.querySelectorAll("[data-order-status]")];
    buttons.forEach(button => button.disabled = true);

    const alert = document.getElementById("actionAlert");
    alert.className = "alert alert-info";
    alert.textContent = "Updating order status...";

    try {
        const { response, data } = await SellzonAPI.json(
            `/api/seller/orders/${encodeURIComponent(currentSellerOrder.id)}/status`,
            {
                method: "PATCH",
                body: JSON.stringify({ status: targetStatus })
            }
        );

        if (!response.ok) throw new Error(data?.message || "Unable to update order status.");

        currentSellerOrder = data;
        renderOrder(data);

        const successAlert = document.getElementById("actionAlert");
        successAlert.className = "alert alert-success";
        successAlert.textContent = `Order moved to ${SELLER_STATUS_LABELS[data.status] || data.status}.`;
    } catch (error) {
        buttons.forEach(button => button.disabled = false);
        alert.className = "alert alert-danger";
        alert.textContent = error.message;
    }
}

function renderTimeline(status) {
    if (status === "CANCELLED") {
        return `<div class="status-timeline"><div class="timeline-step cancelled current"><div class="timeline-dot"></div>Cancelled</div></div>`;
    }

    const currentIndex = SELLER_STATUS_FLOW.indexOf(status);
    return `<div class="status-timeline">${SELLER_STATUS_FLOW.map((item, index) => `
        <div class="timeline-step ${index < currentIndex ? "done" : ""} ${index === currentIndex ? "current" : ""}">
            <div class="timeline-dot"></div>${esc(SELLER_STATUS_LABELS[item])}
        </div>
    `).join("")}</div>`;
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
    return value ? new Date(value).toLocaleString("en-IN") : "-";
}

function money(value) {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(value || 0));
}

function esc(value) {
    return String(value ?? "").replace(/[&<>'"]/g, char => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[char]));
}
