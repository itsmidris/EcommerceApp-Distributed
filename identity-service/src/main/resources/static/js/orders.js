document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("CUSTOMER")) return;
    const root = document.getElementById("ordersRoot");
    try {
        const { response, data } = await SellzonAPI.json("/api/customer/orders");
        if (!response.ok) throw new Error(data?.message || "Unable to load orders.");
        if (!data?.length) {
            root.innerHTML = `<div class="text-center py-5"><h2 class="h5">You have no orders yet.</h2><a class="btn btn-dark mt-2" href="/customer/products.html">Start shopping</a></div>`;
            return;
        }
        root.innerHTML = data.map(o => `
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start gap-3">
                        <div><div class="fw-bold">${CustomerStore.esc(o.orderNumber)}</div><div class="small text-secondary">${new Date(o.createdAt).toLocaleString("en-IN")}</div></div>
                        <span class="badge text-bg-dark">${CustomerStore.esc(o.orderStatus)}</span>
                    </div>
                    <div class="small mt-3">${o.items.length} item${o.items.length === 1 ? "" : "s"} · ${CustomerStore.esc(o.paymentMethod)}</div>
                    <div class="fw-bold mt-2">${CustomerStore.money(o.totalAmount)}</div>
                    <a class="btn btn-outline-dark btn-sm mt-3" href="/customer/order-details.html?id=${encodeURIComponent(o.id)}">View order</a>
                </div>
            </div>`).join("");
    } catch (e) {
        root.innerHTML = `<div class="alert alert-danger">${CustomerStore.esc(e.message)}</div>`;
    }
});
