# 2. Sample-data plan

The shop must look real on day one of the demo: enough products to make search, filters and paging meaningful, and sellers whose names tell the demo story. The data is loaded by a **Flyway migration in Phase 1** (V2 seed), so every computer and the live server get exactly the same shop.

>> A new supermarket does not open with empty shelves. Before the first customer walks in, the staff fill every aisle with a few of each item, so the shop looks and works like a real shop.

## 2.1 Numbers

@widths 3500 1500 4026
| What | How many | Why this number |
| Top categories | **10** | One row of category chips on the home page |
| Sub-categories | **2-3 per category (25)** | The category filter has something to filter |
| Sellers (approved) | **8** | Enough for "top sellers" reports and the shop pages |
| Sellers (pending, for the approval demo) | **1** | The admin has something to approve in Phase 6 |
| Products | **120** (12 per category) | 5 pages of 24 = real paging; facets with useful counts |
| Variants | **about 300** | Sizes for fashion and shoes, colours / storage for phones |
| Photos | **1-4 per product (about 250)** | Gallery on the product page |

## 2.2 The 10 categories

@widths 2600 3800 2626
| Category | Sub-categories | Variants by |
| Mobiles & Accessories | Mobiles, Cases & covers, Chargers & cables | Colour, storage |
| Laptops & Computers | Laptops, Keyboards & mice, Storage | RAM / storage |
| Audio & Wearables | Headphones, Speakers, Smartwatches | Colour |
| Men's Fashion | Shirts, T-shirts, Kurtas | Size S-XXL, colour |
| Women's Fashion | Sarees, Kurtis, Tops | Size, colour |
| Footwear | Running shoes, Sandals, Formal shoes | UK size 5-11 |
| Sports & Fitness | Cricket, Yoga & gym, Cycling | Size / weight |
| Home & Kitchen | Cookware, Storage, Decor | Capacity |
| Books | Fiction, Programming, Tamil books | Paperback / hardcover |
| Kids & Toys | Toys, Kids wear, School | Age / size |

## 2.3 The 8 sellers (+1 pending)

@widths 2400 1400 2400 2826
| Shop | City | Sells | Demo role |
| **Chennai Mobiles** | Chennai | Mobiles & accessories | Package 1 of order TV-1001 (iPhone 15 case) |
| **Kovai Sports** | Coimbatore | Sports, footwear | Package 2 of TV-1001 (shoes); the return + partial refund |
| **Bengaluru Gadget Hub** | Bengaluru | Laptops, audio, wearables | Offers the flash-sale headphones (100 at ₹999) |
| **Madurai Handlooms** | Madurai | Women's fashion, sarees | Size variants, price-drop alert for Kavya |
| **Mumbai Style Co** | Mumbai | Men's fashion | Size + colour variants |
| **Salem Steel Home** | Salem | Home & kitchen | Low-stock warning example |
| **Pondy Books** | Puducherry | Books | Simple products without sizes |
| **Tirupur Kids Wear** | Tirupur | Kids & toys | Age-size variants |
| *Erode Organics* (PENDING) | Erode | Grocery-style home items | Waits for admin approval in the demo |

> Shop names are made up for the demo. **Brands are made up too** (for example *Kovai Run*, *Volta*, *Arc*), so no real company's products or prices are copied. Product names may say what they fit ("case for iPhone 15"), as real accessory shops do.

## 2.4 Prices and stock

- Prices in rupees with realistic ranges: ₹199 (cable) to ₹74,999 (laptop). MRP 10-50% above the selling price, so "% off" looks real.
- Stock between 0 and 60 per variant. On purpose: some variants **sold out** (crossed-out size), some **below 5** (low-stock warning), most comfortable.
- A few products get 3-4 old prices in `price_history`, so "lowest price in 30 days" and price-drop alerts have data from the start.
- Every seed product is **ACTIVE** except 3 **PENDING** products (product-approval demo) and 1 **REJECTED** with a reason.

## 2.5 Where the product photos come from

@widths 2400 3600 3026
| Option | Good | Bad |
| **A. Free stock photos (Pexels / Unsplash licence) → Cloudinary** (chosen) | Real photos look professional in the portfolio; free licence allows use without payment; Cloudinary resizes them | About 250 photos to pick; must keep a credits file |
| B. Generated simple illustrations (SVG per category) | No licence questions; tiny files | Looks like a toy shop, weak in a portfolio |
| C. Grey placeholders | Zero work | Looks unfinished |

**Plan:** in Phase 1, pick photos from Pexels / Unsplash (free licence), upload them **once** to the TriVoKo Cloudinary account with a script, and keep `docs/photo-credits.csv` (product, photo page URL, photographer). The seed migration stores only the Cloudinary URLs. Until the Cloudinary account exists, products show a neutral placeholder.

## 2.6 Demo accounts (created in Phase 2)

| Account | Role | Used for |
| admin@trivoko.test | ADMIN | Approvals, flash sale, reports |
| ravi@trivoko.test | CUSTOMER | The main demo story (buys from 2 sellers, returns shoes) |
| kavya@trivoko.test | CUSTOMER | Wishlist + price-drop alert |
| chennai@trivoko.test, kovai@trivoko.test ... | SELLER | One login per demo seller |

The password comes from `.env` (`DEMO_PASSWORD`), never from the code - the EventHub rule.
