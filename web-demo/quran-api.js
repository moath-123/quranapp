// نسخة JavaScript من واجهة QuranApi في quran-sdk (أندرويد) — نفس أسماء الدوال ونفس شكل النماذج.
(function () {
  const JUZ_NAMES = ["آلم", "سَيَقُولُ", "تِلْكَ ٱلْرُّسُلُ", "لَنْ تَنَالُوا", "وَٱلْمُحْصَنَاتُ", "لَا يُحِبُّ ٱللهُ",
    "وَإِذَا سَمِعُوا", "وَلَوْ أَنَّنَا", "قَالَ ٱلْمَلَأُ", "وَٱعْلَمُواْ", "يَعْتَذِرُونَ", "وَمَا مِنْ دَآبَّةٍ",
    "وَمَا أُبَرِّئُ", "رُبَمَا", "سُبْحَانَ ٱلَّذِى", "قَالَ أَلَمْ", "ٱقْتَرَبَ لِلْنَّاسِ", "قَدْ أَفْلَحَ",
    "وَقَالَ ٱلَّذِينَ", "أَمَّنْ خَلَقَ", "أُتْلُ مَاأُوْحِیَ", "وَمَنْ يَّقْنُتْ", "وَمَآ لي", "فَمَنْ أَظْلَمُ",
    "إِلَيْهِ يُرَدُّ", "حم", "قَالَ فَمَا خَطْبُكُم", "قَدْ سَمِعَ ٱللهُ", "تَبَارَكَ ٱلَّذِى", "عَمَّ"];

  // ArabicNormalizer.kt
  const DIACRITICS = /[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]/g;
  function normalize(input) {
    if (!input || !input.trim()) return "";
    return input.replace(DIACRITICS, "").replace(/\u0640/g, "")
      .replace(/[أإآٱ]/g, "ا").replace(/ى/g, "ي").replace(/ؤ/g, "و").replace(/ئ/g, "ي")
      .replace(/[﴿﴾،؛؟]/g, " ").trim().replace(/\s+/g, " ");
  }
  function extractPageNumber(input) {
    const d = input.replace(/[٠-٩]/g, (c) => String(c.charCodeAt(0) - 0x0660));
    const m = d.match(/\d{1,4}/);
    return m ? parseInt(m[0], 10) : null;
  }

  let db = null; // يُبنى عند initialize()

  function build() {
    const raw = window.QURAN_DATA;
    const suras = raw.suras.map(([number, nameAr, nameEn, ayahCount, startPage]) =>
      ({ number, nameAr, nameEn, ayahCount, startPage }));
    const juz = raw.juz.map(([number, nameAr, nameEn, startPage]) => ({ number, nameAr, nameEn, startPage }));
    const ayahs = raw.ayahs.map(([id, s, a, page, j, uth, simple, ls, le]) => ({
      id, surahNumber: s, ayahNumber: a, juzNumber: j, pageNumber: page,
      textUthmani: uth, textSimple: simple, lineStart: ls, lineEnd: le
    }));
    const byPage = new Map();
    for (const a of ayahs) {
      if (!byPage.has(a.pageNumber)) byPage.set(a.pageNumber, []);
      byPage.get(a.pageNumber).push(a);
    }
    const norm = ayahs.map((a) => normalize(a.textSimple));
    return { suras, juz, ayahs, byPage, norm, maxPage: Math.max(...byPage.keys()) };
  }

  const clone = (o) => (o ? { ...o } : null);

  window.QuranApi = {
    normalize,
    juzNameAr: (n) => JUZ_NAMES[n - 1] || `الجزء ${n}`,

    async initialize(onProgress) {
      if (db) return;
      onProgress && onProgress(0.3, "قراءة quran.json…");
      await new Promise((r) => setTimeout(r, 120));
      db = build();
      onProgress && onProgress(1, "تم");
    },
    getSurahs: () => db.suras.map(clone),
    getJuzList: () => db.juz.map(clone),
    getAyahsByPage: (page) => (db.byPage.get(page) || []).map(clone),
    getAyahsBySurah: (n) => db.ayahs.filter((a) => a.surahNumber === n).map(clone),
    getAyahsByJuz: (n) => db.ayahs.filter((a) => a.juzNumber === n).map(clone),
    getAyahById: (id) => clone(db.ayahs[id - 1]),
    getFirstAyahOnPage: (page) => clone((db.byPage.get(page) || [])[0]),
    getMaxPage: () => db.maxPage,

    // QuranRepository.searchArabic: رقم صفحة أو نص عربي، الحد الافتراضي 200
    search(query, limit = 200) {
      const q = normalize(query);
      if (!q) return [];
      const toResult = (a) => {
        const s = db.suras[a.surahNumber - 1];
        const j = db.juz[a.juzNumber - 1];
        return {
          ayahId: a.id, ayahNumber: a.ayahNumber, surahNumber: a.surahNumber,
          surahNameAr: s.nameAr, surahNameEn: s.nameEn,
          textUthmani: a.textUthmani, textSimple: a.textSimple,
          pageNumber: a.pageNumber, juzNumber: a.juzNumber,
          juzNameAr: j ? j.nameAr : "", juzNameEn: j ? j.nameEn : ""
        };
      };
      const out = [];
      if (/^[\d٠-٩\s]+$/.test(query.trim())) {
        const p = extractPageNumber(query);
        if (p && db.byPage.has(p)) return db.byPage.get(p).slice(0, limit).map(toResult);
      }
      for (let i = 0; i < db.ayahs.length && out.length < limit; i++) {
        if (db.norm[i].includes(q)) out.push(toResult(db.ayahs[i]));
      }
      return out;
    }
  };
})();
