(function (global) {
  function toggleClosest(el, containerSel, openClass) {
    var container = el.closest(containerSel);
    if (container) container.classList.toggle(openClass);
  }


  var CvAgent = (function () {
    function toggle(headerEl) {
      toggleClosest(headerEl, '.cv-agent-block', 'cv-agent-open');
    }

    function initFirstOpen() {
      var first = document.querySelector('.cv-tl-item .cv-agent-block');
      if (first) first.classList.add('cv-agent-open');
    }

    document.addEventListener('DOMContentLoaded', initFirstOpen);

    return { toggle: toggle };
  })();


  var CvToolCard = (function () {
    function toggle(headerEl) {
      var card = headerEl.closest('.tool-timeline-card');
      if (!card) return;
      card.classList.toggle('open');
      headerEl.classList.toggle('cv-tool-card-header--open');
      var body = card.querySelector('.tool-timeline-card-body');
      if (body) body.classList.toggle('hidden');
    }
    return { toggle: toggle };
  })();


  var CvToolInput = (function () {
    function toggle(toggleEl) {
      var section = toggleEl.closest('.tool-timeline-input');
      if (!section) return;
      section.classList.toggle('open');
      var body = section.querySelector('.tool-timeline-input-body');
      if (body) body.classList.toggle('hidden');
    }
    return { toggle: toggle };
  })();


  var CvAiAnalyze = (function () {
    var _interval = null;
    var _idx = 0;

    function start() {
      var btnWrap = document.getElementById('cv-ai-gen-btn-wrap');
      var panel   = document.getElementById('cv-ai-analyzing-inline');
      var msgEl   = document.getElementById('cv-ai-analyze-msg-text');
      if (!panel || !msgEl) return;

      var raw      = panel.getAttribute('data-messages') || '';
      var messages = raw.split('|').map(function (s) { return s.trim(); }).filter(Boolean);
      if (!messages.length) return;

      if (btnWrap) btnWrap.classList.add('hidden');
      panel.classList.remove('hidden');
      panel.classList.add('flex');

      _idx = 0;
      _setMsg(msgEl, messages[0]);

      _interval = setInterval(function () {
        _idx = (_idx + 1) % messages.length;
        _setMsg(msgEl, messages[_idx]);
      }, 1500);
    }

    function stop() {
      if (_interval) { clearInterval(_interval); _interval = null; }
    }

    function _setMsg(el, text) {
      el.style.opacity = '0';
      setTimeout(function () {
        el.textContent = text;
        el.style.opacity = '1';
      }, 180);
    }

    return { start: start, stop: stop };
  })();

  global.CvAgent     = CvAgent;
  global.CvToolCard  = CvToolCard;
  global.CvToolInput = CvToolInput;
  global.CvAiAnalyze = CvAiAnalyze;

})(window);

