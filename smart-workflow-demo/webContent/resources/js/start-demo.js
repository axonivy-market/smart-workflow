/**
 * Start Demo — 3-slide landing carousel.
 * Reuses fd- CSS classes from finish-demo.css for panel visibility and
 * animation (fd-step-panel, fd-active, fd-go-back, fd-dot, fd-dot-active).
 * Button IDs are sd-prev / sd-next to avoid collision with FinishDemo.
 */
(function () {
  'use strict';

  var TOTAL_SLIDES = 4;
  var currentSlide = 0;
  var wrapper = null;

  /* ── Inner navigator state (Slide 1 process panels) ────────────── */
  var INNER_TOTAL = 7;
  var currentInner = 0;
  var innerWrapper = null;

  /* ── Slide navigation ──────────────────────────────────────── */
  function goToSlide(index, isBack) {
    if (index < 0 || index >= TOTAL_SLIDES) return;

    // Hide current active panel
    var prev = wrapper.querySelector('.fd-step-panel.fd-active');
    if (prev) prev.classList.remove('fd-active');

    // Direction class drives CSS animation (fdSlideInRight / fdSlideInLeft)
    wrapper.classList.toggle('fd-go-back', isBack);

    // Show new panel
    var next = wrapper.querySelector('.fd-step-panel[data-step="' + index + '"]');
    if (next) next.classList.add('fd-active');

    currentSlide = index;
    syncUI();
  }

  function syncUI() {
    var i = currentSlide;

    // Dots
    document.querySelectorAll('.fd-dot').forEach(function (dot, idx) {
      dot.classList.toggle('fd-dot-active', idx === i);
    });

    // Prev/next buttons
    var btnPrev = document.getElementById('sd-prev');
    var btnNext = document.getElementById('sd-next');
    if (btnPrev) btnPrev.disabled = (i === 0);
    if (btnNext) btnNext.disabled = (i === TOTAL_SLIDES - 1);
  }

  /* ── Inner navigator ───────────────────────────────────────── */
  function goToInner(index) {
    if (index < 0 || index >= INNER_TOTAL || !innerWrapper) return;
    var prev = innerWrapper.querySelector('.sd-inner-panel.sd-inner-active');
    if (prev) prev.classList.remove('sd-inner-active');
    var next = innerWrapper.querySelector('.sd-inner-panel[data-panel="' + index + '"]');
    if (next) next.classList.add('sd-inner-active');
    currentInner = index;
    syncInnerUI();
  }

  function syncInnerUI() {
    var i = currentInner;
    document.querySelectorAll('.sd-inner-dot').forEach(function (dot, idx) {
      dot.classList.toggle('sd-inner-dot-active', idx === i);
    });
    document.querySelectorAll('.fd-flow .fd-flow-step[data-panel]').forEach(function (step) {
      step.classList.toggle('sd-inner-flow-active', parseInt(step.getAttribute('data-panel'), 10) === i);
    });
  }

  /* ── Public API (called from inline onclick) ───────────────── */
  window.sdGoToSlide = function (index) {
    goToSlide(index, index < currentSlide);
  };

  window.sdPrevSlide = function () {
    if (currentSlide > 0) goToSlide(currentSlide - 1, true);
  };

  window.sdNextSlide = function () {
    if (currentSlide < TOTAL_SLIDES - 1) goToSlide(currentSlide + 1, false);
  };

  window.sdInnerGo = function (index) { goToInner(index); };

  /* ── Bootstrap ─────────────────────────────────────────────── */
  function init() {
    wrapper = document.querySelector('.fd-steps-wrapper');
    if (!wrapper) return;

    // Activate first slide
    var first = wrapper.querySelector('.fd-step-panel[data-step="0"]');
    if (first) first.classList.add('fd-active');

    // Activate first inner panel (Slide 1 process steps)
    innerWrapper = document.querySelector('.sd-inner-wrapper');
    if (innerWrapper) {
      var firstInner = innerWrapper.querySelector('.sd-inner-panel[data-panel="0"]');
      if (firstInner) firstInner.classList.add('sd-inner-active');
      syncInnerUI();
    }

    // Disable Start Demo until data generation completes,
    // unless data is already generated (server signals via hidden element)
    var alreadyGenEl = document.getElementById('gen-form:data-already-generated');
    if (!alreadyGenEl) {
      var proceed = document.getElementById('form:proceed');
      if (proceed) {
        proceed.disabled = true;
        proceed.classList.add('ui-state-disabled');
      }
    }

    syncUI();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

/**
 * sdToggleDownloads — collapse/expand the "Before You Start" file list.
 * Passes the clicked <a> element so we can rotate its chevron.
 */
window.sdToggleDownloads = function (link) {
  var body = document.querySelector('.sd-download-body');
  if (!body) return;
  var open = body.classList.toggle('sd-download-body--open');
  var chevron = link ? link.querySelector('.sd-details-chevron') : null;
  if (chevron) chevron.style.transform = open ? 'rotate(180deg)' : '';
  var label = link ? link.querySelector('span') : null;
  if (label) {
    var hideEl   = document.getElementById('gen-form:file-details-hide');
    var toggleEl = document.getElementById('gen-form:file-details-toggle');
    label.textContent = open
      ? (hideEl   && hideEl.textContent   ? hideEl.textContent.trim()   : 'Hide details')
      : (toggleEl && toggleEl.textContent ? toggleEl.textContent.trim() : 'File details');
  }
};

/**
 * sdGen — Data generation timeline for slide 2 (Are you ready?).
 * Follows the same sequential-step pattern as soValidation in supplier-validation.js.
 */
var sdGen = (function () {
  'use strict';

  var _running = false;
  var TOTAL_STEPS = 3;

  function _getStepSubtitle(n) {
    var el = document.getElementById('gen-form:gen' + n + '-running-subtitle');
    if (el && el.textContent) { return el.textContent.trim(); }
    var fallback = document.getElementById('gen-form:gen-processing-subtitle');
    return (fallback && fallback.textContent) ? fallback.textContent.trim() : 'Processing\u2026';
  }

  /* ── DOM helpers ────────────────────────────────────────────────── */

  function _getGenBtn() {
    return document.getElementById('gen-form:sdGenBtn');
  }

  function _getTimeline() {
    return document.querySelector('.js-sd-gen-timeline');
  }

  /* ── Step visual state (called before AJAX fires) ───────────────── */

  function _setStepRunning(n) {
    var tl = document.querySelector('.js-sd-tl');
    if (!tl) { return; }
    var items = tl.querySelectorAll('.so-checklist-item');
    var item  = items[n - 1];
    if (!item) { return; }

    item.className = 'so-checklist-item running so-tl-item';

    var bubble = item.querySelector('.so-tl-bubble');
    if (bubble) { bubble.className = 'so-tl-bubble so-tl-bubble-running'; }

    var icon = item.querySelector('.so-tl-bubble i');
    if (icon) { icon.className = 'ti ti-loader so-spin'; }

    // Animated subtitle
    var card = item.querySelector('.so-tl-card');
    var sub  = item.querySelector('.so-checklist-subtitle');
    if (!sub && card) {
      sub = document.createElement('div');
      sub.className = 'so-checklist-subtitle';
      card.appendChild(sub);
    }
    if (sub) { sub.textContent = _getStepSubtitle(n); }
  }

  /* ── Step invocation ────────────────────────────────────────────── */

  function _invokeStep(n) {
    _setStepRunning(n);
    var fn = window['sdGenStep' + n];
    if (typeof fn === 'function') { fn(); }
  }

  /* ── Public API ─────────────────────────────────────────────────── */

  /**
   * Entry point: called by the "Generate Data" button onclick.
   */
  function start() {
    if (_running) { return; }
    _running = true;

    // Reveal the timeline panel immediately (remove server-rendered hidden class)
    var tl = _getTimeline();
    if (tl) { tl.classList.remove('hidden'); tl.style.display = ''; }

    // Disable button while running
    var btn = _getGenBtn();
    if (btn) {
      btn.disabled = true;
      btn.classList.add('ui-state-disabled');
    }

    _invokeStep(1);
  }

  /**
   * Called from p:remoteCommand oncomplete after step n finishes.
   */
  function onStepDone(n) {
    if (n < TOTAL_STEPS) {
      _invokeStep(n + 1);
    } else {
      _onAllDone();
    }
  }

  /**
   * Called from p:remoteCommand onerror.
   */
  function onStepError(n) {
    _running = false;
    var btn = _getGenBtn();
    if (btn) {
      btn.disabled = false;
      btn.classList.remove('ui-state-disabled');
      var text = btn.querySelector('.ui-button-text');
      if (text) {
        var retryEl = document.getElementById('gen-form:gen-retry-button');
        text.textContent = (retryEl && retryEl.textContent) ? retryEl.textContent.trim() : 'Retry';
      }
    }
  }

  function _onAllDone() {
    _running = false;

    // Grey out the Generate button (data is ready)
    var btn = _getGenBtn();
    if (btn) {
      btn.disabled = true;
      btn.classList.add('ui-state-disabled');
    }

    // Enable the Start Demo button
    var proceed = document.getElementById('form:proceed');
    if (proceed) {
      proceed.disabled = false;
      proceed.classList.remove('ui-state-disabled');
    }
  }

  return {
    start:       start,
    onStepDone:  onStepDone,
    onStepError: onStepError
  };

}());

/* ── Knowledge Base modal ────────────────────────────────────────── */

window.sdOpenKbModal = function () {
  var overlay = document.getElementById('sd-kb-modal');
  if (overlay) {
    overlay.classList.add('is-open');
    document.addEventListener('keydown', _sdKbEsc);
  }
};

window.sdCloseKbModal = function (e) {
  if (e && e.target !== e.currentTarget) return; // ignore clicks inside modal
  var overlay = document.getElementById('sd-kb-modal');
  if (overlay) overlay.classList.remove('is-open');
  document.removeEventListener('keydown', _sdKbEsc);
};

function _sdKbEsc(e) {
  if (e.key === 'Escape') window.sdCloseKbModal();
}

window.sdKbTab = function (btn, panelId) {
  var tabsEl = btn.closest('.sd-kb-tabs');
  tabsEl.querySelectorAll('.sd-kb-tab').forEach(function (t) { t.classList.remove('sd-kb-tab--active'); });
  btn.classList.add('sd-kb-tab--active');
  tabsEl.parentElement.querySelectorAll('.sd-kb-panel').forEach(function (p) { p.style.display = 'none'; });
  var target = document.getElementById(panelId);
  if (target) target.style.display = '';
};

window.sdKbChunkToggle = function (header) {
  header.closest('.sd-kb-chunk').classList.toggle('sd-kb-chunk--open');
};
