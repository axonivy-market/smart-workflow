/**
 * Supplier Validation — client-side step orchestration.
 *
 * Namespace: soValidation (module pattern, no global pollution beyond
 * the thin shim functions at the bottom that XHTML oncomplete hooks call).
 */
var soValidation = (function () {
  'use strict';

  var _running = false;
  var _failed  = false;

  // ── DOM helpers ─────────────────────────────────────────────────────────────

  function _getAnalyzeBtn() {
    // Button is inside h:form id="form", so JSF client-id is "form:analyzeBtn"
    return document.getElementById('form:analyzeBtn');
  }

  function _getContinueBtn() {
    // Continue button is in the template footer, outside the form
    return document.getElementById('continue');
  }

  function _setAnalyzeBtnState(disabled, label) {
    var btn = _getAnalyzeBtn();
    if (!btn) { return; }
    if (disabled) {
      btn.disabled = true;
      btn.classList.add('ui-state-disabled');
    } else {
      btn.disabled = false;
      btn.classList.remove('ui-state-disabled');
    }
    if (label) {
      var text = btn.querySelector('.ui-button-text');
      if (text) { text.textContent = label; }
    }
  }

  function _enableContinueBtn() {
    var btn = _getContinueBtn();
    if (!btn) { return; }
    btn.removeAttribute('disabled');
    btn.classList.remove('ui-state-disabled');
  }

  // ── Step subtitles (mirrors bean constants) ─────────────────────────────────

  var STEP_SUBTITLES = [
    'Extracting supplier documents',
    'Validating against onboarding policy',
    'Running cross-reference checks',
    'Calculating risk score'
  ];

  // ── Step invocation ─────────────────────────────────────────────────────────

  /**
   * Patch the nth checklist row to look "running" before the AJAX call fires.
   * The server overwrites this with the real state when the response arrives.
   * @param {number} n  1-based step index
   */
  function _setStepRunning(n) {
    var panel = document.querySelector('.js-checklist-panel');
    if (!panel) { return; }
    var items = panel.querySelectorAll('.so-checklist-item');
    var item  = items[n - 1];
    if (!item) { return; }

    // Row class (preserve so-tl-item layout class)
    item.className = 'so-checklist-item running so-tl-item';

    // Bubble → running state
    var bubble = item.querySelector('.so-tl-bubble');
    if (bubble) {
      bubble.className = 'so-tl-bubble so-tl-bubble-running';
    }

    // Icon inside bubble: spinner
    var icon = item.querySelector('.so-tl-bubble i');
    if (icon) {
      icon.className = 'ti ti-loader so-spin';
    }

    // Subtitle inside the card (appears below the header row)
    var card = item.querySelector('.so-tl-card');
    var subtitle = item.querySelector('.so-checklist-subtitle');
    if (!subtitle) {
      subtitle = document.createElement('div');
      subtitle.className = 'so-checklist-subtitle';
      (card || item).appendChild(subtitle);
    }
    subtitle.textContent = STEP_SUBTITLES[n - 1] || 'Processing';
  }

  /**
   * Invoke a PrimeFaces remoteCommand by step number (1-based).
   * Patches the DOM first so the running animation is visible immediately.
   * @param {number} n  1..4
   */
  function invokeStep(n) {
    _setStepRunning(n);
    var cmd = window['runStep' + n];
    if (typeof cmd === 'function') {
      cmd();
    }
  }

  // ── Public API ──────────────────────────────────────────────────────────────

  /**
   * Entry point called by the Analyze button onclick.
   * Disables the button, calls startAnalysisRC (which initialises 4 PENDING
   * steps server-side), and the oncomplete of that RC triggers invokeStep(1).
   */
  function startAnalysis() {
    if (_running) { return; }
    _running = true;
    _failed  = false;
    _setAnalyzeBtnState(true, null);
    var cmd = window['startAnalysisRC'];
    if (typeof cmd === 'function') { cmd(); }
  }

  /**
   * Called from p:remoteCommand oncomplete after each step completes.
   * Updates badges and triggers the next step, or finalises on step 4.
   * @param {number} n  1-based index of the step that just completed
   */
  function onStepDone(n) {
    _updateStepSummaryBadges();
    if (n < 4) {
      invokeStep(n + 1);
    } else {
      onAllStepsDone();
    }
  }

  /**
   * Called from p:remoteCommand oncomplete when the orchestration call completes.
   * Updates badges, enables Continue, and re-enables the Analyze button.
   */
  function onAllStepsDone() {
    _updateStepSummaryBadges();
    _running = false;
    var flagEl = document.getElementById('stepFailedFlag');
    if (flagEl && flagEl.getAttribute('data-failed') === 'true') {
      _failed = true;
      _setAnalyzeBtnState(false, 'Retry');
      return;
    }
    _setAnalyzeBtnState(false, 'Re-analyze');
    _enableContinueBtn();
  }

  /**
   * Called from p:remoteCommand onerror when the orchestration AJAX call fails.
   */
  function onAnalysisError() {
    _running = false;
    _failed  = true;
    _setAnalyzeBtnState(false, 'Retry');
  }

  /**
   * Scan each step card for error/warning log lines and inject a compact
   * summary badge ("2 errors · 1 warning") next to the step title.
   * Called after each step completes so the badge is visible even when collapsed.
   */
  function _updateStepSummaryBadges() {
    var panel = document.querySelector('.js-checklist-panel');
    if (!panel) { return; }
    panel.querySelectorAll('.so-tl-card').forEach(function(card) {
      var errors   = card.querySelectorAll('.so-log-line-error').length;
      var warnings = card.querySelectorAll('.so-log-line-warning').length;
      var existing = card.querySelector('.so-step-issue-badge');
      if (errors === 0 && warnings === 0) {
        if (existing) { existing.remove(); }
        return;
      }
      if (!existing) {
        existing = document.createElement('span');
        var chevron = card.querySelector('.so-tl-chevron');
        if (chevron) { chevron.parentElement.insertBefore(existing, chevron); }
      }
      var parts = [];
      if (errors)   { parts.push(errors   + (errors   === 1 ? ' error'   : ' errors')); }
      if (warnings) { parts.push(warnings + (warnings === 1 ? ' warning' : ' warnings')); }
      existing.textContent = parts.join(' · ');
      existing.className = 'so-step-issue-badge ' +
        (errors > 0 ? 'so-step-issue-badge--error' : 'so-step-issue-badge--warning');
    });
  }

  /**
   * Called from p:remoteCommand onerror when an AJAX step fails.
   * Stops the chain and re-enables the Analyze button for retry.
   * @param {number} stepIndex  1-based index of the step that failed
   */
  function onStepError(stepIndex) {
    _running = false;
    _failed  = true;
    _setAnalyzeBtnState(false, 'Retry');
  }

  /**
   * Resets the analysis state so a fresh chain can be started.
   * Called automatically when the dialog is re-opened after clarification.
   */
  function resetAnalysis() {
    _running = false;
    _failed  = false;
    _setAnalyzeBtnState(false, 'Analyze');
  }

  return {
    startAnalysis:  startAnalysis,
    invokeStep:     invokeStep,
    onStepDone:     onStepDone,
    onAllStepsDone: onAllStepsDone,
    onAnalysisError: onAnalysisError,
    onStepError:    onStepError,
    resetAnalysis:  resetAnalysis
  };

}());

