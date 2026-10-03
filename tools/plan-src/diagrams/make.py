"""Writes the TriVoKo master-plan diagrams as HTML pages and renders each to PNG with headless Edge."""
import os, subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'src')
os.makedirs(SRC, exist_ok=True)
EDGE = r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'

# TriVoKo colours: deep teal brand + saffron accent
CSS = """*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'Segoe UI',Calibri,Arial,sans-serif;background:#fff;color:#1f2937;padding:28px;width:1400px}
.row{display:flex;align-items:center;gap:18px}
.col{display:flex;flex-direction:column;gap:14px}
.b{border:2px solid #0f766e;background:#f0fdfa;border-radius:12px;padding:14px 18px;text-align:center}
.b b{display:block;font-size:22px;color:#134e4a}
.b span{font-size:16px;color:#374151}
.b ul{text-align:left;font-size:17px;color:#1f2937;margin:8px 0 0 20px;line-height:1.55}
.dark{background:#134e4a;border-color:#134e4a}.dark b,.dark span,.dark ul{color:#fff}
.amber{background:#fff7ed;border-color:#f97316}.amber b{color:#9a3412}
.green{background:#dcfce7;border-color:#15803d}.green b{color:#14532d}
.red{background:#fee2e2;border-color:#b91c1c}.red b{color:#7f1d1d}
.gray{background:#f3f4f6;border-color:#9ca3af}.gray b{color:#374151}
.blue{background:#eff6ff;border-color:#2563eb}.blue b{color:#1e3a8a}
.arrow{font-size:34px;color:#6b7280;line-height:1}
.lbl{font-size:16px;color:#4b5563;text-align:center}
.frame{border:3px dashed #134e4a;border-radius:16px;padding:18px}
.frame>h3{font-size:20px;color:#134e4a;text-align:center;margin-bottom:12px}
h2{font-size:26px;color:#134e4a;margin-bottom:18px}
.chip{display:inline-block;border-radius:999px;padding:4px 12px;font-size:15px;font-weight:600;margin:3px}
.big{font-size:44px}
"""

