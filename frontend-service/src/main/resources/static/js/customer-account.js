document.addEventListener("DOMContentLoaded", async () => {
    /* =====================================================
       AUTHENTICATION
       ===================================================== */
    if (!SellzonAPI.requireRole("CUSTOMER")) {
        return;
    }

    /* =====================================================
       USER
       ===================================================== */
    const user = SellzonAPI.getUser() || {};
    const email = user.email || "Customer";
    const initial = email.charAt(0).toUpperCase() || "S";

    const esc = value => CustomerStore.esc(value);

    /* =====================================================
       PROFILE HEADER
       ===================================================== */
    document.getElementById("welcomeName").textContent =
        `Welcome back, ${email.split("@")[0]}`;

    document.getElementById("sidebarEmail").textContent = email;
    document.getElementById("accountAvatar").textContent = initial;
    document.getElementById("sidebarAvatar").textContent = initial;
    document.getElementById("profileAvatar").textContent = initial;
    document.getElementById("profileEmail").textContent = email;
    document.getElementById("profileEmailField").textContent = email;
    document.getElementById("profileUserId").textContent = user.userId || "-";
    document.getElementById("profileRole").textContent = user.role || "CUSTOMER";

    /* =====================================================
       SECTION NAVIGATION
       ===================================================== */
    document.querySelectorAll("[data-section]").forEach(control => {
        control.addEventListener("click", () => {
            const target = control.dataset.section;

            /* Hide all sections */
            document.querySelectorAll(".account-section").forEach(section => {
                section.classList.toggle(
                    "active",
                    section.id === `section-${target}`
                );
            });

            /* Update sidebar active state */
            document
                .querySelectorAll(".account-nav-item[data-section]")
                .forEach(item => {
                    item.classList.toggle(
                        "active",
                        item.dataset.section === target
                    );
                });

            window.scrollTo({
                top: 0,
                behavior: "smooth"
            });
        });
    });

    /* =====================================================
       SEARCH
       ===================================================== */
    document
        .getElementById("accountSearchForm")
        ?.addEventListener("submit", event => {
            event.preventDefault();

            const value = document
                .getElementById("accountSearch")
                .value
                .trim();

            if (value) {
                window.location.href =
                    `/customer/products.html?q=${encodeURIComponent(value)}`;
            } else {
                window.location.href = "/customer/products.html";
            }
        });

    /* =====================================================
       CART
       ===================================================== */
    try {
        const { response, data } = await SellzonAPI.json("/api/customer/cart");

        if (response.ok) {
            const count = Number(data?.totalItems || 0);

            document.getElementById("cartCount").textContent = count;

            /* Update all cart badges */
            CustomerStore.setCartCount(count);
        } else {
            document.getElementById("cartCount").textContent = "-";
        }
    } catch (_) {
        document.getElementById("cartCount").textContent = "-";
    }

    /* =====================================================
       ORDERS
       ===================================================== */
    try {
        const { response, data } = await SellzonAPI.json("/api/customer/orders");

        if (!response.ok) {
            throw new Error(data?.message || "Unable to load orders.");
        }

        /*
         * Current backend may return:
         *
         * [
         *    ...
         * ]
         *
         * or
         *
         * {
         *    content: [...]
         * }
         */
        const orders = Array.isArray(data)
            ? data
            : Array.isArray(data?.content)
              ? data.content
              : [];

        document.getElementById("ordersCount").textContent = orders.length;

        const root = document.getElementById("recentOrders");

        /* =================================================
           EMPTY
           ================================================= */
        if (!orders.length) {
            root.innerHTML = `
                <div class="text-center py-4 text-secondary">
                    <i class="bi bi-bag-x fs-2 d-block mb-2"></i>
                    <div class="small">
                        You have not placed any orders yet.
                    </div>
                    <a
                        href="/customer/products.html"
                        class="btn btn-dark btn-sm mt-3"
                    >
                        Start Shopping
                    </a>
                </div>
            `;
            return;
        }

        /* =================================================
           RECENT ORDERS
           ================================================= */
        root.innerHTML = orders
            .slice(0, 5)
            .map(order => {
                const id = order.id || order.orderId || "";
                const number = order.orderNumber || "Order";
                const status = order.orderStatus || order.status || "PLACED";
                const amount = order.totalAmount ?? order.total ?? 0;
                const items = order.itemCount ?? order.items?.length ?? 0;

                return `
                    <div class="account-order">
                        <div class="order-main">
                            <div class="order-number">
                                ${esc(number)}
                            </div>
                            <div class="order-meta">
                                ${esc(items)}
                                item${Number(items) === 1 ? "" : "s"}
                                ${
                                    order.paymentMethod
                                        ? ` · ${esc(order.paymentMethod)}`
                                        : ""
                                }
                            </div>
                        </div>

                        <span class="order-status">
                            ${esc(status)}
                        </span>

                        <div class="order-amount">
                            ${CustomerStore.money(amount)}
                        </div>

                        ${
                            id
                                ? `
                                    <a
                                        class="btn btn-outline-dark btn-sm"
                                        href="/customer/order-details.html?id=${encodeURIComponent(id)}"
                                    >
                                        View
                                    </a>
                                `
                                : ""
                        }
                    </div>
                `;
            })
            .join("");
    } catch (_) {
        document.getElementById("ordersCount").textContent = "-";

        document.getElementById("recentOrders").innerHTML = `
            <div class="alert alert-light border mb-0">
                Unable to load recent orders right now.
            </div>
        `;
    }

    /* =====================================================
       LOGOUT
       ===================================================== */
    document.querySelectorAll("[data-logout]").forEach(button => {
        button.addEventListener("click", async () => {
            button.disabled = true;

            try {
                await SellzonAPI.logout();
            } finally {
                window.location.href = "/customer/index.html";
            }
        });
    });
});