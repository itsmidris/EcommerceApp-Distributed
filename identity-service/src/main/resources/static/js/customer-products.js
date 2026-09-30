document.addEventListener("DOMContentLoaded", async () => {
    const grid = document.getElementById("productGrid");
    const loading = document.getElementById("productsLoading");
    const empty = document.getElementById("productsEmpty");

    const params = new URLSearchParams(location.search);
    let page = Number(params.get("page") || 0);
    const categoryId = params.get("categoryId") || "";
    const q = params.get("q") || "";

    const search = document.getElementById("catalogSearch");
    if (search) search.value = q;

    try {
        const url = new URL("/api/products", location.origin);
        url.searchParams.set("page", page);
        url.searchParams.set("size", "24");
        if (categoryId) url.searchParams.set("categoryId", categoryId);
        if (q) url.searchParams.set("q", q);

        const { response, data } = await SellzonAPI.json(url.pathname + url.search);

        if (!response.ok) throw new Error(data?.message || "Unable to load products.");

        loading.classList.add("d-none");

        if (!data.content?.length) {
            empty.classList.remove("d-none");
            return;
        }

        grid.innerHTML = data.content.map(p => {
            const d = CustomerStore.discount(p.mrp, p.sellingPrice);

            return `
                <div class="col-6 col-md-4 col-xl-3">
                    <article class="product-card">
                        <a href="/customer/product-details.html?id=${encodeURIComponent(p.marketplaceProductId)}" class="text-decoration-none text-dark">
                            ${CustomerStore.image(p.primaryImageUrl, "product-image", p.title)}
                            <div class="p-3">
                                <div class="small text-secondary">${CustomerStore.esc(p.brand)}</div>
                                <h2 class="h6 mt-1 mb-2">${CustomerStore.esc(p.title)}</h2>
                                <div>
                                    <span class="price">${CustomerStore.money(p.sellingPrice)}</span>
                                    ${d ? `<span class="discount ms-1">${d}% off</span>` : ""}
                                </div>
                                <div class="mrp">${CustomerStore.money(p.mrp)}</div>
                            </div>
                        </a>
                        <div class="px-3 pb-3">
                            <button class="btn btn-dark w-100 add-cart" data-id="${CustomerStore.esc(p.marketplaceProductId)}" ${p.stockQuantity < 1 ? "disabled" : ""}>
                                ${p.stockQuantity < 1 ? "Out of stock" : "Add to Cart"}
                            </button>
                        </div>
                    </article>
                </div>
            `;
        }).join("");

        grid.querySelectorAll(".add-cart").forEach(btn => btn.addEventListener("click", async () => {
            btn.disabled = true;

            try {
                await CustomerStore.addToCart(btn.dataset.id);
                btn.textContent = "Added to Cart";
                await CustomerStore.loadCartCount();
            } catch (e) {
                alert(e.message);
                btn.disabled = false;
            }
        }));

        const pagination = document.getElementById("pagination");

        if (data.totalPages > 1) {
            pagination.innerHTML = Array.from({ length: data.totalPages }, (_, i) => `
                <li class="page-item ${i === data.page ? 'active' : ''}">
                    <a class="page-link" href="/customer/products.html?${new URLSearchParams({
                        ...(q ? { q } : {}),
                        ...(categoryId ? { categoryId } : {}),
                        page: i
                    })}">
                        ${i + 1}
                    </a>
                </li>
            `).join("");
        }
    } catch (e) {
        loading.classList.add("d-none");
        empty.classList.remove("d-none");
        empty.innerHTML = `<div class="text-danger">${CustomerStore.esc(e.message)}</div>`;
    }

    const form = document.getElementById("catalogSearchForm");

    if (form) form.addEventListener("submit", e => {
        e.preventDefault();
        const value = document.getElementById("catalogSearch")?.value.trim() || "";
        location.href = `/customer/products.html${value ? `?q=${encodeURIComponent(value)}` : ""}`;
    });
});