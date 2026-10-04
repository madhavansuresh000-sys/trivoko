"""
TriVoKo sample data -> backend/src/main/resources/db/migration/V2__seed_catalogue.sql

Run:  python tools/seed-gen/make_seed.py      (from the project folder)

Follows the approved sample-data plan (Phase_0_Setup\\Phase0_Sketches_and_Sample_Data):
10 top categories, 3 sub-categories each, 8 approved sellers + 1 pending, 120 products
(12 per top category), about 300 variants, a few price histories. Brands are made up.

The random numbers use a fixed seed, so running the script again writes the SAME file.
IMPORTANT: once V2 has run on a database, never edit it - Flyway would refuse to start
(checksum mismatch). Changes to the sample data go into a NEW migration (V3, V4 ...).
"""
import math
import random
import re
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "backend/src/main/resources/db/migration/V2__seed_catalogue.sql"
rng = random.Random(2026_10_03)

# ---------------------------------------------------------------- sellers
# (shop name, slug, city, description, status)
SELLERS = [
    ("Chennai Mobiles", "chennai-mobiles", "Chennai", "Phones, cases and chargers from T. Nagar since 2012.", "APPROVED"),
    ("Kovai Sports", "kovai-sports", "Coimbatore", "Running shoes, cricket kit and fitness gear.", "APPROVED"),
    ("Bengaluru Gadget Hub", "bengaluru-gadget-hub", "Bengaluru", "Laptops, audio and wearables for work and play.", "APPROVED"),
    ("Madurai Handlooms", "madurai-handlooms", "Madurai", "Handloom sarees and kurtis from Madurai weavers.", "APPROVED"),
    ("Mumbai Style Co", "mumbai-style-co", "Mumbai", "Shirts, T-shirts and kurtas for men.", "APPROVED"),
    ("Salem Steel Home", "salem-steel-home", "Salem", "Steel cookware and home storage.", "APPROVED"),
    ("Pondy Books", "pondy-books", "Puducherry", "New books in English and Tamil.", "APPROVED"),
    ("Tirupur Kids Wear", "tirupur-kids-wear", "Tirupur", "Cotton kids wear, toys and school things.", "APPROVED"),
    ("Erode Organics", "erode-organics", "Erode", "Organic home and kitchen items (waiting for approval).", "PENDING"),
]
SELLER_ID = {s[1]: i + 1 for i, s in enumerate(SELLERS)}

# ---------------------------------------------------------------- variant kinds
# each kind -> list of (label, size, colour, price_add)
VARIANT_KINDS = {
    "single":   [("Standard", None, None, 0)],
    "phone":    [("Black / 128 GB", "128 GB", "Black", 0), ("Black / 256 GB", "256 GB", "Black", 2000),
                 ("Blue / 128 GB", "128 GB", "Blue", 0)],
    "colour2":  [("Black", None, "Black", 0), ("Blue", None, "Blue", 0)],
    "colour3":  [("Black", None, "Black", 0), ("White", None, "White", 0), ("Teal", None, "Teal", 0)],
    "laptop":   [("8 GB / 512 GB", "8/512", None, 0), ("16 GB / 1 TB", "16/1TB", None, 8000)],
    "ssd":      [("500 GB", "500 GB", None, 0), ("1 TB", "1 TB", None, 3000)],
    "card":     [("64 GB", "64 GB", None, 0), ("128 GB", "128 GB", None, 300)],
    "size4":    [("S", "S", None, 0), ("M", "M", None, 0), ("L", "L", None, 0), ("XL", "XL", None, 0)],
    "size3":    [("M", "M", None, 0), ("L", "L", None, 0), ("XL", "XL", None, 0)],
    "shoe":     [("UK 7", "UK 7", None, 0), ("UK 8", "UK 8", None, 0), ("UK 9", "UK 9", None, 0), ("UK 10", "UK 10", None, 0)],
    "sandal":   [("UK 7", "UK 7", None, 0), ("UK 8", "UK 8", None, 0), ("UK 9", "UK 9", None, 0)],
    "bat":      [("Short handle", "SH", None, 0), ("Long handle", "LH", None, 200)],
    "weight":   [("2 kg pair", "2 kg", None, 0), ("3 kg pair", "3 kg", None, 300), ("5 kg pair", "5 kg", None, 700)],
    "litre":    [("2 litre", "2 L", None, 0), ("3 litre", "3 L", None, 300)],
    "cooker":   [("3 litre", "3 L", None, 0), ("5 litre", "5 L", None, 400)],
    "book":     [("Paperback", "Paperback", None, 0), ("Hardcover", "Hardcover", None, 200)],
    "kid":      [("2-3 years", "2-3 Y", None, 0), ("4-5 years", "4-5 Y", None, 0), ("6-7 years", "6-7 Y", None, 0)],
}

