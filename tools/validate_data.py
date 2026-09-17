#!/usr/bin/env python3
"""
فحص سلامة بيانات المصحف في quran.json ومقارنتها بمراجع مستقلة:
  - quran.ar.uthmani.db  (نص الآيات — من حزمة iOS، أصلها quran-ios)
  - ayahinfo_1024.db      (مواقع الآيات على صور صفحات مصحف المدينة، طبعة 1405)
  - tools/reference/quran-ios-madani.json (بدايات الصفحات والأجزاء من quran-ios، طبعتا 1405 و 1440)

مستويات النتائج: خطأ (يُفشل الفحص) / مراجعة (يحتاج تأكيداً علمياً، لا يُفشل) / معلومة.

الاستخدام:
  python3 tools/validate_data.py                      # يفحص ملف الـSDK ويكتب docs/data-validation.md
  python3 tools/validate_data.py --json other.json    # يفحص ملفاً آخر
  python3 tools/validate_data.py --no-report          # بدون كتابة التقرير
  python3 tools/validate_data.py --edition 1440       # الطبعة المرجعية لأرقام الصفحات (الافتراضي 1405 = صور المصحف)

يرجع 1 إذا وُجد أي خطأ.
"""
import argparse
import json
import sqlite3
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path


def open_db(path):
    """فتح قاعدة SQLite للقراءة فقط بدون إنشاء ملفات جانبية (-wal/-shm) بجانب ملفات الحزمة."""
    return sqlite3.connect(f"file:{path}?mode=ro&immutable=1", uri=True)


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JSON = ROOT / "quranapp-main/quran-sdk/src/main/assets/quran.json"
BUNDLE = ROOT / "iOS-SDK/QuranSDK_QuranSDK.bundle"
REPORT = ROOT / "docs/data-validation.md"
REFERENCE = ROOT / "tools/reference/quran-ios-madani.json"

TOTAL_SURAS, TOTAL_AYAHS, TOTAL_PAGES, TOTAL_JUZ = 114, 6236, 604, 30
LINES_PER_PAGE = 15



ERROR, REVIEW, INFO = "error", "review", "info"


class Checker:
    def __init__(self):
        self.results = []  # (section, name, ok, detail, level)

    def check(self, section, name, ok, detail="", level=ERROR):
        self.results.append((section, name, bool(ok), detail, level))
        return ok

    @property
    def errors(self):
        return [r for r in self.results if not r[2] and r[4] == ERROR]

    @property
    def reviews(self):
        return [r for r in self.results if not r[2] and r[4] == REVIEW]


ODD_SPACES = "\u00a0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u202f\u205f\u3000\ufeff"


def sample(items, n=5):
    items = list(items)
    s = ", ".join(str(x) for x in items[:n])
    return s + (f" … (+{len(items) - n})" if len(items) > n else "")


def load(path):
    d = json.loads(Path(path).read_text(encoding="utf-8"))
    suras = d["suras"]
    ayahs = [a for s in suras for a in s["ayas"]]
    return d, suras, ayahs


def check_structure(c, suras, ayahs):
    sec = "الهيكل"
    c.check(sec, f"عدد السور = {TOTAL_SURAS}", len(suras) == TOTAL_SURAS, f"الموجود {len(suras)}")
    c.check(sec, "أرقام السور متسلسلة 1..114", [s["id"] for s in suras] == list(range(1, len(suras) + 1)))
    c.check(sec, f"عدد الآيات = {TOTAL_AYAHS}", len(ayahs) == TOTAL_AYAHS, f"الموجود {len(ayahs)}")
    c.check(sec, "معرّفات الآيات متسلسلة 1..6236", [a["id"] for a in ayahs] == list(range(1, len(ayahs) + 1)))

    bad_count = [s["id"] for s in suras if len(s["ayas"]) != s["aya_numbers"]]
    c.check(sec, "عدد آيات كل سورة = aya_numbers", not bad_count, f"سور مخالفة: {sample(bad_count)}")

    bad_seq = [s["id"] for s in suras if [a["aya"] for a in s["ayas"]] != list(range(1, len(s["ayas"]) + 1))]
    c.check(sec, "أرقام الآيات داخل كل سورة متسلسلة", not bad_seq, f"سور مخالفة: {sample(bad_seq)}")

    bad_parent = [a["id"] for s in suras for a in s["ayas"] if a["sura"] != s["id"]]
    c.check(sec, "حقل sura في الآية يطابق السورة الأم", not bad_parent, f"آيات مخالفة: {sample(bad_parent)}")

    empty = [a["id"] for a in ayahs if not (a.get("text") or "").strip() or not (a.get("aya_text") or "").strip()]
    c.check(sec, "كل آية لها نص عثماني ونص مبسط", not empty, f"آيات ناقصة: {sample(empty)}")

    odd = [a["id"] for a in ayahs if any(ch in (a.get("text") or "") + (a.get("aya_text") or "") for ch in ODD_SPACES)]
    c.check(sec, "لا توجد مسافات غير اعتيادية داخل النص (U+2009 وأخواتها)", not odd,
            f"آيات فيها مسافات غريبة: {sample(odd)}")


