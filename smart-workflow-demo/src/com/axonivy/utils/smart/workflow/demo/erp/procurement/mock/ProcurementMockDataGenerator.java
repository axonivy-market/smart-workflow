package com.axonivy.utils.smart.workflow.demo.erp.procurement.mock;

import java.util.Calendar;
import java.util.Date;

import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.InventoryItem;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementItem;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.MaterialType;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.repository.InventoryRepository;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.repository.ProcurementItemRepository;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.repository.MaterialTypeRepository;

public class ProcurementMockDataGenerator {

  /**
   * Creates 5 material types (matching sample-procurement-request.md),
   * 10 material items each, and one inventory entry per material item.
   */
  public static void create() {
    MaterialTypeRepository typeRepo = MaterialTypeRepository.getInstance();
    ProcurementItemRepository itemRepo = ProcurementItemRepository.getInstance();
    InventoryRepository inventoryRepo = InventoryRepository.getInstance();

    ProcurementItem mi;

    // ── Type 1: Cement ───────────────────────────────────────────────────────
    MaterialType cement = typeRepo.save(new MaterialType("Cement", "Hydraulic binders and cement products"));

    mi = itemRepo.save(item(cement, 1,  "Portland Cement N 42.5",         "JIS R 5210 Type N",         100.0, "bag",  "Taiheiyo Cement",       1450, "Foundation slab and columns",           "Moisture-resistant packaging required"));
    inventoryRepo.save(inventory(cement, mi, 200.0,  100.0, "Osaka Warehouse A – Rack 1-A", date(2026, 4, 10)));

    mi = itemRepo.save(item(cement, 2,  "Portland Cement M 32.5",         "JIS R 5210 Type M",         100.0, "bag",  "Ube Industries",        1200, "General masonry work",                  null));
    inventoryRepo.save(inventory(cement, mi, 620.0,   80.0, "Osaka Warehouse A – Rack 1-B", date(2026, 4,  8)));

    mi = itemRepo.save(item(cement, 3,  "Rapid Hardening Cement EX",      "JIS R 5210 Type EX",         50.0, "bag",  "Sumitomo Osaka Cement", 1750, "Cold weather concreting",               "Use within 2 h of mixing"));
    inventoryRepo.save(inventory(cement, mi,  45.0,   30.0, "Osaka Warehouse A – Rack 1-C", date(2026, 3, 25)));

    mi = itemRepo.save(item(cement, 4,  "Sulphate-Resistant Cement SR",   "JIS R 5210 Type SR",          80.0, "bag",  "Taiheiyo Cement",       1620, "Underground foundations",               "For high-sulphate ground conditions"));
    inventoryRepo.save(inventory(cement, mi, 320.0,   40.0, "Osaka Warehouse A – Rack 1-D", date(2026, 4,  5)));

    mi = itemRepo.save(item(cement, 5,  "White Portland Cement WN",       "JIS R 5210 Type N",          30.0, "bag",  "Denka Company",         2580, "Architectural finishes",                null));
    inventoryRepo.save(inventory(cement, mi,  18.0,   12.0, "Osaka Warehouse A – Rack 1-E", date(2026, 3, 15)));

    mi = itemRepo.save(item(cement, 6,  "High-Slag Blast Furnace Cement", "JIS R 5211 Type B",          120.0, "bag",  "Ube Industries",        1330, "Mass concrete pours",                   "Low heat of hydration"));
    inventoryRepo.save(inventory(cement, mi, 490.0,  120.0, "Osaka Warehouse A – Rack 1-F", date(2026, 4, 12)));

    mi = itemRepo.save(item(cement, 7,  "Fly-Ash Cement Type II",         "JIS R 5213 Type II",          60.0, "bag",  "Sumitomo Osaka Cement", 1380, "Marine and water-retaining structures", null));
    inventoryRepo.save(inventory(cement, mi, 230.0,   60.0, "Osaka Warehouse A – Rack 1-G", date(2026, 4,  3)));

    mi = itemRepo.save(item(cement, 8,  "Pre-Mixed Mortar M10",           "JIS A 6916 M10",             200.0, "bag",  "Kansai Mortar",          980, "Block laying and rendering",            "25 kg bags"));
    inventoryRepo.save(inventory(cement, mi, 780.0,  200.0, "Osaka Warehouse A – Rack 1-H", date(2026, 4, 18)));

    mi = itemRepo.save(item(cement, 9,  "Self-Levelling Floor Compound",  "JIS A 6024",                  40.0, "bag",  "Ardex Japan",           2080, "Floor levelling",                       null));
    inventoryRepo.save(inventory(cement, mi,  55.0,   40.0, "Osaka Warehouse A – Rack 1-I", date(2026, 3, 20)));

    mi = itemRepo.save(item(cement, 10, "Waterproof Concrete Admixture",  "JIS A 6204",                  10.0, "pcs",  "Sika Japan",            5200, "Basement wall concrete mix",            "5 L containers"));
    inventoryRepo.save(inventory(cement, mi,  38.0,   10.0, "Osaka Warehouse A – Rack 1-J", date(2026, 4, 20)));

    // ── Type 2: Steel & Rebar ────────────────────────────────────────────────
    MaterialType steel = typeRepo.save(new MaterialType("Steel & Rebar", "Structural steel, reinforcement bars and mesh"));

    mi = itemRepo.save(item(steel, 1,  "Deformed Reinforcement Bar SD345",  "JIS G 3112 SD345",       8.5,   "t",   "Nippon Steel",    113000, "Structural reinforcement",              "Cut to length: 6 m bars"));
    inventoryRepo.save(inventory(steel, mi,  32.5,    8.5, "Kobe Warehouse B – Steel Yard 1", date(2026, 3, 20)));

    mi = itemRepo.save(item(steel, 2,  "Welded Steel Wire Mesh",            "JIS G 3551",             5.0,   "t",   "Tokyo Rope Mfg",  127000, "Slab reinforcement",                    "150x150 mm, 2.4 x 6 m sheets"));
    inventoryRepo.save(inventory(steel, mi,  12.0,    5.0, "Kobe Warehouse B – Steel Yard 2", date(2026, 3, 18)));

    mi = itemRepo.save(item(steel, 3,  "H-Section Steel 200x200",          "JIS G 3192 SS400",       3.2,   "t",   "JFE Steel",       144000, "Steel frame columns",                   "12 m lengths"));
    inventoryRepo.save(inventory(steel, mi,   8.4,    3.2, "Kobe Warehouse B – Steel Yard 3", date(2026, 4,  1)));

    mi = itemRepo.save(item(steel, 4,  "Wide-Flange Beam 300x150",         "JIS G 3192 SM490",       4.0,   "t",   "Nippon Steel",    153000, "Main roof beams",                       null));
    inventoryRepo.save(inventory(steel, mi,   6.0,    4.0, "Kobe Warehouse B – Steel Yard 4", date(2026, 3, 28)));

    mi = itemRepo.save(item(steel, 5,  "Corrugated Steel Roof Deck",       "JIS G 3302 SGCC",        2.5,   "t",   "Nippon Steel",    103000, "Roof decking",                          "0.75 mm thickness"));
    inventoryRepo.save(inventory(steel, mi,   4.5,    2.5, "Kobe Warehouse B – Steel Yard 5", date(2026, 4,  5)));

    mi = itemRepo.save(item(steel, 6,  "Column Base Plates SS400",         "JIS G 3101 SS400",       0.8,   "t",   "Kobelco Steel",   168000, "Column-to-foundation connection",       "Custom sizes – see drawing S-07"));
    inventoryRepo.save(inventory(steel, mi,   2.2,    0.8, "Kobe Warehouse B – Steel Yard 6", date(2026, 4, 10)));

    mi = itemRepo.save(item(steel, 7,  "Mechanical Rebar Coupler D22",     "JIS G 3112",           200.0,   "pcs", "Lenton Japan",       520, "Rebar splicing at construction joints", null));
    inventoryRepo.save(inventory(steel, mi, 350.0,  200.0, "Kobe Warehouse B – Rack A1",      date(2026, 4,  8)));

    mi = itemRepo.save(item(steel, 8,  "Plastic Rebar Spacers 40 mm",      "JIS A 5372",           500.0,   "pcs", "Kyushu Spacer",        92, "Concrete cover maintenance",            "40 mm cover"));
    inventoryRepo.save(inventory(steel, mi, 1200.0,  500.0, "Kobe Warehouse B – Rack A2",      date(2026, 4, 15)));

    mi = itemRepo.save(item(steel, 9,  "Binding Wire 1.6 mm",              "JIS G 3532 SWM-B",      50.0,   "kg",  "Aichi Steel",         255, "Rebar tying",                           null));
    inventoryRepo.save(inventory(steel, mi, 120.0,   50.0, "Kobe Warehouse B – Rack A3",      date(2026, 3, 22)));

    mi = itemRepo.save(item(steel, 10, "Galvanised Anchor Bolt M24",       "JIS B 1180 4T",        100.0,   "pcs", "Hilti Japan",        1385, "Baseplate fixing",                      "With nuts and washers"));
    inventoryRepo.save(inventory(steel, mi, 280.0,  100.0, "Kobe Warehouse B – Rack A4",      date(2026, 4, 17)));

    // ── Type 3: Formwork ─────────────────────────────────────────────────────
    MaterialType formwork = typeRepo.save(new MaterialType("Formwork", "Formwork panels, props and accessories"));

    mi = itemRepo.save(item(formwork, 1,  "Structural Formwork Panel",      "JIS A 6003",            150.0, "pcs", "PERI Japan",          5200, "Column and wall formwork",    "Reusable type, min 10 uses"));
    inventoryRepo.save(inventory(formwork, mi, 420.0, 150.0, "Nagoya Warehouse A – Rack 4-A", date(2026, 4,  1)));

    mi = itemRepo.save(item(formwork, 2,  "MANTO Wall Formwork Panel",      "JIS A 6003",             60.0, "pcs", "Doka Japan",         13800, "Core walls",                  "0.9 x 2.7 m panels"));
    inventoryRepo.save(inventory(formwork, mi,  85.0,  60.0, "Nagoya Warehouse A – Rack 4-B", date(2026, 3, 15)));

    mi = itemRepo.save(item(formwork, 3,  "Aluminium Slab Table Form",      "JIS A 6003",             80.0, "pcs", "Toyo Doken",          9800, "Flat slab construction",      "0.6 x 1.2 m tables"));
    inventoryRepo.save(inventory(formwork, mi, 160.0,  80.0, "Nagoya Warehouse A – Rack 4-C", date(2026, 4,  5)));

    mi = itemRepo.save(item(formwork, 4,  "Adjustable Steel Prop JP-300",   "JIS A 8951",            200.0, "pcs", "Sanwa Tekki",         2080, "Slab prop support",           "1.7–3.0 m range"));
    inventoryRepo.save(inventory(formwork, mi, 580.0, 200.0, "Nagoya Warehouse A – Rack 4-D", date(2026, 4, 10)));

    mi = itemRepo.save(item(formwork, 5,  "Concrete Form Plywood 12 mm",    "JIS A 5908 Type II",    300.0, "pcs", "Seihoku Plywood",     3220, "Formwork facing",             "1820 x 910 mm"));
    inventoryRepo.save(inventory(formwork, mi, 950.0, 300.0, "Nagoya Warehouse A – Rack 4-E", date(2026, 4, 18)));

    mi = itemRepo.save(item(formwork, 6,  "Form Tie Rod D15",               "JIS G 3112",            500.0, "pcs", "Okabe Co.",            437, "Wall formwork tie-through",   "0.75 m length"));
    inventoryRepo.save(inventory(formwork, mi, 1200.0, 500.0, "Nagoya Warehouse A – Rack 4-F", date(2026, 4, 12)));

    mi = itemRepo.save(item(formwork, 7,  "Steel Waler Beam 100x100",       "JIS A 6003",            100.0, "pcs", "Doka Japan",          2530, "Horizontal waling",           "3.9 m length"));
    inventoryRepo.save(inventory(formwork, mi, 220.0, 100.0, "Nagoya Warehouse A – Rack 4-G", date(2026, 3, 28)));

    mi = itemRepo.save(item(formwork, 8,  "Climbing Bracket Hydraulic",     "JIS A 8952",             20.0, "pcs", "PERI Japan",         43700, "Core wall climbing system",   "Hydraulic type"));
    inventoryRepo.save(inventory(formwork, mi,   8.0,   8.0, "Nagoya Warehouse A – Rack 4-H", date(2026, 2, 10)));

    mi = itemRepo.save(item(formwork, 9,  "Chamfer Strip 25x25 mm",         "JIS A 5905",            200.0, "m",   "Maruhon Timber",       138, "Edge chamfering",             null));
    inventoryRepo.save(inventory(formwork, mi, 560.0, 200.0, "Nagoya Warehouse A – Rack 4-I", date(2026, 4,  6)));

    mi = itemRepo.save(item(formwork, 10, "Release Agent (Form Oil)",       "JIS K 2213",             20.0, "pcs", "Idemitsu Kosan",      4025, "Formwork surface treatment",  "20 L drums"));
    inventoryRepo.save(inventory(formwork, mi,  42.0,  20.0, "Nagoya Warehouse A – Rack 4-J", date(2026, 4, 14)));

    // ── Type 4: Aggregates ───────────────────────────────────────────────────
    MaterialType aggregates = typeRepo.save(new MaterialType("Aggregates", "Gravel, sand and crushed stone"));

    mi = itemRepo.save(item(aggregates, 1,  "Crushed Gravel 20 mm",            "JIS A 5005",             60.0,  "t",   "Sumitomo Kensetsu Shizai", 3220, "Sub-base and drainage layer", "Washed, certificate required"));
    inventoryRepo.save(inventory(aggregates, mi, 145.0,  60.0, "Yokohama External Yard – Stockpile 2-A", date(2026, 4, 15)));

    mi = itemRepo.save(item(aggregates, 2,  "Concrete Fine Sand 0/2 mm",       "JIS A 5308",             40.0,  "t",   "Hanwa Kogyo",              2530, "Concrete mix",                "Clean, low silt content"));
    inventoryRepo.save(inventory(aggregates, mi,  88.0,  40.0, "Yokohama External Yard – Stockpile 2-B", date(2026, 4, 10)));

    mi = itemRepo.save(item(aggregates, 3,  "Crushed Limestone 4/8 mm",        "JIS A 5005",             30.0,  "t",   "Ube Materials",            3570, "Concrete aggregate",          null));
    inventoryRepo.save(inventory(aggregates, mi,  52.0,  30.0, "Yokohama External Yard – Stockpile 2-C", date(2026, 3, 30)));

    mi = itemRepo.save(item(aggregates, 4,  "Recycled Concrete Aggregate",     "JIS A 5021 Type H",      50.0,  "t",   "Taisei Recycling",         2070, "Non-structural fill",         "Min. 70% recycled content"));
    inventoryRepo.save(inventory(aggregates, mi, 110.0,  50.0, "Yokohama External Yard – Stockpile 2-D", date(2026, 4,  2)));

    mi = itemRepo.save(item(aggregates, 5,  "Crushed Stone Base 40/150 mm",    "JIS A 5001",             80.0,  "t",   "Fukuoka Quarry",           1610, "Hardcore bed below slab",     null));
    inventoryRepo.save(inventory(aggregates, mi, 220.0,  80.0, "Yokohama External Yard – Stockpile 2-E", date(2026, 4, 18)));

    mi = itemRepo.save(item(aggregates, 6,  "Washed Sand 0/4 mm",              "JIS A 5308",             25.0,  "t",   "Sumitomo Kensetsu Shizai", 2760, "Mortar sand",                 null));
    inventoryRepo.save(inventory(aggregates, mi,  64.0,  25.0, "Yokohama External Yard – Stockpile 2-F", date(2026, 4,  8)));

    mi = itemRepo.save(item(aggregates, 7,  "Expanded Shale Lightweight Agg.", "JIS A 5002",             15.0,  "t",   "Asahi Concrete",          10925, "Lightweight fill",            null));
    inventoryRepo.save(inventory(aggregates, mi,  28.0,  15.0, "Yokohama External Yard – Stockpile 2-G", date(2026, 3, 22)));

    mi = itemRepo.save(item(aggregates, 8,  "Fine Gravel 4/8 mm",              "JIS A 5005",             20.0,  "t",   "Hanwa Kogyo",              3450, "Drainage blanket",            null));
    inventoryRepo.save(inventory(aggregates, mi,  75.0,  20.0, "Yokohama External Yard – Stockpile 2-H", date(2026, 4, 13)));

    mi = itemRepo.save(item(aggregates, 9,  "Ready-Mix Concrete Fc24",         "JIS A 5308 Fc24",       120.0,  "m³",  "Kanto Ready Mixed",       15525, "Foundation slab pour",        "Consult site engineer for slump"));
    inventoryRepo.save(inventory(aggregates, mi, 380.0, 120.0, "Yokohama External Yard – Stockpile 2-I", date(2026, 4, 20)));

    mi = itemRepo.save(item(aggregates, 10, "Ready-Mix Concrete Fc30",         "JIS A 5308 Fc30",        80.0,  "m³",  "Kanto Ready Mixed",       17020, "Columns and walls",           "Pump mix"));
    inventoryRepo.save(inventory(aggregates, mi, 210.0,  80.0, "Yokohama External Yard – Stockpile 2-J", date(2026, 4, 20)));

    // ── Type 5: Waterproofing ────────────────────────────────────────────────
    MaterialType waterproofing = typeRepo.save(new MaterialType("Waterproofing", "Membranes, coatings and sealants"));

    mi = itemRepo.save(item(waterproofing, 1,  "Self-Adhesive Waterproof Sheet", "JIS A 6008 Type II",   320.0, "m²",  "Sika Japan",        1007, "Underground floor sealing",          "Self-adhesive, 3 mm thickness"));
    inventoryRepo.save(inventory(waterproofing, mi, 680.0, 320.0, "Tokyo Warehouse A – Rack 7-A", date(2026, 4, 22)));

    mi = itemRepo.save(item(waterproofing, 2,  "Torch-Applied Bitumen Sheet",    "JIS A 6005 Type SA",   200.0, "m²",  "Tajima Roofing",    1093, "Flat roof waterproofing",            "Torch-applied, 4 mm"));
    inventoryRepo.save(inventory(waterproofing, mi, 430.0, 200.0, "Tokyo Warehouse A – Rack 7-B", date(2026, 4, 15)));

    mi = itemRepo.save(item(waterproofing, 3,  "Cementitious Waterproof Coat",   "JIS A 6909",            50.0, "pcs", "Sika Japan",        4830, "Wet room floors and walls",          "15 kg buckets"));
    inventoryRepo.save(inventory(waterproofing, mi,  92.0,  50.0, "Tokyo Warehouse A – Rack 7-C", date(2026, 4, 10)));

    mi = itemRepo.save(item(waterproofing, 4,  "EPDM Rubber Sheet 1.5 mm",      "JIS K 6256",           100.0, "m²",  "Bridgestone Chemi.", 1610, "Green roof retention layer",         null));
    inventoryRepo.save(inventory(waterproofing, mi, 250.0, 100.0, "Tokyo Warehouse A – Rack 7-D", date(2026, 4,  8)));

    mi = itemRepo.save(item(waterproofing, 5,  "Crystalline Waterproof Slurry",  "JIS A 6909",            30.0, "pcs", "Denka Company",     6325, "Concrete tank lining",               "25 kg bags"));
    inventoryRepo.save(inventory(waterproofing, mi,  48.0,  30.0, "Tokyo Warehouse A – Rack 7-E", date(2026, 3, 28)));

    mi = itemRepo.save(item(waterproofing, 6,  "Dimple Drainage Board 8 mm",     "JIS A 6517",           150.0, "m²",  "Enkadrain Japan",    828, "Below-ground wall protection",       "8 mm dimple board"));
    inventoryRepo.save(inventory(waterproofing, mi, 320.0, 150.0, "Tokyo Warehouse A – Rack 7-F", date(2026, 4, 18)));

    mi = itemRepo.save(item(waterproofing, 7,  "Polyurethane Sealant Cartridge", "JIS A 5758",           100.0, "pcs", "Cemedine Co.",       1438, "Movement joint sealing",             "330 ml cartridges"));
    inventoryRepo.save(inventory(waterproofing, mi, 185.0, 100.0, "Tokyo Warehouse A – Rack 7-G", date(2026, 4, 12)));

    mi = itemRepo.save(item(waterproofing, 8,  "Bentonite Waterstop Strip",      "JIS A 6909",            80.0, "m",   "Volclay Japan",       920, "Construction joint sealing",         "20x25 mm profile"));
    inventoryRepo.save(inventory(waterproofing, mi, 145.0,  80.0, "Tokyo Warehouse A – Rack 7-H", date(2026, 4,  5)));

    mi = itemRepo.save(item(waterproofing, 9,  "PVC Waterstop 200 mm",           "JIS A 5371",            60.0, "m",   "Sika Japan",         1725, "In-situ concrete joints",            null));
    inventoryRepo.save(inventory(waterproofing, mi,  90.0,  60.0, "Tokyo Warehouse A – Rack 7-I", date(2026, 3, 20)));

    mi = itemRepo.save(item(waterproofing, 10, "Elastic Roof Coating",           "JIS A 6021",            40.0, "pcs", "Mapei Japan",        4370, "Parapet and detail waterproofing",   "10 kg tins"));
    inventoryRepo.save(inventory(waterproofing, mi,  65.0,  40.0, "Tokyo Warehouse A – Rack 7-J", date(2026, 4, 22)));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static ProcurementItem item(MaterialType type, int position,
      String description, String standard, double quantity, String unit,
      String supplier, double unitPrice, String purpose, String comment) {
    ProcurementItem i = new ProcurementItem();
    i.setMaterialTypeId(type.getId());
    i.setPosition(position);
    i.setMaterialDescription(description);
    i.setStandardGradeClass(standard);
    i.setQuantity(quantity);
    i.setUnit(unit);
    i.setSupplier(supplier);
    i.setUnitPriceNet(unitPrice);
    i.setPurpose(purpose);
    i.setComment(comment);
    i.calculateTotal();
    return i;
  }

  private static Date date(int year, int month, int day) {
    Calendar c = Calendar.getInstance();
    c.set(year, month - 1, day, 0, 0, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static InventoryItem inventory(MaterialType type, ProcurementItem item,
      double available, double reserved, String location, Date restocked) {
    InventoryItem inv = new InventoryItem(type.getId(), type.getName(), item.getId(), item.getUnit());
    inv.setQuantityAvailable(available);
    inv.setQuantityReserved(reserved);
    inv.setLocation(location);
    inv.setLastRestockedDate(restocked);
    return inv;
  }
}
