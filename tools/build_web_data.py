#!/usr/bin/env python3
"""
توليد بيانات النموذج التجريبي (web-demo) من مصادر الـSDK:
  web-demo/data.js        ← quran.json (السور، الأجزاء، الآيات)
  web-demo/page-rects.js  ← ayahinfo_1024.db (مستطيلات تظليل الآيات لكل صفحة، بمقاس صورة 1024)

الاستخدام: python3 tools/build_web_data.py
"""
import json
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QURAN_JSON = ROOT / "quranapp-main/quran-sdk/src/main/assets/quran.json"
AYAHINFO = ROOT / "iOS-SDK/QuranSDK_QuranSDK.bundle/ayahinfo_1024.db"
OUT = ROOT / "web-demo"


def build_data(d):
    suras = [[s["id"], s["name_ar"], s["name_en"], s["aya_numbers"], s["page_number"]] for s in d["suras"]]
    juz = [[c["id"], c["name_ar"], c["name_en"], c["page_number"]] for c in d["chapters"]]
    ayahs = [[a["id"], a["sura"], a["aya"], a["page"], a["chapter_id"], a["text"], a["aya_text"],
              a["line_start"], a["line_end"]] for s in d["suras"] for a in s["ayas"]]
    return {"suras": suras, "juz": juz, "ayahs": ayahs}


def build_rects(d):
    ids = {(a["sura"], a["aya"]): a["id"] for s in d["suras"] for a in s["ayas"]}
    con = sqlite3.connect(AYAHINFO)
    # كل سطر: مستطيل واحد لكل آية بعرضها الفعلي، وبارتفاع السطر كاملاً ليكون التظليل متناسقاً
    line_band = {(p, l): (y1, y2) for p, l, y1, y2 in con.execute(
        "select page_number, line_number, min(min_y), max(max_y) from glyphs group by 1, 2")}
    pages = {}
    for p, l, s, a, x1, x2 in con.execute(
            "select page_number, line_number, sura_number, ayah_number, min(min_x), max(max_x) from glyphs "
            "group by 1, 2, 3, 4 order by 1, 2, min(min_x) desc"):
        y1, y2 = line_band[(p, l)]
        pages.setdefault(p, []).append([ids[(s, a)], x1, y1, x2, y2])
    return pages


def main():
    d = json.loads(QURAN_JSON.read_text(encoding="utf-8"))
    data = build_data(d)
    (OUT / "data.js").write_text(
        "window.QURAN_DATA=" + json.dumps(data, ensure_ascii=False, separators=(",", ":")) + ";", encoding="utf-8")
    rects = build_rects(d)
    (OUT / "page-rects.js").write_text(
        "window.PAGE_RECTS=" + json.dumps(rects, separators=(",", ":")) + ";", encoding="utf-8")
    print(f"data.js: {len(data['suras'])} suras, {len(data['ayahs'])} ayahs · page-rects.js: {len(rects)} pages")


if __name__ == "__main__":
    main()