def check_pages(c, suras, ayahs):
    sec = "الصفحات"
    pages = [a["page"] for a in ayahs]
    out = [a["id"] for a in ayahs if not 1 <= a["page"] <= TOTAL_PAGES]
    c.check(sec, f"كل الصفحات بين 1 و {TOTAL_PAGES}", not out, f"آيات خارج النطاق: {sample(out)}")
    dec = [ayahs[i]["id"] for i in range(1, len(ayahs)) if pages[i] < pages[i - 1]]
    c.check(sec, "الصفحات تصاعدية مع ترتيب الآيات", not dec, f"تراجع عند الآيات: {sample(dec)}")
    missing = sorted(set(range(1, TOTAL_PAGES + 1)) - set(pages))
    c.check(sec, "كل صفحة من 1 إلى 604 فيها آية واحدة على الأقل", not missing, f"صفحات فارغة: {sample(missing)}")

    bad_start = [(s["id"], s["name_ar"], s["page_number"], s["ayas"][0]["page"])
                 for s in suras if s["page_number"] != s["ayas"][0]["page"]]
    c.check(sec, "page_number لكل سورة = صفحة أول آية فيها", not bad_start,
            "; ".join(f"سورة {i} ({n}): مكتوب {p} والصحيح {f}" for i, n, p, f in bad_start))

    zero = [a["id"] for a in ayahs if not a.get("line_start") or not a.get("line_end")]
    c.check(sec, "كل آية لها line_start و line_end", not zero, f"{len(zero)} آية بقيمة 0، أمثلة: {sample(zero)}")
    bad_lines = [a["id"] for a in ayahs if a.get("line_start") and not (
        (a["page"] - 1) * LINES_PER_PAGE < a["line_start"] <= a["page"] * LINES_PER_PAGE
        and a["line_end"] >= a["line_start"])]
    c.check(sec, "line_start يقع داخل أسطر صفحة الآية (15 سطراً للصفحة)", not bad_lines,
            f"آيات مخالفة: {sample(bad_lines)}")


def check_juz(c, data, suras, ayahs, ref):
    sec = "الأجزاء"
    missing = [a["id"] for a in ayahs if not a.get("chapter_id")]
    c.check(sec, "كل آية لها chapter_id (رقم الجزء)", not missing, f"آيات بدون جزء: {sample(missing)}")
    if missing:
        return
    juz = [a["chapter_id"] for a in ayahs]
    c.check(sec, "أرقام الأجزاء بين 1 و 30 وتصاعدية",
            all(1 <= j <= TOTAL_JUZ for j in juz) and all(juz[i] >= juz[i - 1] for i in range(1, len(juz))))

    first = {}
    for a in ayahs:
        first.setdefault(a["chapter_id"], a)
    actual = [(first[j]["sura"], first[j]["aya"]) for j in sorted(first)]
    c.check(sec, "عدد الأجزاء المستخدمة = 30", len(actual) == TOTAL_JUZ, f"الموجود {len(actual)}")
    expected = [tuple(q) for q in ref["quarters"][::8]]
    wrong = [(j + 1, actual[j], expected[j]) for j in range(min(len(actual), len(expected))) if actual[j] != expected[j]]
    c.check(sec, "بداية كل جزء تطابق quran-ios (Tanzil)", not wrong,
            "; ".join(f"جزء {j}: في الملف {a[0]}:{a[1]} وفي quran-ios {e[0]}:{e[1]}" for j, a, e in wrong)
            + " — يحتاج تأكيداً من مصدر معتمد (مجمع الملك فهد)", level=REVIEW)

    chapters = {ch["id"]: ch for ch in data.get("chapters", [])}
    c.check(sec, "قائمة chapters فيها 30 جزءاً", len(chapters) == TOTAL_JUZ, f"الموجود {len(chapters)}")
    bad_page = [(j, ch["page_number"], first[j]["page"]) for j, ch in chapters.items()
                if j in first and ch["page_number"] != first[j]["page"]]
    c.check(sec, "page_number لكل جزء = صفحة أول آية فيه", not bad_page,
            "; ".join(f"جزء {j}: مكتوب {p} والصحيح {f}" for j, p, f in bad_page))