# ---------------------------------------------------------------- catalogue
# top category -> (slug, seller slug, [(sub name, sub slug, [ (product name, brand, price, kind), x4 ])])
C = [
    ("Mobiles & Accessories", "mobiles-accessories", "chennai-mobiles", [
        ("Mobile phones", "phones", [
            ("Volta V12 5G", "Volta", 12999, "phone"),
            ("Nimbus N8 Pro", "Nimbus", 18999, "phone"),
            ("Kaveri K5", "Kaveri", 8999, "colour2"),
            ("Orbit X Ultra", "Orbit", 34999, "phone")]),
        ("Cases & covers", "cases-covers", [
            ("Silicone case for iPhone 15", "ShieldUp", 499, "colour3"),
            ("Clear armour case for Galaxy S24", "ShieldUp", 399, "single"),
            ("Leather flip cover for Volta V12", "Volta", 599, "colour2"),
            ("Rugged case for Pixel 9", "ShieldUp", 699, "single")]),
        ("Chargers & cables", "chargers-cables", [
            ("Volta 33W fast charger", "Volta", 899, "colour2"),
            ("Braided USB-C cable 1.5 m", "ShieldUp", 199, "colour2"),
            ("Nimbus 10000 mAh power bank", "Nimbus", 1299, "colour2"),
            ("Volta dual-port car charger", "Volta", 549, "single")]),
    ]),
    ("Laptops & Computers", "laptops-computers", "bengaluru-gadget-hub", [
        ("Laptops", "laptops", [
            ("Arc Book 14", "Arc", 54999, "laptop"),
            ("Zentra Pro 16", "Zentra", 66999, "laptop"),
            ("Arc Book Air 13", "Arc", 45999, "laptop"),
            ("Zentra Student 15", "Zentra", 32999, "laptop")]),
        ("Keyboards & mice", "keyboards-mice", [
            ("Zentra wireless keyboard and mouse", "Zentra", 1499, "single"),
            ("Arc mechanical keyboard K87", "Arc", 3499, "colour2"),
            ("Arc silent wireless mouse", "Arc", 699, "colour3"),
            ("Zentra ergonomic mouse", "Zentra", 1199, "single")]),
        ("Storage", "computer-storage", [
            ("Arc portable SSD", "Arc", 5499, "ssd"),
            ("Zentra external hard disk", "Zentra", 4299, "ssd"),
            ("Volta 64 GB pen drive", "Volta", 499, "single"),
            ("Arc microSD card", "Arc", 599, "card")]),
    ]),
    ("Audio & Wearables", "audio-wearables", "bengaluru-gadget-hub", [
        ("Headphones", "headphones", [
            ("Arc Pulse ANC headphones", "Arc", 2499, "colour2"),
            ("Arc Buds 2 earbuds", "Arc", 1799, "colour3"),
            ("Pulse Bass wired earphones", "Pulse", 399, "colour2"),
            ("Pulse Studio over-ear headphones", "Pulse", 3999, "single")]),
        ("Speakers", "speakers", [
            ("Pulse Boom portable speaker", "Pulse", 2199, "colour3"),
            ("Arc Home smart speaker", "Arc", 3499, "colour2"),
            ("Pulse Mini speaker", "Pulse", 999, "colour3"),
            ("Arc Soundbar 2.1", "Arc", 7999, "single")]),
        ("Smartwatches", "smartwatches", [
            ("Arc Watch Fit", "Arc", 2999, "colour3"),
            ("Pulse Active band", "Pulse", 1499, "colour2"),
            ("Arc Watch Pro", "Arc", 6999, "colour2"),
            ("Pulse Kids watch", "Pulse", 1999, "colour2")]),
    ]),
    ("Men's Fashion", "mens-fashion", "mumbai-style-co", [
        ("Shirts", "shirts", [
            ("Marine Drive oxford shirt", "Marine Drive", 1299, "size4"),
            ("Marine Drive linen shirt", "Marine Drive", 1599, "size4"),
            ("Bandra Basics check shirt", "Bandra Basics", 899, "size3"),
            ("Marine Drive formal white shirt", "Marine Drive", 1199, "size4")]),
        ("T-shirts", "t-shirts", [
            ("Bandra Basics crew-neck T-shirt", "Bandra Basics", 399, "size4"),
            ("Bandra Basics polo T-shirt", "Bandra Basics", 699, "size4"),
            ("Marine Drive graphic T-shirt", "Marine Drive", 499, "size4"),
            ("Bandra Basics dry-fit T-shirt", "Bandra Basics", 599, "size4")]),
        ("Kurtas", "kurtas", [
            ("Kurta Kraft cotton kurta", "Kurta Kraft", 999, "size4"),
            ("Kurta Kraft festive silk kurta", "Kurta Kraft", 2499, "size4"),
            ("Kurta Kraft short kurta", "Kurta Kraft", 799, "size4"),
            ("Kurta Kraft linen kurta", "Kurta Kraft", 1399, "size3")]),
    ]),
    ("Women's Fashion", "womens-fashion", "madurai-handlooms", [
        ("Sarees", "sarees", [
            ("Meenakshi Weaves Madurai cotton saree", "Meenakshi Weaves", 1499, "colour2"),
            ("Meenakshi Weaves silk saree", "Meenakshi Weaves", 5999, "single"),
            ("Vaigai Threads Chettinad cotton saree", "Vaigai Threads", 1899, "colour2"),
            ("Vaigai Threads printed georgette saree", "Vaigai Threads", 1199, "colour3")]),
        ("Kurtis", "kurtis", [
            ("Vaigai Threads A-line kurti", "Vaigai Threads", 799, "size4"),
            ("Vaigai Threads straight kurti", "Vaigai Threads", 699, "size4"),
            ("Meenakshi Weaves block-print kurti", "Meenakshi Weaves", 999, "size4"),
            ("Vaigai Threads anarkali kurti", "Vaigai Threads", 1299, "size3")]),
        ("Tops", "tops", [
            ("Vaigai Threads cotton top", "Vaigai Threads", 499, "size4"),
            ("Vaigai Threads peplum top", "Vaigai Threads", 699, "size4"),
            ("Meenakshi Weaves embroidered top", "Meenakshi Weaves", 899, "size3"),
            ("Vaigai Threads casual shirt top", "Vaigai Threads", 599, "size3")]),
    ]),
    ("Footwear", "footwear", "kovai-sports", [
        ("Running shoes", "running-shoes", [
            ("Kovai Run Swift running shoes", "Kovai Run", 2499, "shoe"),
            ("Kovai Run Marathon Pro", "Kovai Run", 4499, "shoe"),
            ("Stride Lite walking shoes", "Stride", 1499, "shoe"),
            ("Kovai Run Trail shoes", "Kovai Run", 3299, "shoe")]),
        ("Sandals", "sandals", [
            ("Stride comfort sandals", "Stride", 699, "sandal"),
            ("Stride sports sandals", "Stride", 899, "sandal"),
            ("Kovai Run slides", "Kovai Run", 399, "sandal"),
            ("Stride leather chappals", "Stride", 599, "sandal")]),
        ("Formal shoes", "formal-shoes", [
            ("Stride Oxford formal shoes", "Stride", 1999, "shoe"),
            ("Stride Derby formal shoes", "Stride", 1799, "shoe"),
            ("Stride loafers", "Stride", 1499, "shoe"),
            ("Stride office slip-ons", "Stride", 1299, "shoe")]),
    ]),
    ("Sports & Fitness", "sports-fitness", "kovai-sports", [
        ("Cricket", "cricket", [
            ("Kovai Willow English willow bat", "Kovai Willow", 4999, "bat"),
            ("Kovai Willow Kashmir willow bat", "Kovai Willow", 1499, "bat"),
            ("Kovai Willow leather ball pack of 2", "Kovai Willow", 599, "single"),
            ("Kovai Willow batting gloves", "Kovai Willow", 899, "size3")]),
        ("Yoga & gym", "yoga-gym", [
            ("FitAsana yoga mat 6 mm", "FitAsana", 699, "colour3"),
            ("FitAsana dumbbell pair", "FitAsana", 1299, "weight"),
            ("FitAsana resistance bands set", "FitAsana", 499, "single"),
            ("FitAsana gym gloves", "FitAsana", 399, "size3")]),
        ("Cycling", "cycling", [
            ("Velo city bicycle 26T", "Velo", 8999, "colour2"),
            ("Velo cycling helmet", "Velo", 1199, "size3"),
            ("Velo bicycle lights set", "Velo", 499, "single"),
            ("Velo water bottle 750 ml", "Velo", 249, "colour3")]),
    ]),
    ("Home & Kitchen", "home-kitchen", "salem-steel-home", [
        ("Cookware", "cookware", [
            ("Salem Steel tri-ply kadai", "Salem Steel", 1899, "litre"),
            ("Salem Steel pressure cooker", "Salem Steel", 1599, "cooker"),
            ("Salem Steel tawa 28 cm", "Salem Steel", 799, "single"),
            ("Salem Steel idli maker", "Salem Steel", 999, "single")]),
        ("Storage", "home-storage", [
            ("HomeNest steel dabba set", "HomeNest", 899, "single"),
            ("HomeNest glass jar set", "HomeNest", 699, "single"),
            ("HomeNest steel lunch box", "HomeNest", 499, "colour2"),
            ("HomeNest steel water bottle 1 L", "HomeNest", 399, "colour3")]),
        ("Decor", "decor", [
            ("HomeNest brass diya pair", "HomeNest", 599, "single"),
            ("HomeNest wall clock", "HomeNest", 899, "colour2"),
            ("HomeNest cotton cushion covers", "HomeNest", 499, "colour3"),
            ("HomeNest table lamp", "HomeNest", 1299, "single")]),
    ]),
    ("Books", "books", "pondy-books", [
        ("Fiction", "fiction", [
            ("The Monsoon Station", "Pondy Press", 349, "book"),
            ("Letters from Pondicherry", "Pondy Press", 299, "book"),
            ("The Last Tram to Kolkata", "Bay Books", 399, "book"),
            ("Salt and Saffron", "Bay Books", 449, "single")]),
        ("Programming", "programming", [
            ("Java from Zero", "Bay Books", 599, "book"),
            ("Spring Boot Step by Step", "Bay Books", 699, "single"),
            ("React for Beginners", "Pondy Press", 549, "single"),
            ("SQL in 30 Days", "Pondy Press", 449, "book")]),
        ("Tamil books", "tamil-books", [
            ("Kaveri Kathaigal", "Pondy Press", 199, "single"),
            ("Ponni Nadhi Kanavu", "Pondy Press", 349, "book"),
            ("Thamizh Kavithai Thoguppu", "Bay Books", 249, "single"),
            ("Siruvar Kathaigal", "Bay Books", 199, "single")]),
    ]),
    ("Kids & Toys", "kids-toys", "tirupur-kids-wear", [
        ("Toys", "toys", [
            ("PlayBox wooden blocks 50 pcs", "PlayBox", 699, "single"),
            ("PlayBox remote control car", "PlayBox", 1499, "colour3"),
            ("PlayBox jigsaw puzzle 100 pcs", "PlayBox", 399, "single"),
            ("PlayBox doctor play set", "PlayBox", 599, "single")]),
        ("Kids wear", "kids-wear", [
            ("Tirupur Tots cotton T-shirt pack of 3", "Tirupur Tots", 499, "kid"),
            ("Tirupur Tots cotton frock", "Tirupur Tots", 699, "kid"),
            ("Tirupur Tots shorts set", "Tirupur Tots", 599, "kid"),
            ("Tirupur Tots night suit", "Tirupur Tots", 649, "kid")]),
        ("School", "school", [
            ("PlayBox school bag", "PlayBox", 899, "colour2"),
            ("PlayBox geometry box", "PlayBox", 199, "single"),
            ("PlayBox kids water bottle", "PlayBox", 299, "colour3"),
            ("PlayBox lunch bag", "PlayBox", 349, "colour2")]),
    ]),
]

