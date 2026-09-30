document.addEventListener("DOMContentLoaded", async () => {
    if (!SellzonAPI.requireRole("CUSTOMER")) return;

    const root = document.getElementById("checkoutRoot");
    const loading = document.getElementById("checkoutLoading");

    function alertBox(message, type = "danger") {
        const el = document.getElementById("checkoutAlert");
        el.className = `alert alert-${type}`;
        el.textContent = message;
    }

    function addressHtml(a, selectedId) {
        return `
            <div class="card border ${a.id === selectedId ? "border-dark" : ""} h-100">
                <div class="card-body">
                    <div class="form-check">
                        <input class="form-check-input address-radio" type="radio" name="addressId"
                               value="${a.id}" ${a.id === selectedId ? "checked" : ""}>
                        <label class="form-check-label w-100">
                            <div class="d-flex justify-content-between gap-2">
                                <strong>${CustomerStore.esc(a.recipientName)}</strong>
                                ${a.defaultAddress ? '<span class="badge text-bg-dark">Default</span>' : ''}
                            </div>
                            <div class="small mt-2">${CustomerStore.esc(a.addressLine1)}</div>
                            ${a.addressLine2 ? `<div class="small">${CustomerStore.esc(a.addressLine2)}</div>` : ""}
                            ${a.landmark ? `<div class="small text-secondary">Near ${CustomerStore.esc(a.landmark)}</div>` : ""}
                            <div class="small">${CustomerStore.esc(a.city)}, ${CustomerStore.esc(a.state)} - ${CustomerStore.esc(a.postalCode)}</div>
                            <div class="small">${CustomerStore.esc(a.country)}</div>
                            <div class="small mt-1">Phone: ${CustomerStore.esc(a.phoneNumber)}</div>
                        </label>
                    </div>
                    <div class="d-flex gap-2 mt-3">
                        ${!a.defaultAddress ? `<button class="btn btn-sm btn-outline-dark make-default" data-id="${a.id}">Set default</button>` : ""}
                        <button class="btn btn-sm btn-outline-danger delete-address" data-id="${a.id}">Delete</button>
                    </div>
                </div>
            </div>`;
    }

    async function load() {
        loading.classList.remove("d-none");
        root.classList.add("d-none");
        try {
            const { response, data } = await SellzonAPI.json("/api/customer/checkout");
            if (!response.ok) throw new Error(data?.message || "Unable to load checkout.");
            if (!data.cart?.items?.length) {
                window.location.href = "/customer/cart.html";
                return;
            }

            const selectedId = data.defaultAddressId || data.addresses?.[0]?.id || null;
            root.innerHTML = `
                <div class="row g-4">
                    <div class="col-lg-8">
                        <section class="card border-0 shadow-sm mb-4">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <h2 class="h5 mb-0">Delivery address</h2>
                                    <button id="showAddressForm" class="btn btn-sm btn-dark">Add new address</button>
                                </div>
                                <div id="addressGrid" class="row g-3">
                                    ${data.addresses?.length ? data.addresses.map(a => addressHtml(a, selectedId)).join("") : '<div class="col-12"><div class="alert alert-light border mb-0">Add a delivery address to continue.</div></div>'}
                                </div>
                                <div id="addressForm" class="border rounded p-3 mt-3 d-none">
                                    <h3 class="h6">New address</h3>
                                    <div class="row g-3">
                                        <div class="col-md-6"><label class="form-label">Recipient name</label><input id="recipientName" class="form-control"></div>
                                        <div class="col-md-6"><label class="form-label">Phone</label><input id="phoneNumber" class="form-control" maxlength="10"></div>
                                        <div class="col-12"><label class="form-label">Address line 1</label><input id="addressLine1" class="form-control"></div>
                                        <div class="col-md-6"><label class="form-label">Address line 2</label><input id="addressLine2" class="form-control"></div>
                                        <div class="col-md-6"><label class="form-label">Landmark</label><input id="landmark" class="form-control"></div>
                                        <div class="col-md-4"><label class="form-label">City</label><input id="city" class="form-control"></div>
                                        <div class="col-md-4"><label class="form-label">State</label><input id="state" class="form-control"></div>
                                        <div class="col-md-4"><label class="form-label">Postal code</label><input id="postalCode" class="form-control"></div>
                                        <div class="col-md-6"><label class="form-label">Address type</label><select id="addressType" class="form-select"><option value="HOME">Home</option><option value="WORK">Work</option><option value="OTHER">Other</option></select></div>
                                        <div class="col-md-6 d-flex align-items-end"><div class="form-check mb-2"><input id="defaultAddress" class="form-check-input" type="checkbox"><label class="form-check-label" for="defaultAddress">Make this my default address</label></div></div>
                                    </div>
                                    <div class="d-flex gap-2 mt-3"><button id="saveAddress" class="btn btn-dark">Save address</button><button id="cancelAddress" class="btn btn-outline-secondary">Cancel</button></div>
                                </div>
                            </div>
                        </section>

                        <section class="card border-0 shadow-sm mb-4">
                            <div class="card-body">
                                <h2 class="h5 mb-3">Order items</h2>
                                ${data.cart.items.map(i => `
                                    <div class="d-flex gap-3 border-bottom py-3">
                                        ${i.imageUrl ? `<img src="${CustomerStore.esc(SellzonAPI.catalogAssetUrl(i.imageUrl))}" alt="${CustomerStore.esc(i.title)}" style="width:72px;height:72px;object-fit:contain">` : '<div class="product-placeholder" style="width:72px;height:72px"><i class="bi bi-image"></i></div>'}
                                        <div class="flex-grow-1 min-width-0">
                                            <div class="fw-semibold checkout-title">${CustomerStore.esc(i.title)}</div>
                                            <div class="small text-secondary">${CustomerStore.esc(i.brand)} · Qty ${i.quantity}</div>
                                            <div class="fw-bold mt-1">${CustomerStore.money(i.sellingPrice)}</div>
                                        </div>
                                        <div class="fw-bold">${CustomerStore.money(i.itemTotal)}</div>
                                    </div>`).join("")}
                            </div>
                        </section>

                        <section class="card border-0 shadow-sm">
                            <div class="card-body">
                                <h2 class="h5 mb-3">Payment method</h2>

                                <div class="form-check border rounded p-3 mb-2">
                                    <input class="form-check-input payment-method" type="radio"
                                           name="paymentMethod" id="paymentCod" value="COD" checked>
                                    <label class="form-check-label w-100" for="paymentCod">
                                        <strong>Cash on Delivery</strong>
                                        <div class="small text-secondary">Pay when your order is delivered.</div>
                                    </label>
                                </div>

                                <div class="form-check border rounded p-3">
                                    <input class="form-check-input payment-method" type="radio"
                                           name="paymentMethod" id="paymentOnline" value="ONLINE">
                                    <label class="form-check-label w-100" for="paymentOnline">
                                        <strong>Pay Online</strong>
                                        <div class="small text-secondary">UPI, cards, net banking and other Razorpay-supported methods.</div>
                                    </label>
                                </div>
                            </div>
                        </section>
                    </div>

                    <div class="col-lg-4">
                        <div class="card border-0 shadow-sm sticky-lg-top" style="top:90px">
                            <div class="card-body">
                                <h2 class="h5">Order summary</h2>
                                <div class="d-flex justify-content-between mt-3"><span>Items</span><span>${data.cart.totalItems}</span></div>
                                <div class="d-flex justify-content-between mt-2"><span>Subtotal</span><span>${CustomerStore.money(data.cart.subtotal)}</span></div>
                                <div class="d-flex justify-content-between mt-2"><span>Shipping</span><span>${CustomerStore.money(data.shippingAmount)}</span></div>
                                <div class="d-flex justify-content-between mt-2"><span>Discount</span><span>${CustomerStore.money(data.discountAmount)}</span></div>
                                <hr>
                                <div class="d-flex justify-content-between fs-5 fw-bold"><span>Total</span><span>${CustomerStore.money(data.totalAmount)}</span></div>
                                <button id="placeOrder" class="btn btn-dark w-100 mt-4" ${selectedId ? "" : "disabled"}>Place Order</button>
                            </div>
                        </div>
                    </div>
                </div>`;

            bindEvents();
        } finally {
            loading.classList.add("d-none");
            root.classList.remove("d-none");
        }
    }

    function bindEvents() {
        document.getElementById("showAddressForm")?.addEventListener("click", () => document.getElementById("addressForm").classList.remove("d-none"));
        document.getElementById("cancelAddress")?.addEventListener("click", () => document.getElementById("addressForm").classList.add("d-none"));

        document.querySelectorAll(".make-default").forEach(button => button.addEventListener("click", async () => {
            const { response, data } = await SellzonAPI.json(`/api/customer/addresses/${button.dataset.id}/default`, { method: "PATCH" });
            if (!response.ok) return alertBox(data?.message || "Unable to set default address.");
            await load();
        }));

        document.querySelectorAll(".delete-address").forEach(button => button.addEventListener("click", async () => {
            if (!confirm("Delete this address?")) return;
            const { response, data } = await SellzonAPI.json(`/api/customer/addresses/${button.dataset.id}`, { method: "DELETE" });
            if (!response.ok) return alertBox(data?.message || "Unable to delete address.");
            await load();
        }));

        document.getElementById("saveAddress")?.addEventListener("click", saveAddress);
        document.getElementById("placeOrder")?.addEventListener("click", placeOrder);
    }

    async function saveAddress() {
        const payload = {
            recipientName: document.getElementById("recipientName").value.trim(),
            phoneNumber: document.getElementById("phoneNumber").value.trim(),
            addressLine1: document.getElementById("addressLine1").value.trim(),
            addressLine2: document.getElementById("addressLine2").value.trim() || null,
            landmark: document.getElementById("landmark").value.trim() || null,
            city: document.getElementById("city").value.trim(),
            state: document.getElementById("state").value.trim(),
            postalCode: document.getElementById("postalCode").value.trim(),
            country: "India",
            addressType: document.getElementById("addressType").value,
            defaultAddress: document.getElementById("defaultAddress").checked
        };
        if (!payload.recipientName || !/^[6-9]\d{9}$/.test(payload.phoneNumber) || !payload.addressLine1 || !payload.city || !payload.state || !payload.postalCode) {
            alertBox("Please enter all required address fields and a valid 10-digit Indian phone number.");
            return;
        }
        const { response, data } = await SellzonAPI.json("/api/customer/addresses", { method: "POST", body: JSON.stringify(payload) });
        if (!response.ok) return alertBox(data?.message || "Unable to save address.");
        await load();
    }

    async function placeOrder() {
        const selected = document.querySelector("input[name='addressId']:checked")?.value;
        const paymentMethod = document.querySelector("input[name='paymentMethod']:checked")?.value;

        if (!selected) return alertBox("Please select a delivery address.");
        if (!paymentMethod) return alertBox("Please select a payment method.");

        const button = document.getElementById("placeOrder");
        button.disabled = true;
        button.textContent = paymentMethod === "ONLINE"
            ? "Preparing payment..."
            : "Placing order...";

        try {
            const clientRequestId = crypto.randomUUID
                ? crypto.randomUUID()
                : `${Date.now()}-${Math.random()}`;

            const { response, data } = await SellzonAPI.json("/api/customer/orders", {
                method: "POST",
                body: JSON.stringify({
                    addressId: selected,
                    paymentMethod,
                    clientRequestId
                })
            });

            if (!response.ok) {
                throw new Error(data?.message || "Unable to place order.");
            }

            if (paymentMethod === "COD") {
                window.location.href =
                    `/customer/order-details.html?id=${encodeURIComponent(data.id)}`;
                return;
            }

            const paymentResult = await SellzonAPI.json("/api/customer/payments/razorpay/order", {
                method: "POST",
                body: JSON.stringify({ orderId: data.id })
            });

            if (!paymentResult.response.ok) {
                throw new Error(
                    paymentResult.data?.message || "Unable to start online payment."
                );
            }

            const payment = paymentResult.data;

            if (!window.Razorpay) {
                throw new Error("Razorpay Checkout could not be loaded. Please refresh and try again.");
            }

            const options = {
                key: payment.keyId,
                amount: payment.amountInPaise,
                currency: payment.currency,
                name: "SELLZON",
                description: `Payment for ${payment.orderNumber}`,
                order_id: payment.razorpayOrderId,

                handler: async function (razorpayResponse) {
                    try {
                        button.textContent = "Verifying payment...";

                        const verification = await SellzonAPI.json(
                            "/api/customer/payments/razorpay/verify",
                            {
                                method: "POST",
                                body: JSON.stringify({
                                    orderId: data.id,
                                    razorpayOrderId: razorpayResponse.razorpay_order_id,
                                    razorpayPaymentId: razorpayResponse.razorpay_payment_id,
                                    razorpaySignature: razorpayResponse.razorpay_signature
                                })
                            }
                        );

                        if (!verification.response.ok) {
                            throw new Error(
                                verification.data?.message || "Payment verification failed."
                            );
                        }

                        window.location.href =
                            `/customer/order-details.html?id=${encodeURIComponent(data.id)}`;

                    } catch (e) {
                        alertBox(e.message);
                        button.disabled = false;
                        button.textContent = "Pay Online";
                    }
                },

                modal: {
                    ondismiss: function () {
                        button.disabled = false;
                        button.textContent = "Pay Online";
                        alertBox(
                            "Payment window closed. Your order is still pending and you can retry payment."
                        );
                    }
                }
            };

            const razorpay = new Razorpay(options);

            razorpay.on("payment.failed", async function (response) {
                const reason =
                    response?.error?.description ||
                    response?.error?.reason ||
                    "Payment failed";

                try {
                    await SellzonAPI.json(
                        `/api/customer/payments/razorpay/failed?razorpayOrderId=${encodeURIComponent(payment.razorpayOrderId)}&reason=${encodeURIComponent(reason)}`,
                        { method: "POST" }
                    );
                } catch (_) {
                    // Payment Service can reconcile the final status later.
                }

                alertBox(`Payment failed: ${reason}`);
                button.disabled = false;
                button.textContent = "Pay Online";
            });

            razorpay.open();

        } catch (e) {
            alertBox(e.message);
            button.disabled = false;
            button.textContent = paymentMethod === "ONLINE"
                ? "Pay Online"
                : "Place Order";
        }
    }

    try { await load(); }
    catch (e) { loading.classList.add("d-none"); root.classList.remove("d-none"); root.innerHTML = `<div class="alert alert-danger">${CustomerStore.esc(e.message)}</div>`; }
});