def check_edition(c, ayahs, ref, edition):
    sec = f"أرقام الصفحات (مقارنة بطبعة {edition} في quran-ios)"
    c.check(sec, "عدد آيات كل سورة يطابق quran-ios",
            [len([a for a in ayahs if a["sura"] == s]) for s in range(1, 115)] == ref["numberOfAyahsInSura"])

    def pages_for(ed):
        starts = list(zip(ref[f"madani{ed}"]["startSuraOfPage"], ref[f"madani{ed}"]["startAyahOfPage"]))
        out, p = {}, 0
        for a in ayahs:
            while p + 1 < len(starts) and (a["sura"], a["aya"]) >= starts[p + 1]:
                p += 1
            out[a["id"]] = p + 1
        return out

    matches = {}
    for ed in ("1405", "1440"):
        pm = pages_for(ed)
        matches[ed] = [a["id"] for a in ayahs if a["page"] != pm[a["id"]]]
    c.check(sec, f"صفحة كل آية تطابق طبعة {edition}", not matches[edition],
            f"{len(matches[edition])} آية على صفحة مختلفة، أمثلة: {sample(matches[edition])}")
    other = "1440" if edition == "1405" else "1405"
    c.check(sec, "الطبعة التي تطابقها البيانات فعلاً", True,
            f"اختلافات مع 1405: {len(matches['1405'])} آية، ومع 1440: {len(matches['1440'])} آية", level=INFO)
    return matches


def nfc(s):
    return unicodedata.normalize("NFC", s).strip()


def check_text(c, ayahs):
    sec = "النص (مقارنة بـ quran.ar.uthmani.db)"
    db = BUNDLE / "quran.ar.uthmani.db"
    if not c.check(sec, "ملف المرجع موجود", db.exists(), str(db)):
        return
    con = open_db(db)
    ref = {(s, a): t for s, a, t in con.execute("select sura, ayah, text from arabic_text")}
    c.check(sec, f"المرجع فيه {TOTAL_AYAHS} آية", len(ref) == TOTAL_AYAHS, f"الموجود {len(ref)}")

    counts = defaultdict(int)
    for s, _ in ref:
        counts[s] += 1
    mine = defaultdict(int)
    for a in ayahs:
        mine[a["sura"]] += 1
    bad = [s for s in counts if counts[s] != mine.get(s)]
    c.check(sec, "عدد آيات كل سورة يطابق المرجع", not bad, f"سور مخالفة: {sample(bad)}")

    loose = lambda t: " ".join(nfc(t).replace("\u0640", "").split())
    diff = [a["id"] for a in ayahs if loose(a["text"]) != loose(ref.get((a["sura"], a["aya"]), ""))]
    c.check(sec, "النص العثماني لكل آية يطابق المرجع (مع تجاهل التطويل ـ)", not diff,
            f"{len(diff)} آية مختلفة، أمثلة: {sample(diff)}")
    tatweel = [a["id"] for a in ayahs if a["id"] not in diff and nfc(a["text"]) != nfc(ref.get((a["sura"], a["aya"]), ""))]
    c.check(sec, "فروق شكلية فقط (التطويل ـ قبل الألف الخنجرية)", True,
            f"{len(tatweel)} آية — اختلاف طباعي لا يغيّر النص", level=INFO)


