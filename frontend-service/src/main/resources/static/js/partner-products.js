let products = [];

document.addEventListener("DOMContentLoaded", () => {

    if (!SellzonAPI.requireRole("SELLER")) {
        return;
    }

    loadProducts();

    document.getElementById("searchInput")
        ?.addEventListener("input", renderProducts);

    document.getElementById("statusFilter")
        ?.addEventListener("change", renderProducts);
});


async function loadProducts() {

    const tbody = document.getElementById("productsTableBody");

    try {

        const { response, data } = await SellzonAPI.json(
            "/api/seller/products",
            {
                method: "GET"
            }
        );

        if (!response.ok) {
            throw new Error(
                data?.message || "Unable to load products."
            );
        }

        products = Array.isArray(data) ? data : [];

        updateStats();
        renderProducts();

    } catch (error) {

        console.error("Unable to load products:", error);

        tbody.innerHTML = `
            <tr>
                <td colspan="7"
                    class="text-center py-5 text-danger">
                    ${escapeHtml(error.message)}
                </td>
            </tr>
        `;
    }
}


function updateStats() {

    const total = products.length;

    const active = products.filter(
        product => product.status === "ACTIVE"
    ).length;

    const inactive = products.filter(
        product => product.status === "INACTIVE"
    ).length;

    document.getElementById("totalProducts").textContent = total;
    document.getElementById("activeProducts").textContent = active;
    document.getElementById("inactiveProducts").textContent = inactive;
}


function renderProducts() {

    const tbody = document.getElementById("productsTableBody");
    const empty = document.getElementById("emptyState");

    const query = (
        document.getElementById("searchInput")?.value || ""
    )
        .trim()
        .toLowerCase();

    const status =
        document.getElementById("statusFilter")?.value || "ALL";


    const filtered = products.filter(product => {

        const searchableText = [
            product.title,
            product.sku,
            product.brand,
            product.categoryName,
            product.marketplaceProductId
        ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();

        const matchesSearch =
            !query || searchableText.includes(query);

        const matchesStatus =
            status === "ALL" || product.status === status;

        return matchesSearch && matchesStatus;
    });


    if (!filtered.length) {

        tbody.innerHTML = "";

        empty.classList.remove("d-none");

        return;
    }


    empty.classList.add("d-none");


    tbody.innerHTML = filtered
        .map(product => {

            const inactive =
                product.status === "INACTIVE";

            return `
                <tr>

                    <td>
                        <div class="fw-semibold">
                            ${escapeHtml(product.title || "-")}
                        </div>

                        <div class="small text-secondary">
                            ${escapeHtml(product.brand || "")}
                        </div>

                        <div class="small text-secondary">
                            ID:
                            ${escapeHtml(
                                product.marketplaceProductId || "-"
                            )}
                        </div>
                    </td>


                    <td>
                        <code>
                            ${escapeHtml(product.sku || "-")}
                        </code>
                    </td>


                    <td>
                        ${escapeHtml(
                            product.categoryName || "-"
                        )}
                    </td>


                    <td>
                        <div class="fw-semibold">
                            ₹${money(product.sellingPrice)}
                        </div>

                        <div class="small text-secondary text-decoration-line-through">
                            ₹${money(product.mrp)}
                        </div>
                    </td>


                    <td>
                        ${escapeHtml(
                            String(product.stockQuantity ?? 0)
                        )}
                    </td>


                    <td>
                        <span class="badge ${
                            inactive
                                ? "text-bg-secondary"
                                : "text-bg-success"
                        }">
                            ${escapeHtml(product.status || "-")}
                        </span>
                    </td>


                    <td class="text-end">

                        <div class="btn-group btn-group-sm">

                            <button
                                class="btn btn-outline-primary"
                                title="Edit"
                                onclick="editProduct('${js(
                                    product.marketplaceProductId
                                )}')">

                                <i class="bi bi-pencil"></i>

                            </button>


                            <button
                                class="btn btn-outline-${
                                    inactive ? "success" : "danger"
                                }"
                                title="${
                                    inactive
                                        ? "Activate"
                                        : "Deactivate"
                                }"
                                onclick="toggleProductStatus(
                                    '${js(
                                        product.marketplaceProductId
                                    )}',
                                    ${inactive}
                                )">

                                <i class="bi bi-${
                                    inactive
                                        ? "play"
                                        : "pause"
                                }-circle"></i>

                            </button>

                        </div>

                    </td>

                </tr>
            `;
        })
        .join("");
}


function editProduct(marketplaceProductId) {

    if (!marketplaceProductId) {
        return;
    }

    window.location.href =
        `/partner/edit-product.html?id=${encodeURIComponent(
            marketplaceProductId
        )}`;
}


async function toggleProductStatus(
    marketplaceProductId,
    activate
) {

    if (!marketplaceProductId) {
        return;
    }


    const action =
        activate ? "activate" : "deactivate";


    const confirmed = confirm(
        `Are you sure you want to ${action} this product?`
    );


    if (!confirmed) {
        return;
    }


    try {

        const { response, data } =
            await SellzonAPI.json(
                `/api/seller/products/${encodeURIComponent(
                    marketplaceProductId
                )}/${action}`,
                {
                    method: "PATCH"
                }
            );


        if (!response.ok) {

            throw new Error(
                data?.message ||
                "Unable to update product."
            );
        }


        await loadProducts();

    } catch (error) {

        console.error(
            "Unable to update product:",
            error
        );

        const alert =
            document.getElementById("productsAlert");

        alert.className =
            "alert alert-danger";

        alert.textContent =
            error.message;
    }
}


function money(value) {

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toFixed(2)
        : "0.00";
}


function escapeHtml(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


function js(value) {

    return String(value ?? "")
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'");
}