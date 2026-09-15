// تطبيق العميل: يستخدم QuranApi فقط (مثل التطبيق التجريبي في quranapp-main/app/sdktest)
(function () {
  const api = window.QuranApi;
  const $ = (id) => document.getElementById(id);
  const toAr = (n) => String(n).replace(/\d/g, (d) => "٠١٢٣٤٥٦٧٨٩"[d]);
  const esc = (s) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const BISMILLAH = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ";
  const IMG_W = 1024, IMG_H = 1656;
  const DAY = 86400000;

  const store = {
    get(k, d) { try { const v = localStorage.getItem("qd." + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
    set(k, v) { try { localStorage.setItem("qd." + k, JSON.stringify(v)); } catch { /* ignore */ } }
  };

  let SURAS = [], JUZ = [], MAX_PAGE = 604;
  let currentPage = store.get("page", 1);
  let fontSize = store.get("font", 24);
  let mode = store.get("mode", "mushaf");        // "mushaf" صور مصحف المدينة | "text" نص عثماني
  let bookmarks = store.get("bookmarks", []);
  let wirds = store.get("wirds", []);            // [{id, from, to, at}] — from/to معرّفات آيات
  let wirdStart = store.get("wirdStart", null);  // بداية ورد قيد التسجيل
  let currentTab = "index";
  const marks = { selected: null, pressed: null, range: null };

  const surahName = (n) => SURAS[n - 1].nameAr;
  const ayahLabel = (a) => `${surahName(a.surahNumber)} ${toAr(a.ayahNumber)}`;
  const isBookmarked = (id) => bookmarks.some((b) => b.ayahId === id);
  const pageSrc = (p) => `pages/page${String(p).padStart(3, "0")}.png`;
  const device = $("device");
  const ICON = {
    chev: '<svg class="chev" viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg>',
    trash: '<svg viewBox="0 0 24 24"><path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"/></svg>',
    book: '<svg viewBox="0 0 24 24"><path d="M12 6.5C10 5 7 4.5 3.5 5v13.5c3.5-.5 6.5 0 8.5 1.5 2-1.5 5-2 8.5-1.5V5C17 4.5 14 5 12 6.5z"/></svg>',
    play: '<svg viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg>'
  };

  // ---------------- toast ----------------
  let toastTimer;
  function toast(msg) {
    const t = $("toast");
    t.textContent = msg; t.classList.add("show");
    clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove("show"), 2200);
  }

  // ---------------- tabs ----------------
  function showTab(tab) {
    currentTab = tab;
    document.querySelectorAll(".screen").forEach((s) => (s.hidden = s.id !== "screen-" + tab));
    document.querySelectorAll("[data-tab]").forEach((b) => b.classList.toggle("active", b.dataset.tab === tab));
    closeSheets(); setImmersive(false);
    if (tab === "index") renderContinue();
    if (tab === "bookmarks") renderBookmarks();
    if (tab === "wird") renderWirds();
    if (tab === "reader") layoutPager();
    if (tab === "search") setTimeout(() => $("searchInput").focus(), 60);
  }
  document.querySelectorAll("[data-tab]").forEach((b) => b.addEventListener("click", () => showTab(b.dataset.tab)));
  $("openSdk").addEventListener("click", () => showTab("api"));
  $("closeSdk").addEventListener("click", () => showTab("index"));

  function setImmersive(on) {
    device.classList.toggle("immersive", on);
    if (currentTab === "reader") requestAnimationFrame(() => layoutPager(false));
  }

  // ---------------- index ----------------
  function renderIndex() {
    $("suraList").innerHTML = SURAS.map((s) => `
      <li><button class="row" data-page="${s.startPage}">
        <span class="num">${toAr(s.number)}</span>
        <span class="row-main"><span class="row-title">سورة ${esc(s.nameAr)}</span>
          <span class="row-sub">${toAr(s.ayahCount)} آية، <bdi>${esc(s.nameEn)}</bdi></span></span>
        <span class="row-end"><b>${toAr(s.startPage)}</b>${ICON.chev}</span>
      </button></li>`).join("");
    $("juzList").innerHTML = JUZ.map((j) => `
      <li><button class="row" data-page="${j.startPage}">
        <span class="num">${toAr(j.number)}</span>
        <span class="row-main"><span class="row-title">${esc(j.nameAr)}</span>
          <span class="row-sub">${esc(api.juzNameAr(j.number))}</span></span>
        <span class="row-end"><b>${toAr(j.startPage)}</b>${ICON.chev}</span>
      </button></li>`).join("");
    for (const ul of [$("suraList"), $("juzList")]) {
      ul.addEventListener("click", (e) => {
        const b = e.target.closest("[data-page]");
        if (b) openPage(+b.dataset.page);
      });
    }
    document.querySelectorAll("[data-seg]").forEach((b) => b.addEventListener("click", () => {
      document.querySelectorAll("[data-seg]").forEach((x) => x.classList.toggle("active", x === b));
      $("suraList").hidden = b.dataset.seg !== "suras";
      $("juzList").hidden = b.dataset.seg !== "juz";
    }));
    $("continueCard").addEventListener("click", () => openPage(currentPage));
  }

  function renderContinue() {
    const a = api.getFirstAyahOnPage(currentPage);
    if (!a) return;
    $("continueTitle").textContent = "سورة " + surahName(a.surahNumber);
    const pct = Math.round((currentPage / MAX_PAGE) * 100);
    $("continueMeta").textContent = `صفحة ${toAr(currentPage)} من ${toAr(MAX_PAGE)}، الجزء ${toAr(a.juzNumber)}`;
    $("continueBar").style.width = pct + "%";
  }

  // ---------------- reader: pager (QuranPageView) ----------------
  // كل صفحة "slide" داخل حاوية تمرير أفقي RTL مع scroll-snap: الصفحة تتبع الإصبع وتثبت صفحة صفحة
  const pager = $("pager");
  const slides = [];
  const rendered = new Set();

  function buildPager() {
    const frag = document.createDocumentFragment();
    for (let p = 1; p <= MAX_PAGE; p++) {
      const s = document.createElement("div");
      s.className = "slide"; s.dataset.p = p;
      frag.appendChild(s); slides.push(s);
    }
    pager.appendChild(frag);
  }

  function buildTextPage(p) {
    let html = "";
    for (const a of api.getAyahsByPage(p)) {
      if (a.ayahNumber === 1) {
        html += `<span class="sura-head">سورة ${esc(surahName(a.surahNumber))}</span>`;
        if (a.surahNumber !== 1 && a.surahNumber !== 9) html += `<span class="bismillah">${BISMILLAH}</span>`;
      }
      html += `<span class="ayah" data-id="${a.id}">${esc(a.textUthmani)} <span class="marker">﴿${toAr(a.ayahNumber)}﴾</span></span> `;
    }
    return html;
  }

  function renderSlide(p) {
    const s = slides[p - 1];
    s.innerHTML = mode === "text"
      ? `<article class="text-page"><div class="text-inner">${buildTextPage(p)}</div></article>`
      : `<div class="sheet"><img src="${pageSrc(p)}" alt="صفحة ${toAr(p)}" draggable="false" decoding="async"><svg viewBox="0 0 1024 1656" preserveAspectRatio="none"></svg></div>`;
    rendered.add(p);
    renderMarks(p);
  }

  // نرسم الصفحة الحالية وصفحتين قبلها وبعدها فقط، ونفرّغ البعيدة
  function ensureWindow() {
    for (const p of [...rendered]) if (Math.abs(p - currentPage) > 3) { slides[p - 1].innerHTML = ""; rendered.delete(p); }
    for (let p = currentPage - 2; p <= currentPage + 2; p++) if (p >= 1 && p <= MAX_PAGE && !rendered.has(p)) renderSlide(p);
  }
  function rerenderAll() {
    for (const p of [...rendered]) { slides[p - 1].innerHTML = ""; rendered.delete(p); }
    ensureWindow();
  }

  // مقاس الصفحة = أكبر مساحة داخل الهوامش مع الحفاظ على نسبة الصورة
  function layoutPager(rescroll = true) {
    const w = pager.clientWidth, h = pager.clientHeight;
    if (!w) return;
    const cs = getComputedStyle(slides[currentPage - 1]);
    const aw = w - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight);
    const ah = h - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom);
    const s = Math.min(aw / IMG_W, ah / IMG_H);
    pager.style.setProperty("--sheet-w", IMG_W * s + "px");
    pager.style.setProperty("--sheet-h", IMG_H * s + "px");
    ensureWindow();
    if (rescroll) scrollToPage(currentPage, false);
  }
  window.addEventListener("resize", () => currentTab === "reader" && layoutPager());

  function scrollToPage(p, smooth) {
    const s = slides[p - 1];
    if (s) s.scrollIntoView({ behavior: smooth ? "smooth" : "auto", inline: "center", block: "nearest" });
  }

  function updateHeader(p) {
    const first = api.getFirstAyahOnPage(p);
    $("rSurah").textContent = first ? surahName(first.surahNumber) : "";
    $("rJuz").textContent = first ? api.juzNameAr(first.juzNumber) : "";
    $("rPage").textContent = toAr(p);
    $("pageSlider").value = p;
    $("sliderVal").textContent = toAr(p);
  }

  function setCurrent(p) {
    currentPage = p; store.set("page", p);
    updateHeader(p); ensureWindow();
  }

  // تتبّع الصفحة الظاهرة أثناء السحب
  let scrollRaf = 0, settleTimer = 0, isScrolling = false, lastSL = 0;
  pager.addEventListener("scroll", () => {
    if (Math.abs(pager.scrollLeft - lastSL) < 2) return; // حدث تمرير بدون حركة فعلية (إعادة تخطيط)
    lastSL = pager.scrollLeft;
    isScrolling = true;
    clearTimeout(settleTimer);
    settleTimer = setTimeout(() => { isScrolling = false; pager.classList.remove("dragging"); }, 140);
    if (scrollRaf) return;
    scrollRaf = requestAnimationFrame(() => {
      scrollRaf = 0;
      const p = Math.min(MAX_PAGE, Math.max(1, Math.round(Math.abs(pager.scrollLeft) / pager.clientWidth) + 1));
      if (p !== currentPage) setCurrent(p);
    });
  }, { passive: true });

  function goToPage(p, smooth) {
    p = Math.max(1, Math.min(MAX_PAGE, p));
    if (!smooth || Math.abs(p - currentPage) > 1) setCurrent(p);
    scrollToPage(p, smooth);
  }

  function openPage(p, opts = {}) {
    marks.selected = opts.select || null;
    marks.range = opts.range || null;
    setCurrent(Math.max(1, Math.min(MAX_PAGE, p)));
    showTab("reader");
    renderMarks();
  }

  const next = () => currentPage < MAX_PAGE && goToPage(currentPage + 1, true);
  const prev = () => currentPage > 1 && goToPage(currentPage - 1, true);
  $("nextBtn").addEventListener("click", next);
  $("prevBtn").addEventListener("click", prev);
  document.addEventListener("keydown", (e) => {
    if (currentTab !== "reader" || e.target.tagName === "INPUT") return;
    if (e.key === "ArrowLeft") next();
    if (e.key === "ArrowRight") prev();
    if (e.key === "Escape") closeSheets();
  });

  // ---- تظليل الآيات ----
  function markClass(id) {
    if (id === marks.pressed) return "m-press";
    if (id === marks.selected) return "m-sel";
    if (id === wirdStart) return "m-start";
    if (marks.range && id >= marks.range.from && id <= marks.range.to) return "m-range";
    if (isBookmarked(id)) return "m-bm";
    return "";
  }
  function renderMarks(p) {
    for (const pg of p ? [p] : [...rendered]) {
      if (!rendered.has(pg)) continue;
      const s = slides[pg - 1];
      if (mode === "text") {
        s.querySelectorAll(".ayah").forEach((el) => { el.className = "ayah " + markClass(+el.dataset.id); });
        continue;
      }
      const svg = s.querySelector("svg");
      if (!svg) continue;
      svg.innerHTML = (window.PAGE_RECTS[pg] || []).map(([id, x1, y1, x2, y2]) => {
        const c = markClass(id);
        return c ? `<rect class="${c}" x="${x1 - 6}" y="${y1 - 4}" width="${x2 - x1 + 12}" height="${y2 - y1 + 8}" rx="12"/>` : "";
      }).join("");
    }
  }

  // أي آية تحت الإصبع؟ وضع الصور: نحوّل الإحداثيات لمقاس 1024 ونقارن بمستطيلات ayahinfo_1024.db
  function ayahAt(e, slide) {
    if (mode === "text") {
      const el = e.target.closest && e.target.closest(".ayah");
      return el ? +el.dataset.id : null;
    }
    const sh = slide.querySelector(".sheet");
    if (!sh) return null;
    const r = sh.getBoundingClientRect();
    if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) return null;
    const x = (e.clientX - r.left) * IMG_W / r.width, y = (e.clientY - r.top) * IMG_H / r.height;
    let best = null, bestD = 18;
    for (const [id, x1, y1, x2, y2] of window.PAGE_RECTS[+slide.dataset.p] || []) {
      if (y < y1 - 4 || y > y2 + 4) continue;
      const d = x < x1 ? x1 - x : x > x2 ? x - x2 : 0;
      if (d < bestD) { bestD = d; best = id; }
    }
    return best;
  }

  // ---- اللمس: نقرة = قائمة الآية (onAyahTapped)، ضغط مطوّل (onAyahLongPressed)، نقرة على فراغ = وضع القراءة الكاملة ----
  let press = null;
  function endPress() {
    if (!press) return;
    clearTimeout(press.hlTimer); clearTimeout(press.longTimer);
    if (marks.pressed) { marks.pressed = null; renderMarks(press.p); }
  }
  pager.addEventListener("pointerdown", (e) => {
    if (e.button === 2) return;
    const slide = e.target.closest(".slide");
    if (!slide) return;
    endPress();
    const id = ayahAt(e, slide);
    press = { x: e.clientX, y: e.clientY, p: +slide.dataset.p, id, moved: false, long: false,
      busy: isScrolling, mouse: e.pointerType === "mouse", sl: pager.scrollLeft, dragging: false };
    if (id && !press.busy) {
      press.hlTimer = setTimeout(() => { marks.pressed = id; renderMarks(press.p); }, 90);
      press.longTimer = setTimeout(() => {
        press.long = true; marks.pressed = null;
        onAyahLongPressed(id);
      }, 480);
    }
  });
  pager.addEventListener("pointermove", (e) => {
    if (!press) return;
    const dx = e.clientX - press.x, dy = e.clientY - press.y;
    if (!press.moved && Math.hypot(dx, dy) > 8) {
      press.moved = true; endPress();
      // سحب بالماوس على الكمبيوتر (اللمس يستخدم التمرير الأصلي)
      if (press.mouse && Math.abs(dx) > Math.abs(dy)) {
        press.dragging = true; pager.classList.add("dragging");
        try { pager.setPointerCapture(e.pointerId); } catch { /* ignore */ }
      }
    }
    if (press.dragging) pager.scrollLeft = press.sl - dx;
  });
  pager.addEventListener("pointerup", (e) => {
    if (!press) return;
    const pr = press; endPress(); press = null;
    if (pr.dragging) {
      const w = pager.clientWidth, start = Math.round(Math.abs(pr.sl) / w);
      const delta = Math.abs(pager.scrollLeft) - Math.abs(pr.sl);
      const target = delta > w * 0.12 ? start + 1 : delta < -w * 0.12 ? start - 1 : start;
      scrollToPage(Math.max(1, Math.min(MAX_PAGE, target + 1)), true);
      return;
    }
    if (pr.moved || pr.long || pr.busy) return;
    if (pr.id) onAyahTapped(pr.id);
    else setImmersive(!device.classList.contains("immersive"));
  });
  pager.addEventListener("pointercancel", () => { endPress(); press = null; });
  pager.addEventListener("contextmenu", (e) => {
    e.preventDefault();
    const slide = e.target.closest(".slide");
    const id = slide && ayahAt(e, slide);
    if (id) { endPress(); press = null; onAyahLongPressed(id); }
  });

  function onAyahTapped(id) { openAyahSheet(id); }
  function onAyahLongPressed(id) { if (navigator.vibrate) navigator.vibrate(8); openAyahSheet(id); }

  // ---------------- sheets ----------------
  let openSheetId = null, sheetOpenedAt = 0;
  function openSheet(id) {
    closeSheets(true);
    openSheetId = id; sheetOpenedAt = performance.now();
    $(id).classList.add("show"); $("backdrop").classList.add("show");
  }
  function closeSheets(silent) {
    if (!openSheetId) return;
    $(openSheetId).classList.remove("show"); $("backdrop").classList.remove("show");
    const was = openSheetId; openSheetId = null;
    if (was === "ayahSheet" && !silent) {
      marks.selected = null;
      if (marks.range && marks.range.preview) marks.range = null;
      renderMarks();
    }
  }
  // نتجاهل "النقرة الشبح" اللي يرسلها المتصفح بعد اللمس مباشرة على الخلفية الجديدة
  $("backdrop").addEventListener("click", () => { if (performance.now() - sheetOpenedAt > 450) closeSheets(); });
  document.querySelectorAll("[data-close]").forEach((b) => b.addEventListener("click", () => closeSheets()));

  // سحب الورقة لتحت لإغلاقها
  document.querySelectorAll(".bsheet").forEach((sh) => {
    let y0 = null, dy = 0;
    sh.addEventListener("touchstart", (e) => { if (sh.scrollTop <= 0) { y0 = e.touches[0].clientY; dy = 0; } }, { passive: true });
    sh.addEventListener("touchmove", (e) => {
      if (y0 == null) return;
      dy = Math.max(0, e.touches[0].clientY - y0);
      sh.style.transition = "none"; sh.style.transform = `translateY(${dy}px)`;
    }, { passive: true });
    sh.addEventListener("touchend", () => {
      if (y0 == null) return;
      sh.style.transition = ""; sh.style.transform = "";
      if (dy > 90) closeSheets();
      y0 = null;
    });
  });

  // ---- ورقة الآية + تسجيل الورد ----
  let sheetAyah = null;
  function openAyahSheet(id) {
    const a = api.getAyahById(id);
    sheetAyah = a;
    marks.selected = id;
    const pending = wirdStart && wirdStart !== id;
    if (pending) marks.range = { from: Math.min(wirdStart, id), to: Math.max(wirdStart, id), preview: true };
    renderMarks();

    $("asTitle").textContent = "سورة " + surahName(a.surahNumber);
    $("asSub").textContent = `الآية ${toAr(a.ayahNumber)} · صفحة ${toAr(a.pageNumber)} · الجزء ${toAr(a.juzNumber)}`;
    $("asText").textContent = `${a.textUthmani} ﴿${toAr(a.ayahNumber)}﴾`;
    $("asBmLabel").textContent = isBookmarked(id) ? "في المفضلة" : "مفضلة";
    document.querySelector('[data-act="bookmark"]').classList.toggle("on", isBookmarked(id));

    let primary;
    if (!wirdStart) {
      primary = `<button class="primary-btn" data-act="wird-start">تسجيل ورد من هنا<small>ثم اضغط على آية النهاية</small></button>`;
    } else if (wirdStart === id) {
      primary = `<button class="primary-btn neutral" data-act="wird-cancel">إلغاء بداية الورد</button>`;
    } else {
      const from = api.getAyahById(marks.range.from), to = api.getAyahById(marks.range.to);
      const count = marks.range.to - marks.range.from + 1;
      primary = `<div class="btn-row">
        <button class="primary-btn" data-act="wird-end" style="flex:2.4">تسجيل الورد إلى هنا
          <small>${esc(ayahLabel(from))} ← ${esc(ayahLabel(to))} · ${toAr(count)} آية</small></button>
        <button class="primary-btn neutral" data-act="wird-cancel">إلغاء</button></div>`;
    }
    $("asPrimary").innerHTML = primary;
    openSheet("ayahSheet");
  }

  $("ayahSheet").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-act]");
    if (!b || !sheetAyah) return;
    const a = sheetAyah, act = b.dataset.act;
    if (act === "wird-start") {
      wirdStart = a.id; store.set("wirdStart", wirdStart);
      marks.range = null; closeSheets(); updateWirdBanner();
      toast("حُددت بداية الورد — اضغط على آية النهاية");
    } else if (act === "wird-cancel") {
      wirdStart = null; store.set("wirdStart", null);
      marks.range = null; closeSheets(); updateWirdBanner();
    } else if (act === "wird-end") {
      const { from, to } = marks.range;
      wirds.unshift({ id: Date.now(), from, to, at: Date.now() });
      store.set("wirds", wirds);
      wirdStart = null; store.set("wirdStart", null);
      marks.range = { from, to };
      closeSheets(); updateWirdBanner();
      toast(`سُجّل الورد ✓ ${toAr(to - from + 1)} آية`);
      setTimeout(() => { if (marks.range && marks.range.from === from && !marks.range.preview) { marks.range = null; renderMarks(); } }, 2600);
    } else if (act === "bookmark") {
      toggleBookmark(a); closeSheets();
    } else if (act === "copy" || act === "share") {
      const txt = `${a.textUthmani} ﴿${toAr(a.ayahNumber)}﴾ [سورة ${surahName(a.surahNumber)}]`;
      if (act === "share" && navigator.share) {
        try { await navigator.share({ text: txt }); } catch { /* cancelled */ }
      } else {
        try { await navigator.clipboard.writeText(txt); toast("تم نسخ الآية"); } catch { toast("تعذّر النسخ"); }
      }
      closeSheets();
    }
  });

  function updateWirdBanner() {
    const bn = $("wirdBanner");
    if (!wirdStart) { bn.hidden = true; renderMarks(); return; }
    const a = api.getAyahById(wirdStart);
    $("wirdBannerText").textContent = `بداية الورد: ${ayahLabel(a)} — اختر آية النهاية`;
    bn.hidden = false;
    renderMarks();
  }
  $("wirdBannerCancel").addEventListener("click", () => {
    wirdStart = null; store.set("wirdStart", null); updateWirdBanner(); toast("أُلغي تسجيل الورد");
  });

  // ---- ورقة العرض والتنقل ----
  function openViewSheet() {
    document.querySelectorAll("#modeSeg [data-mode]").forEach((b) => b.classList.toggle("active", b.dataset.mode === mode));
    $("fontGroup").hidden = mode !== "text";
    $("fontVal").textContent = toAr(fontSize);
    openSheet("viewSheet");
  }
  $("viewBtn").addEventListener("click", openViewSheet);
  $("readerInfo").addEventListener("click", openViewSheet);
  $("rPage").addEventListener("click", openViewSheet);
  $("modeSeg").addEventListener("click", (e) => {
    const b = e.target.closest("[data-mode]");
    if (!b || b.dataset.mode === mode) return;
    mode = b.dataset.mode; store.set("mode", mode);
    document.querySelectorAll("#modeSeg [data-mode]").forEach((x) => x.classList.toggle("active", x === b));
    $("fontGroup").hidden = mode !== "text";
    rerenderAll(); layoutPager();
  });
  $("pageSlider").addEventListener("input", (e) => { $("sliderVal").textContent = toAr(e.target.value); });
  $("pageSlider").addEventListener("change", (e) => goToPage(+e.target.value, false));
  function applyFont() {
    document.documentElement.style.setProperty("--ayah-size", fontSize + "px");
    store.set("font", fontSize); $("fontVal").textContent = toAr(fontSize);
  }
  $("fontUp").addEventListener("click", () => { fontSize = Math.min(40, fontSize + 2); applyFont(); });
  $("fontDown").addEventListener("click", () => { fontSize = Math.max(16, fontSize - 2); applyFont(); });

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
    const box = $("bookmarkContent");
    if (!bookmarks.length) {
      box.innerHTML = `<div class="card empty"><span class="big">🔖</span>ما فيه آيات محفوظة.<br>اضغط على أي آية في المصحف واختر «مفضلة».</div>`;
      return;
    }
    box.innerHTML = `<ul class="card list">${bookmarks.map((b) => `
      <li><div class="row" data-open="${b.ayahId}" data-page="${b.pageNumber}">
        <span class="num">${toAr(b.ayahNumber)}</span>
        <span class="row-main"><span class="row-sub">سورة ${esc(surahName(b.surahNumber))}، صفحة ${toAr(b.pageNumber)}</span>
          <span class="row-title" style="font-family:Hafs,serif;font-size:18px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">${esc(b.text)}</span></span>
        <button class="icon-btn" data-del="${b.ayahId}" aria-label="حذف">${ICON.trash}</button>
      </div></li>`).join("")}</ul>`;
  }
  $("bookmarkContent").addEventListener("click", (e) => {
    const del = e.target.closest("[data-del]");
    if (del) {
      bookmarks = bookmarks.filter((b) => b.ayahId !== +del.dataset.del);
      store.set("bookmarks", bookmarks); renderBookmarks(); return;
    }
    const row = e.target.closest("[data-open]");
    if (row) openPage(+row.dataset.page, { select: +row.dataset.open });
  });

  // ---------------- wird ----------------
  const dayStart = (t) => { const d = new Date(t); d.setHours(0, 0, 0, 0); return d.getTime(); };
  function dayLabel(t) {
    const d0 = dayStart(Date.now()), d = dayStart(t);
    if (d === d0) return "اليوم";
    if (d === d0 - DAY) return "أمس";
    return new Intl.DateTimeFormat("ar-u-nu-arab", { weekday: "long", day: "numeric", month: "long" }).format(new Date(t));
  }
  const timeLabel = (t) => new Intl.DateTimeFormat("ar-u-nu-arab", { hour: "numeric", minute: "2-digit" }).format(new Date(t));

  function wirdStats() {
    const today = dayStart(Date.now());
    const count = (w) => w.to - w.from + 1;
    const todayAyahs = wirds.filter((w) => w.at >= today).reduce((s, w) => s + count(w), 0);
    const weekAyahs = wirds.filter((w) => w.at >= today - 6 * DAY).reduce((s, w) => s + count(w), 0);
    const days = new Set(wirds.map((w) => dayStart(w.at)));
    let streak = 0, d = days.has(today) ? today : today - DAY;
    while (days.has(d)) { streak++; d -= DAY; }
    return { todayAyahs, weekAyahs, streak };
  }

  function renderWirds() {
    const box = $("wirdContent");
    const st = wirdStats();
    let html = `<div class="stats">
      <div class="glass stat"><b>${toAr(st.todayAyahs)}</b><span>آية اليوم</span></div>
      <div class="glass stat"><b>${toAr(st.weekAyahs)}</b><span>هذا الأسبوع</span></div>
      <div class="glass stat hot"><b>${toAr(st.streak)}</b><span>أيام متتالية</span></div>
    </div>`;

    if (wirdStart) {
      const a = api.getAyahById(wirdStart);
      html += `<button class="glass cta" data-goto="${a.pageNumber}" data-sel="${a.id}"><span class="cta-icon">${ICON.book}</span>
        <span class="cta-main"><b>ورد قيد التسجيل</b><span>بدأ من ${esc(ayahLabel(a))} — اختر آية النهاية</span></span>${ICON.chev}</button>`;
    } else if (wirds.length) {
      const last = wirds.reduce((m, w) => (w.at > m.at ? w : m));
      const nx = api.getAyahById(Math.min(6236, last.to + 1));
      html += `<button class="glass cta" data-goto="${nx.pageNumber}" data-sel="${nx.id}"><span class="cta-icon">${ICON.book}</span>
        <span class="cta-main"><b>أكمل وردك</b><span>من ${esc(ayahLabel(nx))} · صفحة ${toAr(nx.pageNumber)}</span></span>${ICON.chev}</button>`;
    }

    if (!wirds.length) {
      html += `<div class="card"><div class="empty" style="padding-bottom:8px"><span class="big">📖</span>ما سجّلت أي ورد بعد</div>
        <ol class="steps">
          <li><span class="step-n">١</span><span>افتح المصحف واضغط على آية البداية، واختر «تسجيل ورد من هنا».</span></li>
          <li><span class="step-n">٢</span><span>اقرأ وقلّب الصفحات، ثم اضغط على آية النهاية.</span></li>
          <li><span class="step-n">٣</span><span>اختر «تسجيل الورد إلى هنا» — يتحدد النطاق ويُحفظ هنا.</span></li>
        </ol></div>`;
      box.innerHTML = html;
      return;
    }

    const groups = new Map();
    for (const w of [...wirds].sort((x, y) => y.at - x.at)) {
      const k = dayLabel(w.at);
      if (!groups.has(k)) groups.set(k, []);
      groups.get(k).push(w);
    }
    for (const [label, list] of groups) {
      html += `<div class="group-label">${label}</div><ul class="card list">`;
      for (const w of list) {
        const a = api.getAyahById(w.from), b = api.getAyahById(w.to);
        const pages = b.pageNumber - a.pageNumber + 1;
        html += `<li><div class="row" data-wird="${w.id}">
          <span class="wird-dot">${ICON.book}</span>
          <span class="row-main"><span class="wird-title">${esc(ayahLabel(a))}<span class="arrow">←</span>${esc(ayahLabel(b))}</span>
            <span class="row-sub">${toAr(w.to - w.from + 1)} آية · ${toAr(pages)} ${pages > 2 && pages < 11 ? "صفحات" : "صفحة"} · ${timeLabel(w.at)}</span></span>
          <button class="icon-btn" data-delwird="${w.id}" aria-label="حذف">${ICON.trash}</button>
        </div></li>`;
      }
      html += `</ul>`;
    }
    box.innerHTML = html;
  }
  $("wirdContent").addEventListener("click", (e) => {
    const del = e.target.closest("[data-delwird]");
    if (del) { wirds = wirds.filter((w) => w.id !== +del.dataset.delwird); store.set("wirds", wirds); renderWirds(); return; }
    const go = e.target.closest("[data-goto]");
    if (go) { openPage(+go.dataset.goto, { select: +go.dataset.sel }); return; }
    const row = e.target.closest("[data-wird]");
    if (row) {
      const w = wirds.find((x) => x.id === +row.dataset.wird);
      if (w) openPage(api.getAyahById(w.from).pageNumber, { range: { from: w.from, to: w.to } });
    }
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
    const box = $("results"), meta = $("searchMeta");
    if (!q.trim()) { box.innerHTML = ""; meta.textContent = ""; return; }
    const t0 = performance.now();
    const res = api.search(q, 200);
    const ms = Math.round(performance.now() - t0);
    meta.textContent = res.length ? `${toAr(res.length)}${res.length === 200 ? "+" : ""} نتيجة، ${toAr(ms)} ملّي ثانية` : "";
    if (!res.length) { box.innerHTML = `<div class="card empty"><span class="big">🔍</span>لا توجد نتائج</div>`; return; }
    const re = /^[\d٠-٩\s]+$/.test(q.trim()) ? null : highlightRegex(q);
    box.innerHTML = `<ul class="card list">${res.map((r) => {
      const text = highlight(r.textUthmani, re) ?? highlight(r.textSimple, re) ?? esc(r.textUthmani);
      return `<li><button class="result" data-page="${r.pageNumber}" data-id="${r.ayahId}">
        <div class="result-top"><b>سورة ${esc(r.surahNameAr)}، الآية ${toAr(r.ayahNumber)}</b><span>صفحة ${toAr(r.pageNumber)}</span></div>
        <div class="result-text">${text}</div></button></li>`;
    }).join("")}</ul>`;
  }
  $("results").addEventListener("click", (e) => {
    const b = e.target.closest("[data-page]");
    if (b) openPage(+b.dataset.page, { select: +b.dataset.id });
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
      return `<div class="card api-card" data-i="${i}">
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
      body = `${c.ret} · size = ${result.length}\n` + JSON.stringify(result.slice(0, 2), null, 2) +
        (result.length > 2 ? `\n… +${result.length - 2} more` : "");
    } else {
      body = `${c.ret}\n` + JSON.stringify(result, null, 2);
    }
    out.innerHTML = `<span class="ok">✓ ${ms} ms</span>\n` + esc(body);
  });

  // ---------------- boot: QuranApi.initialize() ----------------
  (async function boot() {
    const bar = $("splashBar");
    await api.initialize((pct) => { bar.style.width = pct * 100 + "%"; });
    SURAS = api.getSurahs(); JUZ = api.getJuzList(); MAX_PAGE = api.getMaxPage();
    $("pageSlider").max = MAX_PAGE;
    buildPager();
    applyFont(); renderIndex(); renderContinue(); renderApiLab(); updateHeader(currentPage); updateWirdBanner();
    showTab("index");
    setTimeout(() => $("splash").classList.add("done"), 300);
  })();
})();