# approval demo: 3 PENDING, 1 REJECTED (product name -> reason)
PENDING = {"Arc Soundbar 2.1", "Kurta Kraft linen kurta", "Velo city bicycle 26T"}
REJECTED = {"Pulse Kids watch": "Photos do not show the real product. Please upload your own photos."}
# demo stock: (product name, variant label or None for all) -> stock
FIXED_STOCK = {
    ("Salem Steel idli maker", None): 3,             # low-stock warning example
    ("Velo bicycle lights set", None): 0,            # sold out: hidden by "in stock only"
    ("Arc Pulse ANC headphones", None): 120,         # flash-sale headphones (100 at 999 in Phase 7)
    ("Silicone case for iPhone 15", "Black"): 40,    # Ravi's order TV-1001
    ("Kovai Run Swift running shoes", "UK 8"): 25,   # Ravi's shoes (returned in Phase 8)
}
# price histories (old prices, oldest first; the last change ends at today's price)
HISTORY = {
    "Volta V12 5G": [14999, 13999, 13499],
    "Arc Pulse ANC headphones": [3299, 2999, 2799],
    "Meenakshi Weaves silk saree": [6999, 6799, 6499, 6299],   # Kavya's price-drop alert
    "Kovai Run Swift running shoes": [2999, 2799, 2699],
    "Arc Book 14": [59999, 57999],
}

