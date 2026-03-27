var ConversationViewer = (function () {

  /** Scroll the conversation panel to the bottom after an AJAX update. */
  function scrollToBottom(panelSelector) {
    var el = document.querySelector(panelSelector);
    if (el) el.scrollTop = el.scrollHeight;
  }

  return { scrollToBottom: scrollToBottom };
})();

/* ── CvAgent ──────────────────────────────────────────────────────────────────
   Collapsible agent panels (Phoenix-style tracing rows).
   ─────────────────────────────────────────────────────────────────────────── */

var CvAgent = (function () {
  function toggle(headerEl) {
    var block = headerEl.closest('.cv-agent-block');
    if (block) block.classList.toggle('cv-agent-open');
  }
  return { toggle: toggle };
})();



/* ── CvMarkdown ───────────────────────────────────────────────────────────────
   Renders markdown in .cv-tab-msg-text elements and highlights code blocks.
   Runs once on DOMContentLoaded (page content is server-rendered, no AJAX lazy load).
   ─────────────────────────────────────────────────────────────────────────── */

var CvMarkdown = (function () {

  // Returns true when obj is a plain JSON object whose every top-level value is a string.
  // Used to detect tool outputs like {"validationResult": "...markdown text..."}.
  function isAllStringObject(obj) {
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) return false;
    var keys = Object.keys(obj);
    return keys.length > 0 && keys.every(function (k) { return typeof obj[k] === 'string'; });
  }

  // Renders a flat {key: stringValue} object as markdown text.
  // Single key  → renders the value directly.
  // Multiple keys → renders each as **key**\n\nvalue, separated by hr.
  function flatStringToMarkdown(obj) {
    var keys = Object.keys(obj);
    if (keys.length === 1) return obj[keys[0]];
    return keys.map(function (k) { return '**' + k + '**\n\n' + obj[k]; }).join('\n\n---\n\n');
  }

  // Walk a parsed JSON value recursively; expand any string that is itself valid JSON.
  function deepParse(value) {
    if (typeof value === 'string') {
      try {
        var inner = JSON.parse(value);
        if (inner !== null && typeof inner === 'object') return deepParse(inner);
      } catch (e) { /* not JSON, keep as-is */ }
      return value;
    }
    if (Array.isArray(value)) {
      return value.map(deepParse);
    }
    if (value !== null && typeof value === 'object') {
      var out = {};
      for (var k in value) {
        if (Object.prototype.hasOwnProperty.call(value, k)) out[k] = deepParse(value[k]);
      }
      return out;
    }
    return value;
  }

  function renderAll() {
    if (typeof marked === 'undefined') return;

    marked.setOptions({ breaks: true, gfm: true });

    document.querySelectorAll('.cv-tab-msg-text:not([data-md])').forEach(function (el) {
      var raw = el.textContent;
      var trimmed = raw.trim();
      var source = raw;

      // Auto-format bare JSON: flat {key:string} objects render as markdown text;
      // everything else is wrapped in a fenced JSON code block.
      if ((trimmed.startsWith('{') || trimmed.startsWith('[')) && !trimmed.startsWith('```')) {
        try {
          var parsed = JSON.parse(trimmed);
          var deep = deepParse(parsed);
          if (isAllStringObject(deep)) {
            source = flatStringToMarkdown(deep);
          } else {
            source = '```json\n' + JSON.stringify(deep, null, 2) + '\n```';
          }
        } catch (e) { /* not valid JSON, render as-is */ }
      }

      el.innerHTML = marked.parse(source);
      el.setAttribute('data-md', '1');
    });

    if (typeof hljs !== 'undefined') {
      document.querySelectorAll('.cv-tab-msg-text pre code').forEach(function (block) {
        hljs.highlightElement(block);
      });
    }
  }

  document.addEventListener('DOMContentLoaded', renderAll);

  return { renderAll: renderAll };
})();


/* ── CvTools ──────────────────────────────────────────────────────────────────
   Left-nav tab switcher for the Tools tab panel.
   Activates the nav item and shows the matching content panel by index.
   ─────────────────────────────────────────────────────────────────────────── */

var CvTools = (function () {

  function select(navItem) {
    var tabview = navItem.closest('.cv-tools-tabview');
    if (!tabview) return;

    var navItems = tabview.querySelectorAll('.cv-tools-nav-item');
    var panels   = tabview.querySelectorAll('.cv-tool-panel');
    var idx      = Array.prototype.indexOf.call(navItems, navItem);

    navItems.forEach(function (el) { el.classList.remove('cv-tools-nav-item--active'); });
    panels.forEach(function (el)   { el.classList.remove('cv-tool-panel--active'); });

    navItem.classList.add('cv-tools-nav-item--active');
    if (panels[idx]) panels[idx].classList.add('cv-tool-panel--active');
  }

  function initAll() {
    document.querySelectorAll('.cv-tools-tabview').forEach(function (tv) {
      var first = tv.querySelector('.cv-tools-nav-item');
      if (first) select(first);
    });
  }

  document.addEventListener('DOMContentLoaded', initAll);

  return { select: select };
})();


/* ── CvArgs ───────────────────────────────────────────────────────────────────
   Horizontal tab switcher for the Arguments tab strip inside each tool panel.
   ─────────────────────────────────────────────────────────────────────────── */

/* ── CvRaw ────────────────────────────────────────────────────────────────────
   Pretty / Raw toggle for agent tabviews.
   Adds/removes 'cv-raw-mode' on the .cv-agent-tabview wrapper;
   CSS then shows .cv-tab-raw-text and hides .cv-tab-msg-text (and vice versa).
   ─────────────────────────────────────────────────────────────────────────── */

var CvRaw = (function () {

  // input is the underlying checkbox element (passed as 'this' from p:toggleSwitch onchange).
  // PrimeFaces manages its own visual state; we just sync the tabview class.
  function toggle(input) {
    var isPretty = input.checked;
    var tabview  = input.closest('.cv-agent-tabview');
    if (tabview) tabview.classList.toggle('cv-raw-mode', !isPretty);
  }

  return { toggle: toggle };
})();


var CvArgs = (function () {

  function select(tab) {
    var tabview = tab.closest('.cv-args-tabview');
    if (!tabview) return;
    var tabs   = tabview.querySelectorAll('.cv-args-tab');
    var panels = tabview.querySelectorAll('.cv-args-panel');
    var idx    = Array.prototype.indexOf.call(tabs, tab);
    tabs.forEach(function (el)   { el.classList.remove('cv-args-tab--active'); });
    panels.forEach(function (el) { el.classList.remove('cv-args-panel--active'); });
    tab.classList.add('cv-args-tab--active');
    if (panels[idx]) panels[idx].classList.add('cv-args-panel--active');
  }

  function initAll() {
    document.querySelectorAll('.cv-args-tabview').forEach(function (tv) {
      var first = tv.querySelector('.cv-args-tab');
      if (first) select(first);
    });
  }

  document.addEventListener('DOMContentLoaded', initAll);

  return { select: select };
})();