// ── Timeline card toggle ─────────────────────────────────────────────────────
/**
 * Expand/collapse the .so-tl-lines block inside a timeline card.
 * Called by onclick on .so-tl-card-header.
 */
function soTlToggle(headerEl) {
  var card      = headerEl.parentElement;
  var lines     = card.querySelector('.so-tl-lines');
  if (!lines) { return; }
  var collapsed = card.classList.toggle('so-tl-collapsed');
  if (collapsed) {
    lines.style.display = 'none';
  } else {
    lines.style.display = '';
    // Replay stagger animation when expanding
    lines.querySelectorAll('.so-tl-line-item').forEach(function(item, i) {
      item.style.animationDelay = (i * 35) + 'ms';
      item.style.animationName  = 'none';
      requestAnimationFrame(function() {
        requestAnimationFrame(function() { item.style.animationName = ''; });
      });
    });
  }
}

// These delegate to the soValidation module to avoid namespace collisions.

function startAnalysis()        { soValidation.startAnalysis(); }
function onStepDone(stepIndex)  { soValidation.onStepDone(stepIndex); }
function onStepError(stepIndex) { soValidation.onStepError(stepIndex); }
function onAllStepsDone()       { soValidation.onAllStepsDone(); }
function onAnalysisError()      { soValidation.onAnalysisError(); }
