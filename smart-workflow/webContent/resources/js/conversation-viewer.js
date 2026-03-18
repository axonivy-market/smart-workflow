var ConversationViewer = (function () {

  /** Scroll the conversation panel to the bottom after an AJAX update. */
  function scrollToBottom(panelSelector) {
    var el = document.querySelector(panelSelector);
    if (el) el.scrollTop = el.scrollHeight;
  }

  /* ── Collapsible bubbles ──────────────────────────────────────────────── */

  /**
   * For each bubble taller than 110px, collapse it and inject
   * a "Show more / Show less" toggle button as a sibling element.
   * Uses data-cv-init to skip already-processed elements (safe to call repeatedly).
   */
  function initCollapsibleBubbles() {
    var selector = '.cv-bubble:not([data-cv-init])';
    document.querySelectorAll(selector).forEach(function (el) {
      el.setAttribute('data-cv-init', '1');

      var maxH = 110;

      if (el.scrollHeight <= maxH) return;

      el.style.maxHeight = maxH + 'px';
      el.classList.add('cv-bubble--collapsed');

      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'cv-expand-btn';
      btn.textContent = 'Show more';
      el.parentNode.insertBefore(btn, el.nextSibling);

      btn.addEventListener('click', function () {
        if (el.classList.contains('cv-bubble--collapsed')) {
          el.style.maxHeight = '';
          el.classList.remove('cv-bubble--collapsed');
          btn.textContent = 'Show less';
        } else {
          el.style.maxHeight = maxH + 'px';
          el.classList.add('cv-bubble--collapsed');
          btn.textContent = 'Show more';
        }
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    initCollapsibleBubbles();
    // Re-run after PrimeFaces AJAX updates (e.g. mode switching)
    if (typeof $ !== 'undefined') {
      $(document).on('pfAjaxComplete', function () {
        initCollapsibleBubbles();
      });
    }
  });

  return {
    scrollToBottom: scrollToBottom,
    initCollapsibleBubbles: initCollapsibleBubbles
  };
})();

/* ── CvSidebar ────────────────────────────────────────────────────────────────
   Manages the collapsible right sidebar panel (Process Viewer / Case Details).
   ─────────────────────────────────────────────────────────────────────────── */

var CvSidebar = (function () {
  var activePanel = null;

  function toggle(panelId) {
    var panel = document.getElementById('cvSidebarPanel');
    if (!panel) { return; }

    if (activePanel === panelId) {
      // close
      panel.classList.remove('cv-sidebar-open');
      activePanel = null;
    } else {
      // switch content or open
      document.querySelectorAll('.cv-sidebar-content').forEach(function (el) {
        el.style.display = 'none';
      });
      var target = document.getElementById('cv-panel-' + panelId);
      if (target) { target.style.display = ''; }
      panel.classList.add('cv-sidebar-open');
      activePanel = panelId;
    }

    // sync menu item active state
    document.querySelectorAll('.cv-sidebar-item').forEach(function (li) {
      var panelId = li.classList.contains('cv-sidebar-item--process') ? 'process' : 'casedetails';
      li.classList.toggle('cv-active', panelId === activePanel);
    });
  }

  return { toggle: toggle };
})();