def check_geometry(c, ayahs, edition):
    sec = "مواقع الآيات (مقارنة بـ ayahinfo_1024.db)"
    db = BUNDLE / "ayahinfo_1024.db"
    if not c.check(sec, "ملف المرجع موجود", db.exists(), str(db)):
        return []
    con = open_db(db)
    pages = defaultdict(set)
    for s, a, p in con.execute("select distinct sura_number, ayah_number, page_number from glyphs"):
        pages[(s, a)].add(p)
    no_geo = [a["id"] for a in ayahs if (a["sura"], a["aya"]) not in pages]
    c.check(sec, "كل آية لها مواقع على صفحة", not no_geo, f"آيات بدون مواقع: {sample(no_geo)}")
    wrong = [(a["id"], a["page"], sorted(pages[(a["sura"], a["aya"])])) for a in ayahs
             if (a["sura"], a["aya"]) in pages and a["page"] not in pages[(a["sura"], a["aya"])]]
    c.check(sec, "صفحة كل آية في quran.json من الصفحات التي تظهر عليها فعلاً في الصور", not wrong,
            f"{len(wrong)} آية، أمثلة: " + "; ".join(f"آية {i}: مكتوب {p} والفعلي {ps}" for i, p, ps in wrong[:4]),
            level=ERROR if edition == "1405" else INFO)

    spanning = [(a["id"], a["sura"], a["aya"], sorted(pages[(a["sura"], a["aya"])]), a["page"])
                for a in ayahs if len(pages.get((a["sura"], a["aya"]), ())) > 1]
    first_page = sum(1 for _, _, _, ps, p in spanning if p == ps[0])
    c.check(sec, "لا توجد آية ممتدة على صفحتين (صفحات مصحف المدينة تنتهي بنهاية آية)", not spanning,
            f"{len(spanning)} آية؛ quran.json يربط {first_page} منها بصفحة البداية و{len(spanning) - first_page} بغيرها", level=INFO)
    return spanning


def write_report(c, json_path, spanning, edition):
    lines = ["# تقرير سلامة بيانات المصحف", "",
             f"الملف المفحوص: `{Path(json_path).resolve().relative_to(ROOT) if str(json_path).startswith(str(ROOT)) else json_path}`  ",
             "يُولَّد تلقائياً بـ `python3 tools/validate_data.py`.", "",
             f"**الطبعة المرجعية لأرقام الصفحات:** {edition}  ",
             f"**النتيجة:** {'✅ ناجح' if not c.errors else f'❌ {len(c.errors)} خطأ'}"
             f" · ⚠️ {len(c.reviews)} بند يحتاج مراجعة"
             f" ({sum(1 for r in c.results if r[2])} من {len(c.results)} فحصاً ناجح)", "",
             "الرموز: ✅ ناجح · ❌ خطأ · ⚠️ يحتاج تأكيداً علمياً · ℹ️ معلومة", ""]
    section = None
    for sec, name, ok, detail, level in c.results:
        if sec != section:
            lines += ["", f"## {sec}", "", "| الفحص | النتيجة | التفاصيل |", "|---|---|---|"]
            section = sec
        mark = "ℹ️" if level == INFO else "✅" if ok else "⚠️" if level == REVIEW else "❌"
        shown = detail if (not ok or level == INFO) else ""
        lines.append(f"| {name} | {mark} | {shown} |")
    if spanning:
        lines += ["", "## الآيات الممتدة على صفحتين", "",
                  "تبدأ الآية في صفحة وتنتهي في التي تليها. `quran.json` يربطها بصفحة واحدة، "
                  "ولذلك يجب أن يعتمد التظليل على `ayahinfo` (معرّف الآية) لا على رقم الصفحة.", "",
                  "| المعرّف | السورة:الآية | الصفحات الفعلية | الصفحة في quran.json |", "|---|---|---|---|"]
        lines += [f"| {i} | {s}:{a} | {'، '.join(map(str, ps))} | {p} |" for i, s, a, ps, p in spanning]
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", default=str(DEFAULT_JSON))
    ap.add_argument("--no-report", action="store_true")
    ap.add_argument("--edition", choices=["1405", "1440"], default="1405")
    args = ap.parse_args()

    data, suras, ayahs = load(args.json)
    ref = json.loads(REFERENCE.read_text(encoding="utf-8"))
    c = Checker()
    check_structure(c, suras, ayahs)
    check_pages(c, suras, ayahs)
    check_edition(c, ayahs, ref, args.edition)
    check_juz(c, data, suras, ayahs, ref)
    check_text(c, ayahs)
    spanning = check_geometry(c, ayahs, args.edition)

    for sec, name, ok, detail, level in c.results:
        tag = "INFO" if level == INFO else "OK  " if ok else "REVW" if level == REVIEW else "FAIL"
        print(f"{tag} [{sec}] {name}" + (f" — {detail}" if detail and (not ok or level == INFO) else ""))
    print(f"\nerrors: {len(c.errors)} · review: {len(c.reviews)} · checks: {len(c.results)}")
    if not args.no_report:
        write_report(c, args.json, spanning, args.edition)
        print(f"report: {REPORT.relative_to(ROOT)}")
    return 1 if c.errors else 0


if __name__ == "__main__":
    sys.exit(main())
