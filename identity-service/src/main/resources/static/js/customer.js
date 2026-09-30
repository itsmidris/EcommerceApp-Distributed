const CustomerStore = (() => {
    function money(value) {
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 2
        }).format(Number(value || 0));
    }

    function discount(mrp, price) {
        const m = Number(mrp);
        const p = Number(price);

        if (!m || m <= p) return 0;

        return Math.round(((m - p) / m) * 100);
    }

    function esc(value) {
        return String(value ?? "").replace(/[&<>'"]/g, c => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            "'": "&#39;",
            '"': "&quot;"
        }[c]));
    }

    function image(url, cls = "product-image", alt = "") {
        return url
            ? `<img class="${cls}" src="${esc(url)}" alt="${esc(alt)}" loading="lazy">`
            : `<div class="product-placeholder"><i class="bi bi-image"></i></div>`;
    }

    function loginFor(path) {
        window.location.href = `/customer/login.html?redirect=${encodeURIComponent(path)}`;
    }

    async function addToCart(id, quantity = 1) {
        if (!SellzonAPI.isAuthenticated()) {
            loginFor(`/customer/product-details.html?id=${encodeURIComponent(id)}`);
            return false;
        }

        const { response, data } = await SellzonAPI.json("/api/customer/cart/items", {
            method: "POST",
            body: JSON.stringify({ marketplaceProductId: id, quantity })
        });

        if (!response.ok) throw new Error(data?.message || "Unable to add product to cart.");

        return data;
    }

    async function loadCategories() {
        const nav = document.getElementById("categoryNav");
        if (!nav) return;

        const { response, data } = await SellzonAPI.json("/api/categories/tree");
        if (!response.ok || !Array.isArray(data)) return;

        nav.innerHTML = data
            .map(c => `<a href="/customer/products.html?categoryId=${c.id}">${esc(c.name)}</a>`)
            .join("");
    }

    function setCartCount(count) {
        document.querySelectorAll("[data-cart-count]").forEach(e => e.textContent = String(count ?? 0));
    }

    async function loadCartCount() {
        if (!SellzonAPI.isAuthenticated()) return setCartCount(0);

        try {
            const { response, data } = await SellzonAPI.json("/api/customer/cart");
            if (response.ok) setCartCount(data?.totalItems || 0);
        } catch (_) {}
    }

    function syncAuthNavigation() {

        const authenticated = SellzonAPI.isAuthenticated();

        const loginLink =
            document.getElementById("loginLink");

        const accountLink =
            document.getElementById("accountLink");

        let logoutButton =
            document.querySelector("[data-logout]");


        /*
         * LOGGED OUT
         */
        if (!authenticated) {

            loginLink?.classList.remove("d-none");

            accountLink?.classList.add("d-none");

            logoutButton?.classList.add("d-none");

            return;
        }


        /*
         * LOGGED IN
         */

        loginLink?.classList.add("d-none");

        accountLink?.classList.remove("d-none");


        /*
         * If the page doesn't already have a logout button,
         * create one.
         */
        if (!logoutButton) {

            logoutButton =
                document.createElement("button");

            logoutButton.type = "button";

            logoutButton.className =
                "btn btn-outline-secondary btn-sm";

            logoutButton.setAttribute(
                "data-logout",
                ""
            );

            logoutButton.textContent = "Logout";

            /*
             * Put Logout after Account/Cart area.
             */
            const navigation =
                accountLink?.parentElement;

            if (navigation) {
                navigation.appendChild(logoutButton);
            }
        }


        logoutButton.classList.remove("d-none");


        /*
         * Prevent attaching the logout handler
         * more than once.
         */
        if (logoutButton.dataset.logoutBound === "true") {
            return;
        }

        logoutButton.dataset.logoutBound = "true";


        logoutButton.addEventListener(
            "click",
            async () => {

                logoutButton.disabled = true;

                try {

                    await SellzonAPI.logout();

                } finally {

                    window.location.href =
                        "/customer/index.html";
                }
            }
        );
    }

    return {
        money,
        discount,
        esc,
        image,
        loginFor,
        addToCart,
        loadCategories,
        setCartCount,
        loadCartCount,
        syncAuthNavigation
    };
})();

document.addEventListener("DOMContentLoaded", async () => {

    CustomerStore.loadCategories().then(CustomerStore.loadCartCount);

    CustomerStore.syncAuthNavigation();

    const searchForm = document.getElementById("catalogSearchForm");
    const searchInput = document.getElementById("catalogSearch");

    if (searchForm && searchInput) {
        searchForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const value = searchInput.value.trim();

            if (!value) {
                window.location.href = "/customer/products.html";
                return;
            }

            window.location.href =
                `/customer/products.html?q=${encodeURIComponent(value)}`;
        });
    }
});