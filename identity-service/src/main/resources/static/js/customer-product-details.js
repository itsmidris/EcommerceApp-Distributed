document.addEventListener("DOMContentLoaded", async () => {
    const id = new URLSearchParams(location.search).get("id");
    const root = document.getElementById("productDetail");

    if (!id) {
        root.innerHTML = '<div class="alert alert-danger">Product not found.</div>';
        return;
    }

    try {
        const { response, data } = await SellzonAPI.json(`/api/products/${encodeURIComponent(id)}`);

        if (!response.ok) throw new Error(data?.message || "Product not found.");

        const d = CustomerStore.discount(data.mrp, data.sellingPrice);
        let selected = data.images?.[0];

        root.innerHTML = `
            <div class="row g-4">
                <div class="col-lg-6">
                    <div id="mainImage">
                        ${CustomerStore.image(selected?.imageUrl, "detail-main-image", data.title)}
                    </div>
                    <div class="d-flex gap-2 flex-wrap mt-3" id="thumbs">
                        ${(data.images || []).map((im, i) => `
                            <img src="${CustomerStore.esc(im.imageUrl)}" class="thumb ${i === 0 ? 'active' : ''}" data-url="${CustomerStore.esc(im.imageUrl)}" alt="">
                        `).join("")}
                    </div>
                </div>
                <div class="col-lg-6">
                    <div class="small text-secondary">${CustomerStore.esc(data.categoryPath)}</div>
                    <div class="small text-secondary mt-3">${CustomerStore.esc(data.brand)}</div>
                    <h1 class="display-6 fw-semibold mt-1">${CustomerStore.esc(data.title)}</h1>
                    <p class="text-secondary">Sold by ${CustomerStore.esc(data.sellerName)}</p>
                    <hr>
                    <div>
                        <span class="display-6 fw-bold">${CustomerStore.money(data.sellingPrice)}</span>
                        ${d ? `<span class="discount ms-2">${d}% off</span>` : ""}
                    </div>
                    <div class="mrp fs-5">${CustomerStore.money(data.mrp)}</div>
                    <div class="mt-3 ${data.stockQuantity > 0 ? 'text-success' : 'text-danger'}">
                        ${data.stockQuantity > 0 ? `${data.stockQuantity} available` : 'Currently unavailable'}
                    </div>
                    <div class="d-flex gap-2 mt-4">
                        <button id="addCart" class="btn btn-dark btn-lg" ${data.stockQuantity < 1 ? 'disabled' : ''}>Add to Cart</button>
                        <button id="buyNow" class="btn btn-outline-dark btn-lg" ${data.stockQuantity < 1 ? 'disabled' : ''}>Buy Now</button>
                    </div>
                </div>
            </div>
            <hr class="my-5">
            <div class="row g-4">
                <div class="col-lg-7">
                    <h2 class="h4">About this product</h2>
                    <p class="text-secondary">${CustomerStore.esc(data.description || 'No description available.')}</p>
                </div>
                <div class="col-lg-5">
                    <h2 class="h4">Product details</h2>
                    <table class="table attribute-table">
                        <tbody>
                            ${(data.attributes || []).map(a => `
                                <tr>
                                    <td>${CustomerStore.esc(a.name)}</td>
                                    <td>${CustomerStore.esc(a.value || ((a.selectedOptions || []).join(', ')) || '-')}${a.unit ? ' ' + CustomerStore.esc(a.unit) : ''}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        document.querySelectorAll(".thumb").forEach(t => t.addEventListener("click", () => {
            document.getElementById("mainImage").innerHTML = CustomerStore.image(t.dataset.url, "detail-main-image", data.title);
            document.querySelectorAll('.thumb').forEach(x => x.classList.remove('active'));
            t.classList.add('active');
        }));

        document.getElementById("addCart")?.addEventListener("click", async () => {
            try {
                await CustomerStore.addToCart(id);
                await CustomerStore.loadCartCount();
                location.href = '/customer/cart.html';
            } catch (e) {
                alert(e.message);
            }
        });

        document.getElementById("buyNow")?.addEventListener("click", async () => {
            try {
                await CustomerStore.addToCart(id);
                location.href = '/customer/checkout.html';
            } catch (e) {
                alert(e.message);
            }
        });
        } catch (e) {
            root.innerHTML = `<div class="alert alert-danger">${CustomerStore.esc(e.message)}</div>`;
        }

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