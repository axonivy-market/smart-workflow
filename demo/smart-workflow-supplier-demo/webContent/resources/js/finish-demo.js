(function () {
  'use strict';

  const TOTAL_STEPS = 7;
  let currentStep = 0;
  let wrapper = null;

  function goToStep(index, isBack) {
    if (index < 0 || index >= TOTAL_STEPS) return;

    const prev = wrapper.querySelector('.fd-step-panel.fd-active');
    if (prev) prev.classList.remove('fd-active');

    if (isBack) {
      wrapper.classList.add('fd-go-back');
    } else {
      wrapper.classList.remove('fd-go-back');
    }

    const next = wrapper.querySelector('.fd-step-panel[data-step="' + index + '"]');
    if (next) next.classList.add('fd-active');

    currentStep = index;
    syncUI();
  }

  function syncUI() {
    const step = currentStep;

    document.querySelectorAll('.fd-flow-step').forEach(function (el) {
      const stepIndex = parseInt(el.getAttribute('data-step'), 10);
      el.classList.toggle('fd-step-active', stepIndex === step);
    });

    document.querySelectorAll('.fd-dot').forEach(function (dot, idx) {
      dot.classList.toggle('fd-dot-active', idx === step);
    });

    const btnPrev = document.getElementById('fd-prev');
    const btnNext = document.getElementById('fd-next');
    if (btnPrev) btnPrev.disabled = (step === 0);
    if (btnNext) btnNext.disabled = (step === TOTAL_STEPS - 1);
  }

  window.fdGoToStep = function (index) {
    goToStep(index, index < currentStep);
  };

  window.fdPrevStep = function () {
    if (currentStep > 0) goToStep(currentStep - 1, true);
  };

  window.fdNextStep = function () {
    if (currentStep < TOTAL_STEPS - 1) goToStep(currentStep + 1, false);
  };

  function onKeyDown(e) {
    const t = e.target;
    const tag = t && t.tagName ? t.tagName.toLowerCase() : '';
    if (tag === 'input' || tag === 'textarea' || (t && t.isContentEditable)) {
      return;
    }
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      window.fdNextStep();
    } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      window.fdPrevStep();
    }
  }

  function init() {
    wrapper = document.querySelector('.fd-steps-wrapper');
    if (!wrapper) return;

    const first = wrapper.querySelector('.fd-step-panel[data-step="0"]');
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
