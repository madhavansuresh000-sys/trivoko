"""
Writes postman/TriVoKo.postman_collection.json (import it in Postman, or run it with newman).

Run:  python tools/postman-gen/make_collection.py      (from the project folder)
Then: npx newman run postman/TriVoKo.postman_collection.json --env-var demoPassword=<DEMO_PASSWORD from .env>
      (needs the backend on localhost:8080 with the Flyway sample data; folder 7 logs in as the demo accounts)
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "postman/TriVoKo.postman_collection.json"


def req(name, method, path, tests, query=None, body=None):
    url = "{{baseUrl}}" + path
    if query:
        url += "?" + "&".join(f"{k}={v}" for k, v in query)
    request = {"method": method, "url": url}
    if method != "GET":
        # CSRF: copy the XSRF-TOKEN cookie (saved by "Get the CSRF cookie") into the header, like Axios does
        request["header"] = [{"key": "X-XSRF-TOKEN", "value": "{{xsrf}}"}]
    if body is not None:
        request["header"].append({"key": "Content-Type", "value": "application/json"})
        request["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2)}
    return {
        "name": name,
        "request": request,
        "event": [{"listen": "test", "script": {"exec": tests}}],
    }


def login(who, extra=None):
    """POST /api/auth/login as a demo account; newman keeps the TRIVOKO_TOKEN cookie for the next requests."""
    return req(f"Login as {who}", "POST", "/api/auth/login",
               [ok, check("it is " + who, f"pm.expect(j.email).to.eql('{who}@trivoko.test')")] + (extra or []),
               body={"email": f"{who}@trivoko.test", "password": "{{demoPassword}}"})


def status(code, text):
    return f"pm.test('{code} {text}', () => pm.response.to.have.status({code}));"


def check(title, expr):
    return f"pm.test({json.dumps(title)}, () => {{ const j = pm.response.json(); {expr}; }});"


ok = status(200, "OK")
folders = [
    ("1. Health", [
        req("Backend is UP", "GET", "/api/health", [
            ok, check("status is UP", "pm.expect(j.status).to.eql('UP')")]),
    ]),
    ("2. Categories", [
        req("Category tree", "GET", "/api/categories", [
            ok,
            check("10 top categories", "pm.expect(j).to.have.length(10)"),
            check("first is Mobiles & Accessories with 3 children",
                  "pm.expect(j[0].name).to.eql('Mobiles & Accessories'); pm.expect(j[0].children).to.have.length(3)"),
        ]),
    ]),
    ("3. Product listing", [
        req("DONE-WHEN: phones by price, page 0", "GET", "/api/products", [
            ok,
            check("4 phones in 2 pages", "pm.expect(j.totalElements).to.eql(4); pm.expect(j.totalPages).to.eql(2)"),
            check("cheapest first", "pm.expect(j.content.map(p => p.name)).to.eql(['Kaveri K5', 'Volta V12 5G'])"),
        ], [("category", "phones"), ("sort", "price"), ("size", "2")]),
        req("DONE-WHEN: phones by price, page 1", "GET", "/api/products", [
            ok,
            check("next two, last page", "pm.expect(j.content.map(p => p.name)).to.eql(['Nimbus N8 Pro', 'Orbit X Ultra']); pm.expect(j.last).to.be.true"),
        ], [("category", "phones"), ("sort", "price"), ("size", "2"), ("page", "1")]),
        req("All live products (newest first)", "GET", "/api/products", [
            ok,
            check("116 live products (3 pending + 1 rejected hidden)", "pm.expect(j.totalElements).to.eql(116)"),
            check("24 per page by default", "pm.expect(j.size).to.eql(24); pm.expect(j.content).to.have.length(24)"),
            check("newest first", "pm.expect(j.content[0].name).to.eql('PlayBox lunch bag')"),
        ]),
        req("Top category includes its sub-categories", "GET", "/api/products", [
            ok, check("12 products", "pm.expect(j.totalElements).to.eql(12)"),
        ], [("category", "mobiles-accessories")]),
        req("Brand filter", "GET", "/api/products", [
            ok,
            check("11 live Arc products", "pm.expect(j.totalElements).to.eql(11)"),
            check("all Arc", "j.content.forEach(p => pm.expect(p.brand).to.eql('Arc'))"),
        ], [("brand", "Arc"), ("size", "48")]),
        req("Price range 500-1000", "GET", "/api/products", [
            ok,
            check("every price inside the range",
                  "pm.expect(j.totalElements).to.be.above(0); j.content.forEach(p => pm.expect(p.priceFrom).to.be.within(500, 1000))"),
        ], [("minPrice", "500"), ("maxPrice", "1000"), ("size", "48")]),
        req("In stock only (cycling)", "GET", "/api/products", [
            ok,
            check("sold-out lights hidden", "pm.expect(j.totalElements).to.eql(2); pm.expect(j.content.map(p => p.slug)).to.not.include('velo-bicycle-lights-set')"),
        ], [("category", "cycling"), ("inStock", "true")]),
        req("Shop filter (Pondy Books)", "GET", "/api/products", [
            ok, check("12 books", "pm.expect(j.totalElements).to.eql(12)"),
        ], [("seller", "pondy-books")]),
        req("Most expensive first", "GET", "/api/products", [
            ok, check("Zentra Pro 16 first", "pm.expect(j.content[0].name).to.eql('Zentra Pro 16')"),
        ], [("sort", "price"), ("dir", "desc")]),
        req("Page size is capped at 48", "GET", "/api/products", [
            ok, check("size 48", "pm.expect(j.size).to.eql(48)"),
        ], [("size", "500")]),
        req("Unknown sort -> 400", "GET", "/api/products", [status(400, "Bad Request")], [("sort", "popular")]),
        req("minPrice > maxPrice -> 400", "GET", "/api/products", [status(400, "Bad Request")],
            [("minPrice", "1000"), ("maxPrice", "500")]),
        req("Unknown category -> 404", "GET", "/api/products", [status(404, "Not Found")], [("category", "spaceships")]),
    ]),
    ("4. Product page", [
        req("iPhone 15 case (Ravi's order)", "GET", "/api/products/silicone-case-for-iphone-15", [
            ok,
            check("3 colours", "pm.expect(j.variants).to.have.length(3)"),
            check("sold by Chennai Mobiles", "pm.expect(j.seller.slug).to.eql('chennai-mobiles')"),
            check("breadcrumb", "pm.expect(j.category.slug).to.eql('cases-covers'); pm.expect(j.category.parent.slug).to.eql('mobiles-accessories')"),
            check("exact stock is never sent", "j.variants.forEach(v => pm.expect(v).to.not.have.property('stock'))"),
        ]),
        req("Low stock shows 'only 3 left'", "GET", "/api/products/salem-steel-idli-maker", [
            ok, check("onlyLeft = 3", "pm.expect(j.variants[0].onlyLeft).to.eql(3)"),
        ]),
        req("Pending product -> 404", "GET", "/api/products/arc-soundbar-2-1", [status(404, "Not Found")]),
    ]),
    ("5. Shop page", [
        req("Chennai Mobiles", "GET", "/api/sellers/chennai-mobiles", [
            ok, check("shop name and city", "pm.expect(j.shopName).to.eql('Chennai Mobiles'); pm.expect(j.city).to.eql('Chennai')"),
        ]),
        req("Pending shop -> 404", "GET", "/api/sellers/erode-organics", [status(404, "Not Found")]),
    ]),
    ("7. Accounts & roles (Phase 2)", [
        req("Get the CSRF cookie", "GET", "/api/auth/csrf", [
            status(204, "No Content"),
            "pm.test('XSRF-TOKEN cookie saved', () => { const t = pm.cookies.get('XSRF-TOKEN'); "
            "pm.expect(t).to.be.a('string'); pm.collectionVariables.set('xsrf', t); });",
        ]),
        login("kovai.sports", [check("a seller", "pm.expect(j.roles).to.include('SELLER')")]),
        req("Who am I (Kovai Sports)", "GET", "/api/auth/me", [
            ok, check("my shop is approved", "pm.expect(j.seller.slug).to.eql('kovai-sports'); pm.expect(j.seller.status).to.eql('APPROVED')"),
        ]),
        req("Create a product (DRAFT)", "POST", "/api/seller/products", [
            status(201, "Created"),
            check("DRAFT with the cheapest price", "pm.expect(j.status).to.eql('DRAFT'); pm.expect(j.priceFrom).to.eql(799)"),
            "pm.collectionVariables.set('productId', pm.response.json().id);",
        ], body={"name": "Newman Practice Cricket Ball", "categoryId": "{{cricketCategory}}", "brand": "Kovai",
                 "description": "Made by the Postman checks; the admin rejects it, so it never goes live.",
                 "variants": [{"label": "Red leather", "colour": "Red", "price": 799, "mrp": 999, "stock": 10},
                              {"label": "White leather", "colour": "White", "price": 899, "mrp": 1099, "stock": 6}]}),
        req("Submit it for approval", "POST", "/api/seller/products/{{productId}}/submit", [
            ok, check("PENDING", "pm.expect(j.status).to.eql('PENDING')"),
        ]),
        login("admin"),
        req("Admin rejects it (keeps the shop counts unchanged)", "POST", "/api/admin/products/{{productId}}/reject", [
            ok, check("REJECTED with the reason", "pm.expect(j.status).to.eql('REJECTED'); pm.expect(j.rejectionReason).to.eql('Practice product from the Postman checks')"),
        ], body={"reason": "Practice product from the Postman checks"}),
        login("chennai.mobiles"),
        req("DONE-WHEN: another shop's product -> 403", "GET", "/api/seller/products/{{productId}}", [
            status(403, "Forbidden"),
        ]),
        login("erode.organics"),
        req("DONE-WHEN: unapproved shop cannot create -> 403", "POST", "/api/seller/products", [
            status(403, "Forbidden"),
        ], body={"name": "Not allowed", "categoryId": "{{cricketCategory}}", "brand": "Erode",
                 "description": "Should never be saved", "variants": [{"label": "1 kg", "price": 100, "mrp": 120, "stock": 1}]}),
        req("Logout", "POST", "/api/auth/logout", [status(204, "No Content")]),
        req("Who am I after logout -> 401", "GET", "/api/auth/me", [status(401, "Unauthorized")]),
    ]),
    ("8. Search & cart (Phase 3)", [
        req("Search 'volta cover'", "GET", "/api/products", [
            ok, check("every result is a Volta cover",
                      "pm.expect(j.content.length).to.be.above(0); j.content.forEach(p => { "
                      "pm.expect(p.name.toLowerCase()).to.include('cover'); pm.expect(p.name.toLowerCase()).to.include('volta'); })"),
        ], query=[("q", "volta%20cover")]),
        req("Brands of Mobile phones", "GET", "/api/products/brands", [
            ok, check("4 brands, sorted", "pm.expect(j.map(b => b.brand)).to.eql(['Kaveri', 'Nimbus', 'Orbit', 'Volta'])"),
        ], query=[("category", "phones")]),
        req("A Chennai Mobiles variant", "GET", "/api/products/volta-v12-5g", [
            ok, "pm.collectionVariables.set('chennaiVariant', pm.response.json().variants.find(v => v.inStock).id);",
        ]),
        req("A Kovai Sports variant", "GET", "/api/products/kovai-run-swift-running-shoes", [
            ok, "pm.collectionVariables.set('kovaiVariant', pm.response.json().variants.find(v => v.inStock).id);",
        ]),
        req("Guest cart preview (no login, 2 sellers)", "POST", "/api/cart/preview", [
            ok,
            check("2 packages, 3 items", "pm.expect(j.packages).to.have.length(2); pm.expect(j.itemCount).to.eql(3)"),
            check("delivery rule: both packages >= 499 are free", "pm.expect(j.shippingTotal).to.eql(0)"),
        ], body={"items": [{"variantId": "{{chennaiVariant}}", "quantity": 1},
                           {"variantId": "{{kovaiVariant}}", "quantity": 2}]}),
        req("My cart without login -> 401", "GET", "/api/cart", [status(401, "Unauthorized")]),
        login("ravi"),
        # start clean, so the run gives the same numbers every time
        req("Clean start: remove line 1", "DELETE", "/api/cart/items/{{chennaiVariant}}", [ok]),
        req("Clean start: remove line 2", "DELETE", "/api/cart/items/{{kovaiVariant}}", [ok]),
        req("DONE-WHEN: merge the guest cart after login", "POST", "/api/cart/merge", [
            ok, check("kept: 2 packages, 3 items", "pm.expect(j.packages).to.have.length(2); pm.expect(j.itemCount).to.eql(3)"),
        ], body={"items": [{"variantId": "{{chennaiVariant}}", "quantity": 1},
                           {"variantId": "{{kovaiVariant}}", "quantity": 2}]}),
        req("Set quantity 3", "PUT", "/api/cart/items/{{chennaiVariant}}", [
            ok, check("5 items now", "pm.expect(j.itemCount).to.eql(5)"),
        ], body={"quantity": 3}),
        req("Quantity 11 -> 400", "PUT", "/api/cart/items/{{chennaiVariant}}", [status(400, "Bad Request")], body={"quantity": 11}),
        req("My cart is saved", "GET", "/api/cart", [
            ok, check("Chennai Mobiles first (added first)", "pm.expect(j.packages[0].seller.slug).to.eql('chennai-mobiles')"),
        ]),
        req("Remove the Kovai line", "DELETE", "/api/cart/items/{{kovaiVariant}}", [
            ok, check("1 package left", "pm.expect(j.packages).to.have.length(1)"),
        ]),
        req("Remove the last line", "DELETE", "/api/cart/items/{{chennaiVariant}}", [
            ok, check("cart is empty again", "pm.expect(j.itemCount).to.eql(0)"),
        ]),
        req("Logout", "POST", "/api/auth/logout", [status(204, "No Content")]),
    ]),
    ("9. Checkout (Phase 4, fake payment page)", [
        login("ravi"),
        # reuse Ravi's first address; add one only when he has none (max 5 per customer)
        req("Ravi's addresses", "GET", "/api/me/addresses", [
            ok, "const a = pm.response.json(); pm.collectionVariables.set('addressId', a.length ? a[0].id : '');",
        ]),
        req("An address for Ravi (only if he has none)", "POST", "/api/me/addresses", [
            "pm.test('201 Created or 409 (already 5)', () => pm.expect(pm.response.code).to.be.oneOf([201, 409]));",
            "if (!pm.collectionVariables.get('addressId') && pm.response.code === 201) "
            "pm.collectionVariables.set('addressId', pm.response.json().id);",
        ], body={"name": "Ravi Kumar", "phone": "9000000002", "line1": "12 Anna Salai", "city": "Chennai",
                 "state": "Tamil Nadu", "pincode": "600002"}),
        req("Chennai line in the cart", "PUT", "/api/cart/items/{{chennaiVariant}}", [ok], body={"quantity": 2}),
        req("Kovai line in the cart", "PUT", "/api/cart/items/{{kovaiVariant}}", [ok], body={"quantity": 1}),
        req("Checkout preview with WELCOME10", "POST", "/api/checkout/preview", [
            ok,
            check("2 packages, coupon applied", "pm.expect(j.packages).to.have.length(2); pm.expect(j.coupon.error).to.be.undefined"),
            check("shares add up to the discount",
                  "const s = j.packages.flatMap(p => p.items).reduce((a, i) => a + i.discountShare, 0); "
                  "pm.expect(Math.round(s * 100)).to.eql(Math.round(j.discountTotal * 100))"),
            "pm.collectionVariables.set('expectedTotal', pm.response.json().grandTotal);",
        ], body={"addressId": "{{addressId}}", "couponCode": "WELCOME10"}),
        req("Wrong total -> 409 prices changed", "POST", "/api/orders", [status(409, "Conflict")],
            body={"addressId": "{{addressId}}", "couponCode": "WELCOME10", "expectedTotal": 1}),
        req("Place the order", "POST", "/api/orders", [
            status(201, "Created"),
            check("a TV- number and a payment page", "pm.expect(j.number).to.match(/^TV-\\d{6}$/); pm.expect(j.redirectUrl).to.include('/test-payment/')"),
            "const j = pm.response.json(); pm.collectionVariables.set('orderNumber', j.number); "
            "pm.collectionVariables.set('session', j.redirectUrl.split('/').pop());",
        ], body={"addressId": "{{addressId}}", "couponCode": "WELCOME10", "expectedTotal": "{{expectedTotal}}"}),
        req("Test payment page", "GET", "/api/payments/fake/{{session}}", [
            ok, check("CREATED", "pm.expect(j.status).to.eql('CREATED')"),
        ]),
        req("DONE-WHEN: pay -> every package PLACED", "POST", "/api/payments/fake/{{session}}/complete", [
            ok, check("PAID, 2 packages PLACED",
                      "pm.expect(j.status).to.eql('PAID'); pm.expect(j.packages.map(p => p.status)).to.eql(['PLACED', 'PLACED'])"),
        ]),
        req("My orders", "GET", "/api/orders", [
            ok, check("newest is this order", "pm.expect(j.content[0].number).to.eql(pm.collectionVariables.get('orderNumber'))"),
        ]),
        req("Logout", "POST", "/api/auth/logout", [status(204, "No Content")]),
    ]),
    ("6. Uploads", [
        # a logged-out visitor (with a CSRF token, like a browser) -> 401 "please log in"
        req("Signature from a logged-out visitor -> 401", "POST", "/api/uploads/signature", [status(401, "Unauthorized")]),
    ]),
]

collection = {
    "info": {
        "name": "TriVoKo API (Phase 1-4)",
        "description": "The public catalogue, accounts & roles, search, the cart and checkout, with automatic checks. Needs the backend on "
                       "localhost:8080 with the Flyway sample data (V2-V5) and the demo password in the "
                       "demoPassword variable (DEMO_PASSWORD from .env). "
                       "Generated by tools/postman-gen/make_collection.py.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080"},
        {"key": "demoPassword", "value": ""},
        {"key": "cricketCategory", "value": "26"},  # sub-category "Cricket" in the seed (tools/seed-gen)
        {"key": "xsrf", "value": ""},
        {"key": "productId", "value": ""},
        {"key": "chennaiVariant", "value": ""},
        {"key": "kovaiVariant", "value": ""},
        {"key": "addressId", "value": ""},
        {"key": "expectedTotal", "value": ""},
        {"key": "orderNumber", "value": ""},
        {"key": "session", "value": ""},
    ],
    "item": [{"name": name, "item": items} for name, items in folders],
}

OUT.parent.mkdir(exist_ok=True)
OUT.write_text(json.dumps(collection, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
checks = sum(len(r["event"][0]["script"]["exec"]) for _, items in folders for r in items)
print(f"wrote {OUT.name}: {sum(len(i) for _, i in folders)} requests, {checks} checks")
