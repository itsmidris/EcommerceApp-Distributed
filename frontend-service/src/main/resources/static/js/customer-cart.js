document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole('CUSTOMER')) return;

    const root = document.getElementById('cartRoot');

    async function load() {
        const { response, data } = await SellzonAPI.json('/api/customer/cart');

        if (!response.ok) throw new Error(data?.message || 'Unable to load cart.');

        CustomerStore.setCartCount(data.totalItems);

        if (!data.items?.length) {
            root.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-cart3 display-3 text-secondary"></i>
                    <h1 class="h3 mt-3">Your cart is empty</h1>
                    <a href="/customer/products.html" class="btn btn-dark mt-2">Continue shopping</a>
                </div>
            `;
            return;
        }

        root.innerHTML = `
            <div class="row g-4">
                <div class="col-lg-8">
                    ${data.items.map(i => `
                        <div class="card border-0 shadow-sm mb-3">
                            <div class="card-body">
                                <div class="d-flex gap-3">
                                    <div class="cart-image d-flex align-items-center justify-content-center">
                                        ${
                                            i.imageUrl
                                                ? `<img src="${CustomerStore.esc(SellzonAPI.catalogAssetUrl(i.imageUrl))}"
                                                        alt="${CustomerStore.esc(i.title)}"
                                                        class="img-fluid rounded"
                                                        style="width: 90px; height: 90px; object-fit: contain;">`
                                                : `<i class="bi bi-image fs-2 text-secondary"></i>`
                                        }
                                    </div>
                                    <div class="flex-grow-1 cart-product-info">
                                        <a class="cart-product-title text-decoration-none text-dark fw-semibold" href="/customer/product-details.html?id=${encodeURIComponent(i.marketplaceProductId)}">${CustomerStore.esc(i.title)}</a>
                                        <div class="small text-secondary">${CustomerStore.esc(i.brand)}</div>
                                        <div class="fw-bold mt-2">${CustomerStore.money(i.sellingPrice)}</div>
                                        <div class="small text-secondary">${i.availableStock} available</div>
                                        <div class="d-flex align-items-center gap-2 mt-3">
                                            <input class="form-control qty" style="width:90px" type="number" min="1" max="${Math.min(99, i.availableStock)}" value="${i.quantity}" data-id="${CustomerStore.esc(i.marketplaceProductId)}">
                                            <button class="btn btn-outline-danger btn-sm remove" data-id="${CustomerStore.esc(i.marketplaceProductId)}">Remove</button>
                                        </div>
                                    </div>
                                    <div class="fw-bold cart-item-total">${CustomerStore.money(i.itemTotal)}</div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
                <div class="col-lg-4">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body">
                            <h2 class="h5">Order summary</h2>
                            <div class="d-flex justify-content-between mt-3">
                                <span>Items</span>
                                <span>${data.totalItems}</span>
                            </div>
                            <div class="d-flex justify-content-between fs-5 fw-bold mt-3">
                                <span>Subtotal</span>
                                <span>${CustomerStore.money(data.subtotal)}</span>
                            </div>
                            <button id="checkoutBtn" class="btn btn-dark w-100 mt-4">Proceed to Checkout</button>
                            <button id="clearCart" class="btn btn-outline-danger w-100 mt-2">Clear Cart</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        root.querySelectorAll('.qty').forEach(input => input.addEventListener('change', async () => {
            try {
                const r = await SellzonAPI.json(`/api/customer/cart/items/${encodeURIComponent(input.dataset.id)}`, {
                    method: 'PUT',
                    body: JSON.stringify({ quantity: Number(input.value) })
                });

                if (!r.response.ok) throw new Error(r.data?.message || 'Unable to update cart.');

                await load();
            } catch (e) {
                alert(e.message);
                await load();
            }
        }));

        root.querySelectorAll('.remove').forEach(b => b.addEventListener('click', async () => {
            await SellzonAPI.json(`/api/customer/cart/items/${encodeURIComponent(b.dataset.id)}`, {
                method: 'DELETE'
            });
            await load();
        }));

        document.getElementById('clearCart').addEventListener('click', async () => {
            if (!confirm('Clear your cart?')) return;
            await SellzonAPI.json('/api/customer/cart', { method: 'DELETE' });
            await load();
        });

        document.getElementById('checkoutBtn').addEventListener('click', () => location.href = '/customer/checkout.html');
    }

    try {
        await load();
    } catch (e) {
        root.innerHTML = `<div class="alert alert-danger">${CustomerStore.esc(e.message)}</div>`;
    }
});