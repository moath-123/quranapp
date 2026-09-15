// تطبيق العميل: يستخدم QuranApi فقط (مثل التطبيق التجريبي في quranapp-main/app/sdktest)
(function () {
  const api = window.QuranApi;
  const $ = (id) => document.getElementById(id);
  const toAr = (n) => String(n).replace(/\d/g, (d) => "٠١٢٣٤٥٦٧٨٩"[d]);
  const esc = (s) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const BISMILLAH = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ";

  const store = {
    get(k, d) { try { const v = localStorage.getItem("qd." + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
    set(k, v) { try { localStorage.setItem("qd." + k, JSON.stringify(v)); } catch { /* ignore */ } }
  };

  let SURAS = [], JUZ = [], MAX_PAGE = 604;
  let currentPage = store.get("page", 1);
  let fontSize = store.get("font", 25);
  let bookmarks = store.get("bookmarks", []);
  let currentTab = "index";

  // ---------------- toast ----------------
  let toastTimer;
  function toast(msg) {
    const t = $("toast");
    t.textContent = msg; t.hidden = false;
    clearTimeout(toastTimer); toastTimer = setTimeout(() => (t.hidden = true), 1800);
  }

  // ---------------- tabs ----------------
  function showTab(tab) {
    currentTab = tab;
    document.querySelectorAll(".screen").forEach((s) => (s.hidden = s.id !== "screen-" + tab));
    document.querySelectorAll(".tab").forEach((b) => b.classList.toggle("active", b.dataset.tab === tab));
    hidePopup(); $("ayahBar").hidden = true;
    if (tab === "index") renderContinue();
    if (tab === "reader") fitSheet();
    if (tab === "bookmarks") renderBookmarks();
    if (tab === "search") setTimeout(() => $("searchInput").focus(), 50);
  }
  document.querySelectorAll(".tab").forEach((b) => b.addEventListener("click", () => showTab(b.dataset.tab)));

  // ---------------- index ----------------
  function renderIndex() {
    $("suraList").innerHTML = SURAS.map((s) => `
      <li><button class="row" data-page="${s.startPage}">
        <span class="num">${toAr(s.number)}</span>
        <span class="row-main"><span class="row-title">سورة ${esc(s.nameAr)}</span>
          <span class="row-sub">${toAr(s.ayahCount)} آية، <bdi>${esc(s.nameEn)}</bdi></span></span>
        <span class="row-end">صفحة<b>${toAr(s.startPage)}</b></span>
      </button></li>`).join("");
    $("juzList").innerHTML = JUZ.map((j) => `
      <li><button class="row" data-page="${j.startPage}">
        <span class="num">${toAr(j.number)}</span>
        <span class="row-main"><span class="row-title">${esc(j.nameAr)}</span>
          <span class="row-sub">${esc(api.juzNameAr(j.number))}</span></span>
        <span class="row-end">صفحة<b>${toAr(j.startPage)}</b></span>
      </button></li>`).join("");
    for (const ul of [$("suraList"), $("juzList")]) {
      ul.addEventListener("click", (e) => {
        const b = e.target.closest("[data-page]");
        if (b) openPage(+b.dataset.page);
      });
    }
    document.querySelectorAll(".seg").forEach((b) => b.addEventListener("click", () => {
      document.querySelectorAll(".seg").forEach((x) => x.classList.toggle("active", x === b));
      $("suraList").hidden = b.dataset.seg !== "suras";
      $("juzList").hidden = b.dataset.seg !== "juz";
    }));
    $("continueCard").addEventListener("click", () => openPage(currentPage));
  }

  function renderContinue() {
    const a = api.getFirstAyahOnPage(currentPage);
    if (!a) return;
    $("continueTitle").textContent = "سورة " + SURAS[a.surahNumber - 1].nameAr;
    $("continueMeta").textContent = `صفحة ${toAr(currentPage)}، الجزء ${toAr(a.juzNumber)}`;
  }

  // ---------------- reader (QuranPageView) ----------------
  // وضعين: "mushaf" صور صفحات مصحف المدينة (مثل iOS SDK) + ayahinfo_1024.db للمس الآيات، و"text" نص عثماني (مثل Android SDK)
  const pageEl = $("page"), sheetEl = $("sheet"), imgEl = $("pageImg"), svgEl = $("pageMarks");
  const IMG_W = 1024, IMG_H = 1656;
  let mode = store.get("mode", "mushaf");
  const marks = { tapped: null, pressed: null };
  const isBookmarked = (id) => bookmarks.some((b) => b.ayahId === id);
  const pageSrc = (p) => `pages/page${String(p).padStart(3, "0")}.png`;

  function updateHeader(p) {
    const first = api.getFirstAyahOnPage(p);
    $("rSurah").textContent = first ? SURAS[first.surahNumber - 1].nameAr : "";
    $("rPage").textContent = toAr(p);
    $("rJuz").textContent = first ? api.juzNameAr(first.juzNumber) : "";
    $("pageSlider").value = p;
  }

  function buildTextPage(p) {
    let html = "";
    for (const a of api.getAyahsByPage(p)) {
      if (a.ayahNumber === 1) {
        html += `<span class="sura-head">سورة ${esc(SURAS[a.surahNumber - 1].nameAr)}</span>`;
        if (a.surahNumber !== 1 && a.surahNumber !== 9) html += `<span class="bismillah">${BISMILLAH}</span>`;
      }
      html += `<span class="ayah" data-id="${a.id}">${esc(a.textUthmani)} <span class="marker">﴿${toAr(a.ayahNumber)}﴾</span></span> `;
    }
    return html;
  }

  function renderMarks() {
    if (mode === "text") {
      pageEl.querySelectorAll(".ayah").forEach((el) => {
        const id = +el.dataset.id;
        el.classList.toggle("tapped", id === marks.tapped);
        el.classList.toggle("pressed", id === marks.pressed);
        el.classList.toggle("bookmarked", isBookmarked(id));
      });
      return;
    }
    svgEl.innerHTML = (window.PAGE_RECTS[currentPage] || []).map(([id, x1, y1, x2, y2]) => {
      const cls = id === marks.pressed ? "m-press" : id === marks.tapped ? "m-tap" : isBookmarked(id) ? "m-bm" : "";
      return cls ? `<rect class="${cls}" x="${x1 - 6}" y="${y1 - 4}" width="${x2 - x1 + 12}" height="${y2 - y1 + 8}" rx="12"/>` : "";
    }).join("");
  }

  // الصفحة تاخذ أكبر مساحة ممكنة مع الحفاظ على نسبة الصورة (الإطار مرسوم خارجها بـbox-shadow)
  function fitSheet() {
    const wrap = $("pageWrap");
    const s = Math.min((wrap.clientWidth - 30) / IMG_W, (wrap.clientHeight - 26) / IMG_H);
    if (s > 0) { sheetEl.style.width = IMG_W * s + "px"; sheetEl.style.height = IMG_H * s + "px"; }
  }
  window.addEventListener("resize", fitSheet);

  function applyMode() {
    pageEl.hidden = mode !== "text";
    $("mushafView").hidden = mode !== "mushaf";
    $("fontCtrl").hidden = mode !== "text";
    document.querySelectorAll("#modeToggle button").forEach((b) => b.classList.toggle("active", b.dataset.mode === mode));
    store.set("mode", mode);
  }
  $("modeToggle").addEventListener("click", (e) => {
    const b = e.target.closest("[data-mode]");
    if (!b || b.dataset.mode === mode) return;
    mode = b.dataset.mode; applyMode(); goToPage(currentPage, null, marks.tapped);
  });

  function renderPage(p, highlightId) {
    updateHeader(p);
    marks.tapped = highlightId || null; marks.pressed = null;
    if (mode === "text") {
      pageEl.innerHTML = buildTextPage(p);
      pageEl.scrollTop = 0;
      renderMarks();
      const el = highlightId && pageEl.querySelector(`[data-id="${highlightId}"]`);
      if (el) setTimeout(() => el.scrollIntoView({ block: "center" }), 30);
    } else {
      imgEl.src = pageSrc(p);
      fitSheet(); renderMarks();
      for (const n of [p - 1, p + 1]) if (n >= 1 && n <= MAX_PAGE) new Image().src = pageSrc(n);
    }
  }

  function goToPage(p, dir, highlightId) {
    p = Math.max(1, Math.min(MAX_PAGE, p));
    hidePopup(); $("ayahBar").hidden = true;
    currentPage = p; store.set("page", p);
    if (!dir) return renderPage(p, highlightId);
    const el = mode === "text" ? pageEl : sheetEl;
    el.classList.add(dir === "next" ? "slide-next" : "slide-prev");
    setTimeout(() => {
      el.classList.remove("slide-next", "slide-prev");
      el.classList.add(dir === "next" ? "enter-next" : "enter-prev");
      renderPage(p, highlightId);
      void el.offsetWidth;
      el.classList.remove("enter-next", "enter-prev");
    }, 180);
  }

  function openPage(p, highlightId) {
    showTab("reader");
    goToPage(p, null, highlightId);
  }

  const next = () => currentPage < MAX_PAGE && goToPage(currentPage + 1, "next");
  const prev = () => currentPage > 1 && goToPage(currentPage - 1, "prev");
  $("nextBtn").addEventListener("click", next);
  $("prevBtn").addEventListener("click", prev);
  $("pageSlider").addEventListener("input", (e) => { $("rPage").textContent = toAr(e.target.value); });
  $("pageSlider").addEventListener("change", (e) => goToPage(+e.target.value));
  document.addEventListener("keydown", (e) => {
    if (currentTab !== "reader") return;
    if (e.key === "ArrowLeft") next();
    if (e.key === "ArrowRight") prev();
    if (e.key === "Escape") { hidePopup(); clearSelection(); }
  });

  function applyFont() { document.documentElement.style.setProperty("--ayah-size", fontSize + "px"); store.set("font", fontSize); }
  $("fontUp").addEventListener("click", () => { fontSize = Math.min(40, fontSize + 2); applyFont(); });
  $("fontDown").addEventListener("click", () => { fontSize = Math.max(16, fontSize - 2); applyFont(); });

  const wrapEl = $("pageWrap");
  // سحب أفقي: في المصحف العربي السحب لليمين = الصفحة التالية
  let sx = 0, sy = 0, swiping = false;
  wrapEl.addEventListener("touchstart", (e) => { sx = e.touches[0].clientX; sy = e.touches[0].clientY; swiping = true; }, { passive: true });
  wrapEl.addEventListener("touchend", (e) => {
    if (!swiping) return; swiping = false;
    const dx = e.changedTouches[0].clientX - sx, dy = e.changedTouches[0].clientY - sy;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) { cancelPress(); dx > 0 ? next() : prev(); }
  });

  // أي آية تحت الإصبع؟ في وضع الصور نحوّل الإحداثيات لمقاس 1024 ونقارن بمستطيلات ayahinfo
  function ayahAt(e) {
    if (mode === "text") {
      const el = e.target.closest && e.target.closest(".ayah");
      return el ? +el.dataset.id : null;
    }
    const r = sheetEl.getBoundingClientRect();
    if (!r.width) return null;
    const x = (e.clientX - r.left) * IMG_W / r.width, y = (e.clientY - r.top) * IMG_H / r.height;
    let best = null, bestD = 16;
    for (const [id, x1, y1, x2, y2] of window.PAGE_RECTS[currentPage] || []) {
      if (y < y1 - 4 || y > y2 + 4) continue;
      const d = x < x1 ? x1 - x : x > x2 ? x - x2 : 0;
      if (d < bestD) { bestD = d; best = id; }
    }
    return best;
  }

  // ---- ضغط / ضغط مطوّل على الآية: onAyahTapped / onAyahLongPressed ----
  let pressTimer = null, pressStart = null, didLong = false;
  function cancelPress() {
    clearTimeout(pressTimer); pressTimer = null;
    if (marks.pressed) { marks.pressed = null; renderMarks(); }
  }

  wrapEl.addEventListener("pointerdown", (e) => {
    const id = ayahAt(e);
    if (!id || e.button === 2) return;
    didLong = false; pressStart = { x: e.clientX, y: e.clientY };
    marks.pressed = id; renderMarks();
    pressTimer = setTimeout(() => {
      didLong = true; pressTimer = null; marks.pressed = null;
      onAyahLongPressed(id, e.clientX, e.clientY);
    }, 480);
  });
  wrapEl.addEventListener("pointermove", (e) => {
    if (pressTimer && Math.hypot(e.clientX - pressStart.x, e.clientY - pressStart.y) > 10) cancelPress();
  });
  wrapEl.addEventListener("pointerup", (e) => {
    const wasPressing = !!pressTimer;
    cancelPress();
    if (!wasPressing || didLong) return;
    const id = ayahAt(e);
    if (id) onAyahTapped(id);
  });
  wrapEl.addEventListener("pointercancel", cancelPress);
  wrapEl.addEventListener("contextmenu", (e) => {
    e.preventDefault();
    const id = ayahAt(e);
    if (id) { cancelPress(); onAyahLongPressed(id, e.clientX, e.clientY); }
  });
  wrapEl.addEventListener("click", (e) => { if (!ayahAt(e)) { hidePopup(); clearSelection(); } });

  function clearSelection() { marks.tapped = null; renderMarks(); $("ayahBar").hidden = true; }

  let barTimer;
  function onAyahTapped(id) {
    hidePopup();
    if (marks.tapped === id) return clearSelection();
    const a = api.getAyahById(id);
    marks.tapped = id; renderMarks();
    const bar = $("ayahBar");
    bar.innerHTML = `<span>سورة ${esc(SURAS[a.surahNumber - 1].nameAr)}، الآية ${toAr(a.ayahNumber)}</span>
      <small>onAyahTapped · id ${a.id}</small>`;
    bar.hidden = false;
    clearTimeout(barTimer); barTimer = setTimeout(() => (bar.hidden = true), 3500);
  }

  let popupAyah = null;
  function onAyahLongPressed(id, x, y) {
    $("ayahBar").hidden = true;
    const a = api.getAyahById(id);
    popupAyah = a;
    marks.tapped = id; renderMarks();
    $("popupHead").textContent = `سورة ${SURAS[a.surahNumber - 1].nameAr}، الآية ${toAr(a.ayahNumber)}، صفحة ${toAr(a.pageNumber)}`;
    $("popBookmark").textContent = isBookmarked(a.id) ? "☆ إزالة من المفضلة" : "★ إضافة للمفضلة";
    const pop = $("popup"), dev = document.querySelector(".device").getBoundingClientRect();
    pop.hidden = false;
    const w = pop.offsetWidth, h = pop.offsetHeight;
    pop.style.left = Math.max(8, Math.min(x - dev.left - w / 2, dev.width - w - 8)) + "px";
    pop.style.top = Math.max(8, Math.min(y - dev.top + 12, dev.height - h - 70)) + "px";
  }
  function hidePopup() { $("popup").hidden = true; popupAyah = null; }

  $("popup").addEventListener("click", async (e) => {
    const act = e.target.dataset.act;
    if (!act || !popupAyah) return;
    const a = popupAyah;
    if (act === "bookmark") toggleBookmark(a);
    if (act === "copy") {
      const txt = `${a.textUthmani} ﴿${toAr(a.ayahNumber)}﴾ [${SURAS[a.surahNumber - 1].nameAr}]`;
      try { await navigator.clipboard.writeText(txt); toast("تم نسخ الآية"); } catch { toast("تعذّر النسخ"); }
    }
    hidePopup(); clearSelection();
  });

  // ---------------- bookmarks (ClientBookmarkStore) ----------------
  function toggleBookmark(a) {
    if (isBookmarked(a.id)) {
      bookmarks = bookmarks.filter((b) => b.ayahId !== a.id);
      toast("أُزيلت من المفضلة");
    } else {
      bookmarks.unshift({ ayahId: a.id, surahNumber: a.surahNumber, ayahNumber: a.ayahNumber, pageNumber: a.pageNumber, text: a.textUthmani, savedAt: Date.now() });
      toast("أُضيفت للمفضلة ★");
    }
    store.set("bookmarks", bookmarks);
    renderMarks();
  }

  function renderBookmarks() {
    const ul = $("bookmarkList");
    if (!bookmarks.length) {
      ul.innerHTML = `<li class="empty"><span class="big">★</span>ما فيه آيات محفوظة.<br>اضغط مطوّلاً على أي آية في المصحف واختر «إضافة للمفضلة».</li>`;
      return;
    }
    ul.innerHTML = bookmarks.map((b) => `
      <li><div class="row" data-open="${b.ayahId}" data-page="${b.pageNumber}">
        <span class="num">${toAr(b.ayahNumber)}</span>
        <span class="row-main"><span class="row-sub">سورة ${esc(SURAS[b.surahNumber - 1].nameAr)}، صفحة ${toAr(b.pageNumber)}</span>
          <span class="bm-text">${esc(b.text)}</span></span>
        <button class="bm-del" data-del="${b.ayahId}" aria-label="حذف">✕</button>
      </div></li>`).join("");
  }
  $("bookmarkList").addEventListener("click", (e) => {
    const del = e.target.closest("[data-del]");
    if (del) {
      bookmarks = bookmarks.filter((b) => b.ayahId !== +del.dataset.del);
      store.set("bookmarks", bookmarks); renderBookmarks(); return;
    }
    const row = e.target.closest("[data-open]");
    if (row) openPage(+row.dataset.page, +row.dataset.open);
  });

  // ---------------- search ----------------
  const DIAC = "[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]*";
  const CHAR_CLASS = { "ا": "[اأإآٱٰ]", "ي": "[يىئ]", "و": "[وؤ]" };
  function highlightRegex(q) {
    const parts = [...api.normalize(q)].map((c) =>
      c === " " ? "\\s+" : CHAR_CLASS[c] || c.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
    try { return new RegExp(parts.join(DIAC) + DIAC, "g"); } catch { return null; }
  }
  function highlight(text, re) {
    if (!re) return esc(text);
    let out = "", last = 0, m, found = false;
    re.lastIndex = 0;
    while ((m = re.exec(text))) {
      if (!m[0]) { re.lastIndex++; continue; }
      found = true;
      out += esc(text.slice(last, m.index)) + "<mark>" + esc(m[0]) + "</mark>";
      last = m.index + m[0].length;
    }
    return found ? out + esc(text.slice(last)) : null;
  }

  let searchTimer;
  $("searchInput").addEventListener("input", (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => runSearch(e.target.value), 180);
  });
  function runSearch(q) {
    const ul = $("results"), meta = $("searchMeta");
    if (!q.trim()) { ul.innerHTML = ""; meta.textContent = ""; return; }
    const t0 = performance.now();
    const res = api.search(q, 200);
    const ms = Math.round(performance.now() - t0);
    meta.textContent = res.length ? `${toAr(res.length)}${res.length === 200 ? "+" : ""} نتيجة، ${toAr(ms)} ملّي ثانية` : "";
    if (!res.length) { ul.innerHTML = `<li class="empty"><span class="big">؟</span>لا توجد نتائج</li>`; return; }
    const re = /^[\d٠-٩\s]+$/.test(q.trim()) ? null : highlightRegex(q);
    ul.innerHTML = res.map((r) => {
      const text = highlight(r.textUthmani, re) ?? highlight(r.textSimple, re) ?? esc(r.textUthmani);
      return `<li><button class="result" data-page="${r.pageNumber}" data-id="${r.ayahId}">
        <div class="result-top"><b>سورة ${esc(r.surahNameAr)}، الآية ${toAr(r.ayahNumber)}</b><span>صفحة ${toAr(r.pageNumber)}، الجزء ${toAr(r.juzNumber)}</span></div>
        <div class="result-text">${text}</div></button></li>`;
    }).join("");
  }
  $("results").addEventListener("click", (e) => {
    const b = e.target.closest("[data-page]");
    if (b) openPage(+b.dataset.page, +b.dataset.id);
  });

  // ---------------- SDK API lab ----------------
  const API_CALLS = [
    { name: "getSurahs", ret: "List<Surah>" },
    { name: "getJuzList", ret: "List<Juz>" },
    { name: "search", ret: "List<SearchResult>", params: [{ v: "الرحمن", wide: true }, { v: 20 }] },
    { name: "getAyahsByPage", ret: "List<Ayah>", params: [{ v: 1 }] },
    { name: "getAyahsBySurah", ret: "List<Ayah>", params: [{ v: 112 }] },
    { name: "getAyahsByJuz", ret: "List<Ayah>", params: [{ v: 30 }] },
    { name: "getAyahById", ret: "Ayah?", params: [{ v: 123 }] },
    { name: "getFirstAyahOnPage", ret: "Ayah?", params: [{ v: 15 }] },
    { name: "getMaxPage", ret: "Int" }
  ];
  function renderApiLab() {
    $("apiList").innerHTML = API_CALLS.map((c, i) => {
      const ps = (c.params || []).map((p, j) =>
        `<input data-p="${j}" class="${p.wide ? "wide" : ""}" value="${esc(String(p.v))}" ${p.wide ? 'dir="rtl"' : 'inputmode="numeric"'}>`).join(", ");
      return `<div class="api-card" data-i="${i}">
        <div class="api-row"><div class="api-sig">quran.${c.name}(${ps})</div><button class="api-run">Run</button></div>
        <pre class="api-out" hidden></pre></div>`;
    }).join("");
  }
  $("apiList").addEventListener("click", (e) => {
    if (!e.target.classList.contains("api-run")) return;
    const card = e.target.closest(".api-card"), c = API_CALLS[+card.dataset.i];
    const args = [...card.querySelectorAll("input")].map((inp, j) =>
      c.params[j].wide ? inp.value : parseInt(inp.value, 10));
    const t0 = performance.now();
    let result, err;
    try { result = api[c.name](...args); } catch (ex) { err = ex; }
    const ms = (performance.now() - t0).toFixed(1);
    const out = card.querySelector(".api-out");
    out.hidden = false;
    if (err) { out.textContent = "✗ " + err.message; return; }
    let body;
    if (Array.isArray(result)) {
      const shown = result.slice(0, 2);
      body = `${c.ret} · size = ${result.length}\n` + JSON.stringify(shown, null, 2) +
        (result.length > 2 ? `\n… +${result.length - 2} more` : "");
    } else {
      body = `${c.ret}\n` + JSON.stringify(result, null, 2);
    }
    out.innerHTML = `<span class="ok">✓ ${ms} ms</span>\n` + esc(body);
  });

  // ---------------- boot: QuranApi.initialize() ----------------
  (async function boot() {
    const bar = $("splashBar");
    await api.initialize((pct, msg) => { bar.style.width = pct * 100 + "%"; $("splashMsg").textContent = msg; });
    SURAS = api.getSurahs(); JUZ = api.getJuzList(); MAX_PAGE = api.getMaxPage();
    $("pageSlider").max = MAX_PAGE;
    applyFont(); applyMode(); renderIndex(); renderContinue(); renderApiLab();
    goToPage(currentPage);
    setTimeout(() => $("splash").classList.add("done"), 350);
  })();
})();
