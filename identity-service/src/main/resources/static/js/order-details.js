document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("CUSTOMER")) return;
    const root = document.getElementById("orderRoot");
    const id = new URLSearchParams(location.search).get("id");
    if (!id) { root.innerHTML = '<div class="alert alert-danger">Order ID is required.</div>'; return; }
    try {
        const { response, data } = await SellzonAPI.json(`/api/customer/orders/${encodeURIComponent(id)}`);
        if (!response.ok) throw new Error(data?.message || "Order not found.");
        const a = data.shippingAddress;
        root.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-4"><div><h1 class="h3 mb-1">${CustomerStore.esc(data.orderNumber)}</h1><div class="text-secondary">${new Date(data.createdAt).toLocaleString("en-IN")}</div></div><span class="badge text-bg-dark">${CustomerStore.esc(data.orderStatus)}</span></div>
            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="card border-0 shadow-sm mb-4"><div class="card-body"><h2 class="h5 mb-3">Items</h2>${data.items.map(i => `<div class="d-flex justify-content-between gap-3 border-bottom py-3"><div><div class="fw-semibold">${CustomerStore.esc(i.productTitle)}</div><div class="small text-secondary">${CustomerStore.esc(i.sku)} · Qty ${i.quantity}</div><div class="small text-secondary">Sold by ${CustomerStore.esc(i.sellerName)}</div>${i.sellerOrderStatus ? `<div class="small mt-1"><span class="badge text-bg-light border">Seller status: ${CustomerStore.esc(i.sellerOrderStatus)}</span></div>` : ""}</div><div class="text-end"><div>${CustomerStore.money(i.unitPrice)} × ${i.quantity}</div><strong>${CustomerStore.money(i.totalPrice)}</strong></div></div>`).join("")}</div></div>
                    <div class="card border-0 shadow-sm"><div class="card-body"><h2 class="h5">Delivery address</h2><div class="mt-3"><strong>${CustomerStore.esc(a.recipientName)}</strong><div>${CustomerStore.esc(a.addressLine1)}</div>${a.addressLine2 ? `<div>${CustomerStore.esc(a.addressLine2)}</div>` : ""}${a.landmark ? `<div>Near ${CustomerStore.esc(a.landmark)}</div>` : ""}<div>${CustomerStore.esc(a.city)}, ${CustomerStore.esc(a.state)} - ${CustomerStore.esc(a.postalCode)}</div><div>${CustomerStore.esc(a.country)}</div><div class="mt-1">Phone: ${CustomerStore.esc(a.phoneNumber)}</div></div></div></div>
                </div>
                <div class="col-lg-4"><div class="card border-0 shadow-sm"><div class="card-body"><h2 class="h5">Order summary</h2><div class="d-flex justify-content-between mt-3"><span>Subtotal</span><span>${CustomerStore.money(data.subtotal)}</span></div><div class="d-flex justify-content-between mt-2"><span>Shipping</span><span>${CustomerStore.money(data.shippingAmount)}</span></div><div class="d-flex justify-content-between mt-2"><span>Discount</span><span>${CustomerStore.money(data.discountAmount)}</span></div><hr><div class="d-flex justify-content-between fw-bold fs-5"><span>Total</span><span>${CustomerStore.money(data.totalAmount)}</span></div><div class="small text-secondary mt-3">Payment: ${CustomerStore.esc(data.paymentMethod)} · ${CustomerStore.esc(data.paymentStatus)}</div></div></div></div>
            </div>`;
    } catch (e) { root.innerHTML = `<div class="alert alert-danger">${CustomerStore.esc(e.message)}</div>`; }
});
