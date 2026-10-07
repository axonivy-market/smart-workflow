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


  global.CvAgent    = CvAgent;
  global.CvToolCard = CvToolCard;
  global.CvToolInput = CvToolInput;

})(window);
