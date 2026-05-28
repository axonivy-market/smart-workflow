var CvAgent = (function () {
  function toggle(headerEl) {
    var block = headerEl.closest('.cv-agent-block');
    if (!block) return;
    block.classList.toggle('cv-agent-open');
    var body = block.querySelector('.cv-agent-body');
    if (body) body.classList.toggle('pb-3');
  }
  return { toggle: toggle };
})();


var CvMarkdown = (function () {

  function isAllStringObject(obj) {
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) return false;
    var keys = Object.keys(obj);
    return keys.length > 0 && keys.every(function (k) { return typeof obj[k] === 'string'; });
  }

  function flatStringToMarkdown(obj) {
    var keys = Object.keys(obj);
    if (keys.length === 1) return obj[keys[0]];
    return keys.map(function (k) { return '**' + k + '**\n\n' + obj[k]; }).join('\n\n---\n\n');
  }

  function deepParse(value) {
    if (typeof value === 'string') {
      try {
        var inner = JSON.parse(value);
        if (inner !== null && typeof inner === 'object') return deepParse(inner);
      } catch (e) {}
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

      if ((trimmed.startsWith('{') || trimmed.startsWith('[')) && !trimmed.startsWith('```')) {
        try {
          var parsed = JSON.parse(trimmed);
          var deep = deepParse(parsed);
          if (isAllStringObject(deep)) {
            source = flatStringToMarkdown(deep);
          } else {
            source = '```json\n' + JSON.stringify(deep, null, 2) + '\n```';
          }
        } catch (e) {}
      }

      el.innerHTML = DOMPurify.sanitize(marked.parse(source));
      el.setAttribute('data-md', '1');
    });
  }

  document.addEventListener('DOMContentLoaded', renderAll);

  return { renderAll: renderAll };
})();


var CvSysPrompt = (function () {
  function toggle(headerEl) {
    var block = headerEl.closest('.cv-agent-block');
    if (block) block.classList.toggle('cv-sysPrompt-open');
  }
  return { toggle: toggle };
})();


var CvRaw = (function () {
  function toggle(input) {
    var isPretty = input.checked;
    var block = input.closest('.cv-agent-block');
    var tabview = block ? block.querySelector('.cv-agent-tabview') : null;
    if (tabview) tabview.classList.toggle('cv-raw-mode', !isPretty);
  }
  return { toggle: toggle };
})();


var CvNav = (function () {

  function select(agentId, linkEl) {
    document.querySelectorAll('.cv-detail-area [data-cv-agent]').forEach(function (el) {
      el.style.display = 'none';
    });
    var target = document.querySelector('[data-cv-agent="' + agentId + '"]');
    if (target) target.style.display = '';

    document.querySelectorAll('.cv-nav-panel-menu .ui-menuitem').forEach(function (el) {
      el.classList.remove('cv-nav-active');
    });
    if (linkEl) {
      var item = linkEl.closest('.ui-menuitem');
      if (item) item.classList.add('cv-nav-active');
    }
  }

  function init() {
    var firstBlock = document.querySelector('.cv-detail-area [data-cv-agent]');
    if (firstBlock) firstBlock.style.display = '';
    var firstItem = document.querySelector('.cv-nav-panel-menu .ui-menuitem');
    if (firstItem) firstItem.classList.add('cv-nav-active');
  }

  document.addEventListener('DOMContentLoaded', init);

  return { select: select };
})();


var CvToolCard = (function () {
  function toggle(headerEl) {
    var card = headerEl.closest('.tool-timeline-card');
    if (!card) return;
    card.classList.toggle('open');
    ['border-bottom-1', 'surface-border', 'surface-50'].forEach(function (cls) {
      headerEl.classList.toggle(cls);
    });
  }
  return { toggle: toggle };
})();


var CvToolInput = (function () {
  function toggle(toggleEl) {
    var section = toggleEl.closest('.tool-timeline-input');
    if (section) section.classList.toggle('open');
  }
  return { toggle: toggle };
})();


var CvToolModal = (function () {

  function open(title, text) {
    var titleEl = document.getElementById('tool-timeline-modal-title');
    var bodyEl  = document.getElementById('tool-timeline-modal-body');
    if (!titleEl || !bodyEl) return;

    titleEl.textContent = title;

    var isMarkdown = /^#{1,3} /m.test(text) || /\|[-| ]+\|/.test(text);
    if (typeof marked !== 'undefined' && isMarkdown) {
      marked.setOptions({ breaks: true, gfm: true });
      bodyEl.innerHTML = DOMPurify.sanitize(marked.parse(text));
    } else {
      bodyEl.innerHTML = '<pre style="white-space:pre-wrap;word-break:break-word;margin:0">' + escHtml(text) + '</pre>';
    }

    PF('cvToolModal').show();
  }

  function close() {
    PF('cvToolModal').hide();
  }

  function escHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  return { open: open, close: close };
})();


var CvToolTimeline = (function () {

  var THRESHOLD = 200;

  function initValueCells() {
    document.querySelectorAll('.tool-timeline-val-cell').forEach(function (cell) {
      var rawPre   = cell.querySelector('.cv-tab-raw-text');
      var prettyEl = cell.querySelector('.cv-tab-msg-text');
      if (!rawPre || !prettyEl) return;

      var rawText = rawPre.textContent.trim();
      if (rawText.length <= THRESHOLD) return;

      var row = cell.closest('.tool-timeline-kv-row');
      var keyCell = row ? row.querySelector('.tool-timeline-kv-key') : null;
      var label = keyCell ? keyCell.textContent.trim() : 'Value';

      var preview = document.createElement('div');
      preview.className = 'flex align-items-center gap-2 flex-wrap';

      var previewText = document.createElement('span');
      previewText.className = 'text-color-secondary font-italic overflow-hidden white-space-nowrap text-overflow-ellipsis flex-1';
      previewText.textContent = rawText.substring(0, 100).trim() + '\u2026';

      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'tool-timeline-expand-btn inline-flex align-items-center justify-content-center w-2rem h-2rem border-circle bg-transparent text-color-secondary cursor-pointer text-base p-0 flex-shrink-0';
      btn.title = 'View full';
      btn.innerHTML = '<i class="ti ti-arrows-maximize"></i>';

      (function (capturedLabel, capturedText) {
        btn.addEventListener('click', function () {
          CvToolModal.open(capturedLabel, capturedText);
        });
      })(label, rawText);

      preview.appendChild(previewText);
      preview.appendChild(btn);
      prettyEl.parentNode.replaceChild(preview, prettyEl);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    setTimeout(initValueCells, 0);
  });

  return {};
})();