PAGES = {
# ------------------------------------------------------------------ 1
'roles': (720, """<h2>Who uses TriVoKo and what each person can do</h2>
<div class="row" style="align-items:stretch">
 <div class="b" style="width:440px"><div class="big">&#128722;</div><b>Customer (buyer)</b>
  <ul><li>Browse, smart search, filters</li><li>Product page: photos, sizes, reviews</li><li>One cart with items from many sellers</li><li>One payment (Stripe test mode)</li><li>Track each package separately</li><li>Return an item and get a refund</li><li>Wishlist + price-drop alerts</li><li>Coupons, PDF invoice, review after buying</li></ul></div>
 <div class="b amber" style="width:440px"><div class="big">&#127978;</div><b>Seller (shop owner)</b>
  <ul><li>Apply to sell, admin approves the shop</li><li>Add products, sizes/colours, photos, stock</li><li>"Orders to ship" list: Packed &rarr; Shipped</li><li>Approve or reject return requests</li><li>Join flash sales with a fixed stock</li><li>Earnings ledger (price minus 10% fee)</li><li>Sales charts for their shop only</li></ul></div>
 <div class="b dark" style="width:440px"><div class="big">&#128081;</div><b>Admin (TriVoKo team)</b>
  <ul><li>Approve sellers and new products</li><li>See all users, orders and payments</li><li>Create coupons and schedule flash sales</li><li>Decide return disputes</li><li>Block bad sellers or users</li><li>Whole-site sales reports and charts</li><li>Audit log: who changed what</li></ul></div>
</div>
<div class="lbl" style="margin-top:18px;font-size:18px">Real-life match: Customer = you on Flipkart &middot; Seller = a shop selling on Flipkart &middot; Admin = Flipkart's own staff.</div>"""),
# ------------------------------------------------------------------ 2
'architecture': (980, """<h2>TriVoKo architecture: one Spring Boot app with walled modules</h2>
<div class="row" style="align-items:stretch">
 <div class="col" style="width:270px;justify-content:center">
  <div class="b"><b>Browser</b><span>React 19 + Vite<br>Tailwind &middot; Redux Toolkit<br>React Router &middot; Axios</span></div>
  <div class="lbl">JSON over HTTPS<br>JWT in httpOnly cookie<br>CSRF header</div>
 </div>
 <div class="arrow" style="align-self:center">&#10140;</div>
 <div class="frame" style="width:640px"><h3>Spring Boot 4 API (Java 25) &mdash; modular monolith</h3>
  <div class="b amber"><b>Security</b><span>JWT cookie filter &middot; CSRF &middot; CORS &middot; roles CUSTOMER / SELLER / ADMIN &middot; rate limits</span></div>
  <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-top:12px">
   <div class="b"><b>catalog</b></div><div class="b"><b>seller</b></div><div class="b"><b>cart</b></div><div class="b"><b>order</b></div>
   <div class="b"><b>payment</b></div><div class="b amber"><b>flashsale</b></div><div class="b amber"><b>returns</b></div><div class="b"><b>review</b></div>
   <div class="b"><b>wishlist</b></div><div class="b"><b>coupon</b></div><div class="b amber"><b>search</b></div><div class="b"><b>notify</b></div>
   <div class="b"><b>admin</b></div><div class="b"><b>analytics</b></div><div class="b"><b>user</b></div><div class="b gray"><b>common</b></div>
  </div>
  <div class="lbl" style="margin-top:12px">Rule: a module calls another module only through its public service &mdash; never its tables or repositories.<br>Orange = standout features. Each module = one Java package (controller, service, repository, entity, dto).</div>
 </div>
 <div class="arrow" style="align-self:center">&#10140;</div>
 <div class="col" style="width:330px;justify-content:center">
  <div class="b dark"><b>MySQL 8.4</b><span>all data &middot; Flyway migrations</span></div>
  <div class="b"><b>Stripe (test mode)</b><span>card payments, refunds, signed webhooks</span></div>
  <div class="b"><b>Cloudinary</b><span>product photos (free tier)</span></div>
  <div class="b"><b>Email</b><span>Mailpit locally, real SMTP when live</span></div>
  <div class="b gray"><b>Redis (optional)</b><span>only if the load test says we need it</span></div>
 </div>
</div>"""),
# ------------------------------------------------------------------ 3
'order-split': (760, """<h2>Core feature: one cart, many sellers, one payment, split packages</h2>
<div class="row" style="align-items:flex-start">
 <div class="b" style="width:330px"><b>&#128722; Cart</b><ul><li>Phone &mdash; Seller A &mdash; &#8377;12,000</li><li>Book &mdash; Seller A &mdash; &#8377;400</li><li>Shoes &mdash; Seller B &mdash; &#8377;3,000</li></ul></div>
 <div class="arrow" style="margin-top:60px">&#10140;</div>
 <div class="b amber" style="width:310px"><b>Checkout</b><span>address &middot; coupon<br>stock held for 10 minutes<br>price checked again on the server</span></div>
 <div class="arrow" style="margin-top:60px">&#10140;</div>
 <div class="b dark" style="width:300px"><b>ONE payment</b><span>&#8377;15,400 via Stripe<br>order TV-1001 = PAID</span></div>
</div>
<div class="row" style="margin:10px 0 0 860px"><div class="arrow">&#8595;</div><div class="lbl">split automatically by seller</div></div>
<div class="row" style="gap:30px;margin-top:6px">
 <div class="b green" style="width:640px"><b>Package 1 &rarr; Seller A</b><span>Phone + Book &middot; &#8377;12,400</span><div style="margin-top:8px"><span class="chip" style="background:#bbf7d0">PLACED</span>&rarr;<span class="chip" style="background:#bbf7d0">PACKED</span>&rarr;<span class="chip" style="background:#bbf7d0">SHIPPED</span>&rarr;<span class="chip" style="background:#bbf7d0">DELIVERED</span></div></div>
 <div class="b green" style="width:640px"><b>Package 2 &rarr; Seller B</b><span>Shoes &middot; &#8377;3,000</span><div style="margin-top:8px"><span class="chip" style="background:#bbf7d0">PLACED</span>&rarr;<span class="chip" style="background:#bbf7d0">PACKED</span>&rarr;<span class="chip" style="background:#bbf7d0">SHIPPED</span>&rarr;<span class="chip" style="background:#bbf7d0">DELIVERED</span></div></div>
</div>
<div class="lbl" style="margin-top:18px;font-size:18px;text-align:left">The customer sees two tracking timelines inside one order. Seller A never sees Seller B's items or the customer's other purchases. Each package keeps its own money line: 90% to the seller's earnings, 10% TriVoKo fee.</div>"""),
# ------------------------------------------------------------------ 4
'states': (900, """<h2>Package states (one package = one seller's part of an order)</h2>
<div class="row" style="gap:14px">
 <div class="b gray" style="width:230px"><b style="font-size:19px">PENDING_PAYMENT</b><span>stock held 10 min</span></div><div class="arrow">&#10140;</div>
 <div class="b" style="width:160px"><b>PLACED</b><span>paid</span></div><div class="arrow">&#10140;</div>
 <div class="b" style="width:160px"><b>PACKED</b><span>by seller</span></div><div class="arrow">&#10140;</div>
 <div class="b blue" style="width:170px"><b>SHIPPED</b><span>tracking no.</span></div><div class="arrow">&#10140;</div>
 <div class="b green" style="width:180px"><b>DELIVERED</b><span>7-day return window starts</span></div>
</div>
<div class="row" style="gap:14px;margin-top:14px">
 <div class="b red" style="width:230px"><b>EXPIRED</b><span>not paid in time,<br>stock back</span></div>
 <div class="lbl" style="width:540px;text-align:left">&#8593; unpaid hold ends (job every minute)</div>
 <div class="b red" style="width:300px"><b>CANCELLED</b><span>customer or seller cancels before SHIPPED &rarr; refund + stock back</span></div>
</div>
<h2 style="margin-top:34px">Return states (standout feature #2)</h2>
<div class="row" style="gap:14px">
 <div class="b amber" style="width:230px"><b>REQUESTED</b><span>reason + photo,<br>within 7 days</span></div><div class="arrow">&#10140;</div>
 <div class="b" style="width:190px"><b>APPROVED</b><span>by seller (or admin)</span></div><div class="arrow">&#10140;</div>
 <div class="b blue" style="width:190px"><b>PICKED_UP</b><span>item collected</span></div><div class="arrow">&#10140;</div>
 <div class="b green" style="width:300px"><b>REFUNDED</b><span>Stripe partial refund &middot; stock +1<br>seller earnings &minus;</span></div>
</div>
<div class="row" style="gap:14px;margin-top:14px">
 <div class="b red" style="width:230px"><b>REJECTED</b><span>by seller, with reason</span></div><div class="arrow">&#10140;</div>
 <div class="b amber" style="width:230px"><b>ESCALATED</b><span>customer disagrees</span></div><div class="arrow">&#10140;</div>
 <div class="b dark" style="width:380px"><b>ADMIN decides</b><span>&rarr; APPROVED or CLOSED (final)</span></div>
</div>
<div class="lbl" style="margin-top:16px;text-align:left;font-size:17px">Every move is checked by a state machine in Java (enum with allowed next states), exactly like EventHub's event approval &mdash; an illegal move (e.g. REFUNDED &rarr; REQUESTED) is a 409 error.</div>"""),
# ------------------------------------------------------------------ 5
'flash-sale': (900, """<h2>Standout feature #1: the flash sale that never oversells</h2>
<div class="row" style="align-items:stretch">
 <div class="b amber" style="width:340px"><b>12:00:00 PM</b><span>"100 phones at &#8377;999"<br>live countdown on the home page</span><div class="big" style="margin-top:6px">&#9889;</div></div>
 <div class="arrow" style="align-self:center">&#10140;</div>
 <div class="b" style="width:300px"><b>5,000 people click "Buy"</b><span>in the same second</span><div class="big" style="margin-top:6px">&#128101;&#128101;&#128101;</div></div>
 <div class="arrow" style="align-self:center">&#10140;</div>
 <div class="b dark" style="width:560px"><b>One atomic database step</b><span style="font-family:Consolas;font-size:16px;display:block;margin-top:8px;text-align:left">UPDATE flash_sale_items<br>&nbsp;&nbsp;SET sold = sold + 1<br>&nbsp;WHERE id = 7 AND sold &lt; 100;</span><span style="display:block;margin-top:8px">1 row changed = you got it &middot; 0 rows = sold out<br>+ UNIQUE(sale, customer) = max 1 per person</span></div>
</div>
<div class="row" style="gap:30px;margin-top:22px">
 <div class="b green" style="width:660px"><b>&#9989; 100 people</b><span>get a 10-minute hold &rarr; pay &rarr; order PLACED<br>did not pay? the hold expires and the phone goes back into the sale</span></div>
 <div class="b red" style="width:660px"><b>&#10060; 4,900 people</b><span>see "Sold out" instantly &mdash; no waiting, no error page,<br>no oversell, no money taken</span></div>
</div>
<div class="b gray" style="margin-top:22px"><b>Proof for interviews</b><span>Java test: 1,000 threads, 100 items &rarr; exactly 100 winners (like EventHub's 100-thread seat test) &middot; k6 load test: 5,000 virtual users against the running app &rarr; graph of response times in the README &middot; Redis is added ONLY if these numbers are bad</span></div>"""),
# ------------------------------------------------------------------ 6
'price-search': (820, """<h2>Standout features #3 and #4: price-drop alerts and smart search</h2>
<div class="row" style="align-items:stretch;gap:30px">
 <div class="frame" style="width:660px"><h3>&#128276; Price-drop alert</h3>
  <div class="col">
   <div class="b"><b>Kavya wishlists shoes</b><span>price then &#8377;2,999</span></div><div class="arrow" style="text-align:center">&#8595;</div>
   <div class="b amber"><b>Seller lowers the price to &#8377;2,499</b><span>saved in price_history &middot; ProductPriceChanged event</span></div><div class="arrow" style="text-align:center">&#8595;</div>
   <div class="b green"><b>Kavya gets email + bell</b><span>"Price dropped 17% on Nike shoes!" (only once per drop)</span></div>
   <div class="b gray"><b>Bonus</b><span>"Lowest price in 30 days" badge from price_history</span></div>
  </div></div>
 <div class="frame" style="width:660px"><h3>&#128269; Smart search</h3>
  <div class="col">
   <div class="b"><b>User types "iphnoe 15"</b><span>suggestions appear after 2 letters</span></div><div class="arrow" style="text-align:center">&#8595;</div>
   <div class="b amber"><b>Hibernate Search + Lucene</b><span>index lives inside the app &mdash; no extra server<br>fuzzy match fixes the typo &rarr; iPhone 15</span></div><div class="arrow" style="text-align:center">&#8595;</div>
   <div class="b green"><b>Results with filters</b><span>category &middot; brand &middot; price range &middot; rating &middot; in stock<br>sort by relevance / price / newest / rating</span></div>
   <div class="b gray"><b>"Customers also bought"</b><span>products that appear in the same orders most often</span></div>
  </div></div>
</div>"""),
# ------------------------------------------------------------------ 7
'er-diagram': (1000, """<h2>Main tables and how they are linked</h2>
<div class="row" style="align-items:flex-start;gap:18px">
 <div class="col" style="width:330px">
  <div class="b"><b>users</b><span>email, password_hash, full_name, phone, status</span></div>
  <div class="b gray"><b>user_roles</b><span>CUSTOMER / SELLER / ADMIN</span></div>
  <div class="b"><b>addresses</b><span>user, name, line1, city, state, pincode, default</span></div>
  <div class="b amber"><b>sellers</b><span>user (owner), shop_name, city, status PENDING/APPROVED/BLOCKED</span></div>
  <div class="b gray"><b>seller_ledger</b><span>seller, package, amount +/&minus;, reason</span></div>
 </div>
 <div class="col" style="width:330px">
  <div class="b gray"><b>categories</b><span>name, slug, parent (tree)</span></div>
  <div class="b dark"><b>products</b><span>seller, category, title, brand, description, status, rating_avg</span></div>
  <div class="b"><b>product_variants</b><span>product, sku, size/colour, price, mrp, stock, version</span></div>
  <div class="b gray"><b>product_images</b><span>product, cloudinary_url, position</span></div>
  <div class="b gray"><b>price_history</b><span>variant, old &rarr; new price, changed_at</span></div>
 </div>
 <div class="col" style="width:330px">
  <div class="b"><b>carts / cart_items</b><span>user (or guest token), variant, qty</span></div>
  <div class="b dark"><b>orders</b><span>number TV-1001, customer, address copy, total, coupon, status</span></div>
  <div class="b dark"><b>packages</b><span>order, seller, status, tracking_no, version</span></div>
  <div class="b"><b>order_items</b><span>package, variant, title copy, price copy, qty</span></div>
  <div class="b"><b>payments / refunds</b><span>order, stripe ids, amount, status</span></div>
 </div>
 <div class="col" style="width:330px">
  <div class="b amber"><b>flash_sales / flash_sale_items</b><span>start, end, variant, sale_price, limit, sold</span></div>
  <div class="b amber"><b>return_requests</b><span>order_item, reason, photo, status</span></div>
  <div class="b"><b>reviews</b><span>order_item (verified buyer), rating 1-5, text</span></div>
  <div class="b"><b>wishlist_items</b><span>user, variant, price_when_added</span></div>
  <div class="b gray"><b>coupons &middot; notifications &middot; audit_log</b></div>
 </div>
</div>
<div class="lbl" style="text-align:left;margin-top:18px;font-size:18px">Read it like this: a <b>seller</b> owns many <b>products</b>; a product has <b>variants</b> (size/colour, each with its own price and stock); an <b>order</b> is split into <b>packages</b> (one per seller); each package has <b>order_items</b>. Order items COPY the title and price, so later price changes never change old orders.</div>"""),
# ------------------------------------------------------------------ 8
'roadmap': (1000, """<h2>The 13 phases (each phase ends with a working, tested, pushed app)</h2>
<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px">
 <div class="b gray"><b>0 &middot; Setup</b><span>repo, Docker, CI, sketches, sample data plan</span></div>
 <div class="b"><b>1 &middot; Catalogue API</b><span>categories, products, variants, Cloudinary photos</span></div>
 <div class="b"><b>2 &middot; Accounts &amp; roles</b><span>customer, seller (approved), admin, JWT</span></div>
 <div class="b"><b>3 &middot; Shop frontend</b><span>home, listing, product page, cart</span></div>
 <div class="b"><b>4 &middot; Checkout &amp; pay</b><span>address, stock hold, Stripe, order split, invoice</span></div>
 <div class="b"><b>5 &middot; Seller dashboard</b><span>products, stock, ship orders, earnings</span></div>
 <div class="b"><b>6 &middot; Admin panel</b><span>approvals, users, orders, coupons, reports</span></div>
 <div class="b amber"><b>7 &middot; &#9889; Flash sale</b><span>atomic stock, countdown, k6 load test<br><i>you type the stock logic</i></span></div>
 <div class="b amber"><b>8 &middot; &#8617; Returns</b><span>state machine, Stripe partial refund<br><i>you type the state machine</i></span></div>
 <div class="b"><b>9 &middot; Reviews &amp; alerts</b><span>verified reviews, wishlist, &#128276; price drops</span></div>
 <div class="b amber"><b>10 &middot; &#128269; Smart search</b><span>Hibernate Search, autocomplete, also-bought</span></div>
 <div class="b"><b>11 &middot; Test &amp; polish</b><span>Playwright E2E, security, speed, phone check</span></div>
 <div class="b dark" style="grid-column:span 4"><b>12 &middot; &#128640; Launch</b><span>live link (free or paid), README with GIFs, 3-minute demo video, resume bullets, LinkedIn post, tag v1.0</span></div>
</div>
<div class="lbl" style="margin-top:16px;font-size:17px">Must-have core = phases 0-8 (a complete marketplace with the two strongest standouts). Phases 9-10 make it shine. If time runs short, 9 and 10 can be shortened without breaking anything.</div>"""),
# ------------------------------------------------------------------ 9
'outcome': (720, """<h2>The finished outcome: what you show a recruiter</h2>
<div class="row" style="align-items:stretch">
 <div class="b dark" style="width:330px"><div class="big">&#127760;</div><b>Live link</b><span>trivoko.&hellip; opens in 1-2 s, demo logins for each role on the login page</span></div>
 <div class="b" style="width:330px"><div class="big">&#128193;</div><b>GitHub</b><span>CI green &middot; ~80% test coverage &middot; README with architecture diagram, GIFs and load-test graph</span></div>
 <div class="b amber" style="width:330px"><div class="big">&#127909;</div><b>3-minute demo video</b><span>buy from 2 sellers &rarr; flash sale &rarr; return &amp; refund</span></div>
 <div class="b green" style="width:330px"><div class="big">&#128172;</div><b>Interview stories</b><span>"How did you stop overselling?"<br>"How do refunds work?"<br>"Why a modular monolith?"</span></div>
</div>
<div class="b gray" style="margin-top:22px;text-align:left"><b style="text-align:left">Resume line</b><span style="font-size:19px">Built <b style="display:inline;font-size:19px">TriVoKo</b>, a multi-seller e-commerce marketplace (Spring Boot 4, React 19, MySQL, Stripe) with split-order fulfilment, a flash-sale engine load-tested at 5,000 concurrent users with zero oversell, a return-and-refund workflow, price-drop alerts and typo-tolerant search; CI with ~80% coverage, deployed with Docker.</span></div>"""),
}

for name, (height, body) in PAGES.items():
    html = os.path.join(SRC, name + '.html')
    with open(html, 'w', encoding='utf-8') as f:
        f.write('<!doctype html><meta charset="utf-8"><style>' + CSS + '</style><body>' + body + '</body>')
    png = os.path.join(HERE, name + '.png')
    subprocess.run([EDGE, '--headless=new', '--disable-gpu', '--hide-scrollbars', f'--screenshot={png}',
                    f'--window-size=1456,{height}', '--default-background-color=ffffffff', 'file:///' + html.replace('\\', '/')],
                   check=True, capture_output=True, timeout=90)
    # crop the empty white space at the bottom and right
    from PIL import Image, ImageChops
    im = Image.open(png).convert('RGB')
    box = ImageChops.difference(im, Image.new('RGB', im.size, (255, 255, 255))).getbbox()
    if box:
        im.crop((0, 0, min(im.width, box[2] + 28), min(im.height, box[3] + 28))).save(png)
    print(name, im.size, os.path.getsize(png))
