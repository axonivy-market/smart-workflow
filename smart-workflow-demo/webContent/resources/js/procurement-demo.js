/**
 * Procurement — Welcome page slide carousel and data-generation timeline.
 *
 * Exports:
 *   pwGoToSlide(i)  pwPrevSlide()  pwNextSlide()   — WelcomePage carousel
 *   pwGen.start() / onStepDone(n) / onStepError(n) — WelcomePage generation
 *   prGen.start() / onStepDone(n) / onStepError(n) — StartDemo generation
 */
(function () {
  'use strict';

  /* ── Welcome-page carousel (pw-*) ──────────────────────────────── */

  var PW_TOTAL_SLIDES = 4;
  var pwCurrentSlide  = 0;
  var pwWrapper       = null;

  function pwGoToSlide(index, isBack) {
    if (index < 0 || index >= PW_TOTAL_SLIDES) { return; }

    var prev = pwWrapper && pwWrapper.querySelector('.fd-step-panel.fd-active');
    if (prev) { prev.classList.remove('fd-active'); }

    if (pwWrapper) { pwWrapper.classList.toggle('fd-go-back', isBack); }

    var next = pwWrapper && pwWrapper.querySelector('.fd-step-panel[data-step="' + index + '"]');
    if (next) { next.classList.add('fd-active'); }

    pwCurrentSlide = index;
    pwSyncUI();
  }

  function pwSyncUI() {
    var i = pwCurrentSlide;
    document.querySelectorAll('.pw-page .fd-dot').forEach(function (dot, idx) {
      dot.classList.toggle('fd-dot-active', idx === i);
    });
    var btnPrev = document.getElementById('pw-prev');
    var btnNext = document.getElementById('pw-next');
    if (btnPrev) { btnPrev.disabled = (i === 0); }
    if (btnNext) { btnNext.disabled = (i === PW_TOTAL_SLIDES - 1); }
  }

  window.pwGoToSlide = function (index) { pwGoToSlide(index, index < pwCurrentSlide); };
  window.pwPrevSlide = function () { if (pwCurrentSlide > 0) { pwGoToSlide(pwCurrentSlide - 1, true); } };
  window.pwNextSlide = function () { if (pwCurrentSlide < PW_TOTAL_SLIDES - 1) { pwGoToSlide(pwCurrentSlide + 1, false); } };

  function pwInit() {
    pwWrapper = document.querySelector('.pw-page .fd-steps-wrapper');
    if (!pwWrapper) { return; }
    var first = pwWrapper.querySelector('.fd-step-panel[data-step="0"]');
    if (first) { first.classList.add('fd-active'); }
    pwSyncUI();
  }

  /* ── Shared generation controller factory ───────────────────────── */

  function makeGenController(opts) {
    /* opts: { timelineSelector, btnSelector, proceedSelector, stepFnPrefix, totalSteps, subtitles } */
    var _running    = false;
    var totalSteps  = opts.totalSteps || 1;
    var subtitles   = opts.subtitles  || [];

    function _tl()  { return document.querySelector(opts.timelineSelector); }
    function _btn() { return document.getElementById(opts.btnSelector); }

    function _setStepRunning(n) {
      var tl = document.querySelector(opts.tlItemsSelector || opts.timelineSelector);
      if (!tl) { return; }
      var items = tl.querySelectorAll('.so-checklist-item');
      var item  = items[n - 1];
      if (!item) { return; }

      item.className = 'so-checklist-item running so-tl-item';

      var bubble = item.querySelector('.so-tl-bubble');
      if (bubble) { bubble.className = 'so-tl-bubble so-tl-bubble-running'; }

      var icon = item.querySelector('.so-tl-bubble i');
      if (icon) { icon.className = 'ti ti-loader so-spin'; }

      var card = item.querySelector('.so-tl-card');
      var sub  = item.querySelector('.so-checklist-subtitle');
      if (!sub && card) {
        sub = document.createElement('div');
        sub.className = 'so-checklist-subtitle';
        card.appendChild(sub);
      }
      if (sub) { sub.textContent = subtitles[n - 1] || 'Processing…'; }
    }

    function _invokeStep(n) {
      _setStepRunning(n);
      var fn = window[opts.stepFnPrefix + n];
      if (typeof fn === 'function') { fn(); }
    }

    function start() {
      if (_running) { return; }
      _running = true;

      var tl = _tl();
      if (tl) { tl.classList.remove('hidden'); tl.style.display = ''; }

      var btn = _btn();
      if (btn) { btn.disabled = true; btn.classList.add('ui-state-disabled'); }

      _invokeStep(1);
    }

    function onStepDone(n) {
      if (n < totalSteps) {
        _invokeStep(n + 1);
      } else {
        _onAllDone();
      }
    }

    function onStepError(n) {
      _running = false;
      var btn = _btn();
      if (btn) {
        btn.disabled = false;
        btn.classList.remove('ui-state-disabled');
        var text = btn.querySelector('.ui-button-text');
        if (text) { text.textContent = 'Retry'; }
      }
    }

    function _onAllDone() {
      _running = false;
      var btn = _btn();
      if (btn) { btn.disabled = true; btn.classList.add('ui-state-disabled'); }

      if (opts.proceedSelector) {
        var proceed = document.getElementById(opts.proceedSelector);
        if (proceed) {
          proceed.disabled = false;
          proceed.classList.remove('ui-state-disabled');
        }
      }
    }

    return { start: start, onStepDone: onStepDone, onStepError: onStepError };
  }

  /* ── WelcomePage generation controller (pwGen) ──────────────────── */
  window.pwGen = makeGenController({
    timelineSelector : '.js-pw-gen-timeline',
    tlItemsSelector  : '.js-pw-tl',
    btnSelector      : 'sd-gen-form:pwGenBtn',
    proceedSelector  : 'sd-footer-form:proceed',
    stepFnPrefix     : 'pwGenStep',
    totalSteps       : 1,
    subtitles        : ['Generating material types, items and inventory…']
  });

  /* ── StartDemo generation controller (prGen) ────────────────────── */
  window.prGen = makeGenController({
    timelineSelector : '.js-pr-gen-timeline',
    tlItemsSelector  : '.js-pr-tl',
    btnSelector      : 'gen-form:prGenBtn',
    proceedSelector  : 'form:proceed',
    stepFnPrefix     : 'prGenStep',
    totalSteps       : 1,
    subtitles        : ['Generating material types, items and inventory…']
  });

  /* ── AI panel resize handle (CreateRequestDialog) ───────────────── */
  function initPanelResize() {
    var handle = document.getElementById('so-resize-handle');
    var panel  = document.getElementById('so-ai-col');
    if (!handle || !panel) { return; }
    var MIN_W = 300, MAX_W = 600;
    var startX, startW;
    handle.addEventListener('mousedown', function (e) {
      startX = e.clientX;
      startW = panel.offsetWidth;
      handle.classList.add('so-dragging');
      document.body.style.cursor     = 'col-resize';
      document.body.style.userSelect = 'none';
      function onMove(e) {
        var delta = startX - e.clientX;
        var w = Math.min(MAX_W, Math.max(MIN_W, startW + delta));
        panel.style.width = w + 'px';
        panel.style.flex  = '0 0 ' + w + 'px';
      }
      function onUp() {
        handle.classList.remove('so-dragging');
        document.body.style.cursor     = '';
        document.body.style.userSelect = '';
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup',   onUp);
      }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup',   onUp);
      e.preventDefault();
    });
  }

  /* ── Bootstrap ──────────────────────────────────────────────────── */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { pwInit(); initPanelResize(); });
  } else {
    pwInit();
    initPanelResize();
  }

}());