DESCRIPTIONS = {
    "mobiles-accessories": "Genuine stock with GST bill. Ships from Chennai in 1-2 days.",
    "laptops-computers": "Brand warranty. Packed safely and shipped from Bengaluru.",
    "audio-wearables": "Clear sound, long battery life, 1 year warranty.",
    "mens-fashion": "Soft breathable fabric. Machine wash cold. Easy 7-day returns.",
    "womens-fashion": "Woven and finished by hand. Colours may vary slightly from the photo.",
    "footwear": "Light cushioned sole made for Indian roads. Easy size exchange.",
    "sports-fitness": "Tested by club players and coaches in Coimbatore.",
    "home-kitchen": "Food-grade steel that lasts for years. Dishwasher safe.",
    "books": "New book, carefully packed so the corners stay sharp.",
    "kids-toys": "Child-safe materials and soft cotton, checked for small parts.",
}


def slugify(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def sql(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def money(value):
    return f"{value:.2f}"


def mrp_for(price):
    """MRP 10-50% above the price, ending in 9 like a real price tag (e.g. 1,799)."""
    raw = price * (1 + rng.uniform(0.10, 0.50))
    mrp = math.ceil(raw / 100) * 100 - 1 if raw >= 300 else math.ceil(raw / 10) * 10 - 1
    return max(mrp, price)


def stock_for():
    """Mostly comfortable stock; on purpose some sold-out and some low-stock variants."""
    roll = rng.random()
    if roll < 0.08:
        return 0
    if roll < 0.20:
        return rng.randint(1, 4)
    return rng.randint(8, 60)


def main():
    lines = [
        "-- TriVoKo V2: sample catalogue (generated by tools/seed-gen/make_seed.py - edit the script, not this file)",
        "-- 10 top categories x 3 sub-categories, 8 approved sellers + 1 pending, 120 products, variants, price history.",
        "-- Shop names and brands are made up for the demo. Product photos come in a later migration (Cloudinary).",
        "",
    ]

    # sellers
    lines.append("INSERT INTO sellers (id, shop_name, slug, city, description, status) VALUES")
    lines.append(",\n".join(
        f"  ({i + 1}, {sql(n)}, {sql(s)}, {sql(c)}, {sql(d)}, {sql(st)})" for i, (n, s, c, d, st) in enumerate(SELLERS)) + ";")
    lines.append("")

    # categories
    cat_rows, cat_id = [], 0
    sub_ids = {}
    for top_order, (top_name, top_slug, _, subs) in enumerate(C, start=1):
        cat_id += 1
        top_id = cat_id
        cat_rows.append(f"  ({top_id}, {sql(top_name)}, {sql(top_slug)}, NULL, {top_order})")
        for sub_order, (sub_name, sub_slug, _) in enumerate(subs, start=1):
            cat_id += 1
            sub_ids[sub_slug] = cat_id
            cat_rows.append(f"  ({cat_id}, {sql(sub_name)}, {sql(sub_slug)}, {top_id}, {sub_order})")
    lines.append("INSERT INTO categories (id, name, slug, parent_id, sort_order) VALUES")
    lines.append(",\n".join(cat_rows) + ";")
    lines.append("")

    # products + variants
    product_rows, variant_rows, history_rows = [], [], []
    product_id = variant_id = 0
    total = sum(len(p) for _, _, _, subs in C for _, _, p in subs)
    for top_name, top_slug, seller_slug, subs in C:
        for sub_name, sub_slug, items in subs:
            for name, brand, base_price, kind in items:
                product_id += 1
                slug = slugify(name)
                status = "PENDING" if name in PENDING else "REJECTED" if name in REJECTED else "ACTIVE"
                reason = REJECTED.get(name)
                description = f"{name} by {brand}. {DESCRIPTIONS[top_slug]}"
                # newest first in the shop = the last product in this list (age in hours)
                age_hours = (total - product_id) * 6 + 1

                variants = []
                for label, size, colour, add in VARIANT_KINDS[kind]:
                    variant_id += 1
                    price = base_price + add
                    mrp = mrp_for(price)
                    stock = FIXED_STOCK.get((name, label), FIXED_STOCK.get((name, None), stock_for()))
                    sku = f"TV-{product_id:03d}-{slugify(label).upper()}"[:60]
                    variants.append((variant_id, price, mrp))
                    variant_rows.append(
                        f"  ({variant_id}, {product_id}, {sql(sku)}, {sql(label)}, {sql(size)}, {sql(colour)}, "
                        f"{money(price)}, {money(mrp)}, {stock})")

                cheapest = min(variants, key=lambda v: (v[1], v[0]))
                product_rows.append(
                    f"  ({product_id}, {SELLER_ID[seller_slug]}, {sub_ids[sub_slug]}, {sql(name)}, {sql(slug)}, "
                    f"{sql(brand)}, {sql(description)}, {sql(status)}, {sql(reason)}, "
                    f"{money(cheapest[1])}, {money(cheapest[2])}, "
                    f"NOW(6) - INTERVAL {age_hours} HOUR, NOW(6) - INTERVAL {age_hours} HOUR)")

                if name in HISTORY:
                    # history on the cheapest variant: old1 -> old2 -> ... -> today's price
                    prices = HISTORY[name] + [cheapest[1]]
                    steps = len(prices) - 1
                    for i in range(steps):
                        days_ago = (steps - i) * 9   # e.g. 27, 18, 9 days ago
                        history_rows.append(
                            f"  ({cheapest[0]}, {money(prices[i])}, {money(prices[i + 1])}, NOW(6) - INTERVAL {days_ago} DAY)")

    lines.append("INSERT INTO products (id, seller_id, category_id, name, slug, brand, description, status, "
                 "rejection_reason, price_from, mrp_from, created_at, updated_at) VALUES")
    lines.append(",\n".join(product_rows) + ";")
    lines.append("")
    lines.append("INSERT INTO product_variants (id, product_id, sku, label, size, colour, price, mrp, stock) VALUES")
    lines.append(",\n".join(variant_rows) + ";")
    lines.append("")
    lines.append("INSERT INTO price_history (variant_id, old_price, new_price, changed_at) VALUES")
    lines.append(",\n".join(history_rows) + ";")
    lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"wrote {OUT.name}: {len(SELLERS)} sellers, {cat_id} categories, {product_id} products, "
          f"{variant_id} variants, {len(history_rows)} price changes")


if __name__ == "__main__":
    main()
