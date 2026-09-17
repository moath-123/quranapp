#!/usr/bin/env python3
"""
إصلاح quran.json ليتطابق مع صور صفحات مصحف المدينة (طبعة 1405) ومواقع ayahinfo_1024.db.

التعديلات (كلها مشتقة من مراجع مستقلة، ولا تمس نص الآيات إلا بإزالة مسافة دخيلة):
  1. page لكل آية        ← الصفحة التي تبدأ عليها الآية في ayahinfo_1024.db (يطابق quran-ios 1405 تماماً)
  2. line_start/line_end ← من ayahinfo: (الصفحة - 1) × 15 + رقم السطر (كانت 0 في 360 آية)
  3. page_number للسورة  ← صفحة أول آية فيها (الفاتحة كانت 2)
  4. page_number للجزء   ← صفحة أول آية فيه
  5. إزالة المسافات غير الاعتيادية داخل النص (U+2009 في البقرة 72 كانت تقسم كلمة «فَٱدَّٰرَٰٔتُمۡ»)

لا يغيّر: حدود الأجزاء (chapter_id) — تحتاج تأكيداً علمياً (انظر docs/data-validation.md).

الاستخدام:
  python3 tools/fix_quran_json.py            # يعدّل ملفي quran.json في مشروع أندرويد
  python3 tools/fix_quran_json.py --check    # يعرض التغييرات بدون حفظ
"""
import argparse
import json
import re
import sqlite3
import sys
from collections import defaultdict
from pathlib import Path


def open_db(path):
    """فتح قاعدة SQLite للقراءة فقط بدون إنشاء ملفات جانبية (-wal/-shm) بجانب ملفات الحزمة."""
    return sqlite3.connect(f"file:{path}?mode=ro&immutable=1", uri=True)


ROOT = Path(__file__).resolve().parent.parent
TARGETS = [
    ROOT / "quranapp-main/quran-sdk/src/main/assets/quran.json",
    ROOT / "quranapp-main/app/src/main/assets/quran.json",
]
AYAHINFO = ROOT / "iOS-SDK/QuranSDK_QuranSDK.bundle/ayahinfo_1024.db"
LINES_PER_PAGE = 15
ODD_SPACES = re.compile("[  -​  　﻿]")


def geometry():
    con = open_db(AYAHINFO)
    rows = con.execute(
        "select sura_number, ayah_number, page_number, min(line_number), max(line_number) "
        "from glyphs group by 1, 2, 3 order by 1, 2, 3")
    geo = defaultdict(list)
    for s, a, p, l1, l2 in rows:
        geo[(s, a)].append((p, l1, l2))
    return geo


def fix(data, geo):
    changes = defaultdict(list)
    first_of_juz = {}
    for sura in data["suras"]:
        for a in sura["ayas"]:
            spans = geo[(a["sura"], a["aya"])]
            start_page, start_line, _ = spans[0]
            end_page, _, end_line = spans[-1]
            page = start_page
            line_start = (start_page - 1) * LINES_PER_PAGE + start_line
            line_end = (end_page - 1) * LINES_PER_PAGE + end_line
            if a["page"] != page:
                changes["page"].append((a["id"], a["page"], page))
                a["page"] = page
            if (a["line_start"], a["line_end"]) != (line_start, line_end):
                changes["lines"].append((a["id"], (a["line_start"], a["line_end"]), (line_start, line_end)))
                a["line_start"], a["line_end"] = line_start, line_end
            for field in ("text", "aya_text"):
                cleaned = ODD_SPACES.sub("", a[field])
                if cleaned != a[field]:
                    changes["spaces"].append((a["id"], field))
                    a[field] = cleaned
            first_of_juz.setdefault(a["chapter_id"], a)

        start = sura["ayas"][0]["page"]
        if sura["page_number"] != start:
            changes["sura_page"].append((sura["id"], sura["page_number"], start))
            sura["page_number"] = start

    # قائمة chapters العامة فقط. النسخ المكررة داخل كل سورة (sura.chapters) معناها صفحة السورة داخل الجزء،
    # فلا نلمسها — والـSDK يجب ألا يعتمد عليها لبدايات الأجزاء (انظر docs/findings.md).
    for ch in data.get("chapters", []):
        start = first_of_juz[ch["id"]]["page"]
        if ch["page_number"] != start:
            changes["juz_page"].append((ch["id"], ch["page_number"], start))
            ch["page_number"] = start
    return changes


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="عرض التغييرات بدون حفظ")
    args = ap.parse_args()
    geo = geometry()

    for path in TARGETS:
        raw = path.read_text(encoding="utf-8")
        data = json.loads(raw)
        # نتأكد أن إعادة الحفظ لا تغيّر التنسيق قبل أي تعديل
        assert json.dumps(data, ensure_ascii=False, indent=2) == raw, f"تنسيق غير متوقع في {path}"
        changes = fix(data, geo)
        print(f"== {path.relative_to(ROOT)}")
        for kind, items in changes.items():
            print(f"  {kind}: {len(items)}  e.g. {items[:3]}")
        if not args.check and changes:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
            print("  saved")
    return 0


if __name__ == "__main__":
    sys.exit(main())
