(function () {
  'use strict';

  const TOTAL_SLIDES = 4;
  let currentSlide = 0;
  let wrapper = null;

  const INNER_TOTAL = 7;
  let currentInner = 0;
  let innerWrapper = null;

  function goToSlide(index, isBack) {
    if (index < 0 || index >= TOTAL_SLIDES) return;

    const prev = wrapper.querySelector('.fd-step-panel.fd-active');
    if (prev) prev.classList.remove('fd-active');

    wrapper.classList.toggle('fd-go-back', isBack);

    const next = wrapper.querySelector('.fd-step-panel[data-step="' + index + '"]');
    if (next) next.classList.add('fd-active');

    currentSlide = index;
    syncUI();
  }

  function syncUI() {
    const i = currentSlide;

    document.querySelectorAll('.fd-dot').forEach(function (dot, idx) {
      dot.classList.toggle('fd-dot-active', idx === i);
    });

    const btnPrev = document.getElementById('sd-prev');
    const btnNext = document.getElementById('sd-next');
    if (btnPrev) btnPrev.disabled = (i === 0);
    if (btnNext) btnNext.disabled = (i === TOTAL_SLIDES - 1);
  }

  function goToInner(index) {
    if (index < 0 || index >= INNER_TOTAL || !innerWrapper) return;
    const prev = innerWrapper.querySelector('.sd-inner-panel.sd-inner-active');
    if (prev) prev.classList.remove('sd-inner-active');
    const next = innerWrapper.querySelector('.sd-inner-panel[data-panel="' + index + '"]');
    if (next) next.classList.add('sd-inner-active');
    currentInner = index;
    syncInnerUI();
  }

  function syncInnerUI() {
    const i = currentInner;
    document.querySelectorAll('.sd-inner-dot').forEach(function (dot, idx) {
      dot.classList.toggle('sd-inner-dot-active', idx === i);
    });
    document.querySelectorAll('.fd-flow .fd-flow-step[data-panel]').forEach(function (step) {
      step.classList.toggle('sd-inner-flow-active', parseInt(step.getAttribute('data-panel'), 10) === i);
    });
  }

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

  function init() {
    wrapper = document.querySelector('.fd-steps-wrapper');
    if (!wrapper) return;

    const first = wrapper.querySelector('.fd-step-panel[data-step="0"]');
    if (first) first.classList.add('fd-active');

    innerWrapper = document.querySelector('.sd-inner-wrapper');
    if (innerWrapper) {
      const firstInner = innerWrapper.querySelector('.sd-inner-panel[data-panel="0"]');
      if (firstInner) firstInner.classList.add('sd-inner-active');
      syncInnerUI();
    }

    const alreadyGenEl = document.getElementById('gen-form:data-already-generated');
    if (!alreadyGenEl) {
      const proceed = document.getElementById('form:proceed');
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
}());

window.sdToggleDownloads = function (link) {
  const body = document.querySelector('.sd-download-body');
  if (!body) return;
  const open = body.classList.toggle('sd-download-body--open');
  const chevron = link ? link.querySelector('.sd-details-chevron') : null;
  if (chevron) chevron.style.transform = open ? 'rotate(180deg)' : '';
  const label = link ? link.querySelector('span') : null;
  if (label) {
    const hideEl   = document.getElementById('gen-form:file-details-hide');
    const toggleEl = document.getElementById('gen-form:file-details-toggle');
    label.textContent = open
      ? (hideEl   && hideEl.textContent   ? hideEl.textContent.trim()   : 'Hide details')
      : (toggleEl && toggleEl.textContent ? toggleEl.textContent.trim() : 'File details');
  }
};

window.sdGen = (function () {
  'use strict';

  let _running = false;
  const TOTAL_STEPS = 3;

  function _getStepSubtitle(n) {
    const el = document.getElementById('gen-form:gen' + n + '-running-subtitle');
    if (el && el.textContent) { return el.textContent.trim(); }
    const fallback = document.getElementById('gen-form:gen-processing-subtitle');
    return (fallback && fallback.textContent) ? fallback.textContent.trim() : 'Processing\u2026';
  }

  function _getGenBtn() {
    return document.getElementById('gen-form:sdGenBtn');
  }

  function _setStepRunning(n) {
    const checklistEl = document.querySelector('.js-sd-tl');
    if (!checklistEl) { return; }
    const items = checklistEl.querySelectorAll('.so-checklist-item');
    const item  = items[n - 1];
    if (!item) { return; }

    item.classList.remove('pending', 'completed', 'failed');
    item.classList.add('running');

    const bubble = item.querySelector('.so-tl-bubble');
    if (bubble) {
      bubble.classList.remove('so-tl-bubble-pending', 'so-tl-bubble-completed', 'so-tl-bubble-failed');
      bubble.classList.add('so-tl-bubble-running');
    }

    const icon = item.querySelector('.so-tl-bubble i');
    if (icon) { icon.className = 'ti ti-loader so-spin'; }

    const card     = item.querySelector('.so-tl-card');
    let subtitle   = item.querySelector('.so-checklist-subtitle');
    if (!subtitle && card) {
      subtitle = document.createElement('div');
      subtitle.className = 'so-checklist-subtitle';
      card.appendChild(subtitle);
    }
    if (subtitle) { subtitle.textContent = _getStepSubtitle(n); }
  }

  function _invokeStep(n) {
    _setStepRunning(n);
    const stepCommand = window['sdGenStep' + n];
    if (typeof stepCommand === 'function') { stepCommand(); }
  }

  function start() {
    if (_running) { return; }
    _running = true;

    const timelinePanel = document.querySelector('.js-sd-gen-timeline');
    if (timelinePanel) { timelinePanel.classList.remove('hidden'); }

    const btn = _getGenBtn();
    if (btn) {
      btn.disabled = true;
      btn.classList.add('ui-state-disabled');
    }

    _invokeStep(1);
  }

  function onStepDone(n) {
    if (n < TOTAL_STEPS) {
      _invokeStep(n + 1);
    } else {
      _onAllDone();
    }
  }

  function onStepError() {
    _running = false;
    const btn = _getGenBtn();
    if (btn) {
      btn.disabled = false;
      btn.classList.remove('ui-state-disabled');
      const text = btn.querySelector('.ui-button-text');
      if (text) {
        const retryEl = document.getElementById('gen-form:gen-retry-button');
        text.textContent = (retryEl && retryEl.textContent) ? retryEl.textContent.trim() : 'Retry';
      }
    }
  }

  function _onAllDone() {
    _running = false;

    const btn = _getGenBtn();
    if (btn) {
      btn.disabled = true;
      btn.classList.add('ui-state-disabled');
    }

    const proceed = document.getElementById('form:proceed');
    if (proceed) {
      proceed.disabled = false;
      proceed.classList.remove('ui-state-disabled');
    }
  }

  return { start, onStepDone, onStepError };

}());

window.sdKbChunkToggle = function (header) {
  header.closest('.sd-kb-chunk').classList.toggle('sd-kb-chunk--open');
};
