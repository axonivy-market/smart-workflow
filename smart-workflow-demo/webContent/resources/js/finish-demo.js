/**
 * Finish Demo — step wizard navigator.
 * Handles step switching with slide transitions, flow strip highlight, and dot indicators.
 */
(function () {
  'use strict';

  var TOTAL_STEPS = 7;
  var currentStep = 0;
  var wrapper = null;

  /* ── Step navigation ──────────────────────────────────────────── */
  function goToStep(index, isBack) {
    if (index < 0 || index >= TOTAL_STEPS) return;

    // Hide current
    var prev = wrapper.querySelector('.fd-step-panel.fd-active');
    if (prev) prev.classList.remove('fd-active');

    // Direction class on wrapper for CSS animation
    if (isBack) {
      wrapper.classList.add('fd-go-back');
    } else {
      wrapper.classList.remove('fd-go-back');
    }

    // Show new
    var next = wrapper.querySelector('.fd-step-panel[data-step="' + index + '"]');
    if (next) next.classList.add('fd-active');

    currentStep = index;
    syncUI();
  }

  function syncUI() {
    var i = currentStep;

    // Flow strip: active bubble
    document.querySelectorAll('.fd-flow-step').forEach(function (el) {
      var s = parseInt(el.getAttribute('data-step'), 10);
      el.classList.toggle('fd-step-active', s === i);
    });

    // Dots
    document.querySelectorAll('.fd-dot').forEach(function (dot, idx) {
      dot.classList.toggle('fd-dot-active', idx === i);
    });

    // Progress text
    var prog = document.getElementById('fd-progress');
    if (prog) {
      var tpl = prog.getAttribute('data-progress-tpl') || 'Step {0} of {1}';
      prog.textContent = tpl.replace('{0}', i + 1).replace('{1}', TOTAL_STEPS);
    }

    // Prev/next buttons
    var btnPrev = document.getElementById('fd-prev');
    var btnNext = document.getElementById('fd-next');
    if (btnPrev) btnPrev.disabled = (i === 0);
    if (btnNext) btnNext.disabled = (i === TOTAL_STEPS - 1);
  }

  /* ── Public API (called from inline onclick) ──────────────────── */
  window.fdGoToStep = function (index) {
    goToStep(index, index < currentStep);
  };

  window.fdPrevStep = function () {
    if (currentStep > 0) goToStep(currentStep - 1, true);
  };

  window.fdNextStep = function () {
    if (currentStep < TOTAL_STEPS - 1) goToStep(currentStep + 1, false);
  };

  /* ── Keyboard navigation ──────────────────────────────────────── */
  function onKeyDown(e) {
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      window.fdNextStep();
    } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      window.fdPrevStep();
    }
  }

  /* ── Bootstrap ────────────────────────────────────────────────── */
  function init() {
    wrapper = document.querySelector('.fd-steps-wrapper');
    if (!wrapper) return;

    // Activate first step
    var first = wrapper.querySelector('.fd-step-panel[data-step="0"]');
    if (first) first.classList.add('fd-active');

    syncUI();
    document.addEventListener('keydown', onKeyDown);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
