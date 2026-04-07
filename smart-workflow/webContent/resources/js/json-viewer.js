/* ── Constants ────────────────────────────────────────────────────────────────
   All magic values live here with a short description.
   ─────────────────────────────────────────────────────────────────────────── */

/** Maximum characters shown in a peek string preview before truncation. */
const PEEK_STRING_MAX_CHARS = 30;

/** Characters reserved for the ellipsis suffix '…' when truncating a peek string. */
const PEEK_TRUNCATION_SUFFIX_LENGTH = 3;

const CSS_ANIMATION_ENTERING = 'cv-jt-entering';
const CSS_NODE_COLLAPSED = 'cv-jt-collapsed';
const ICON_CLASS_COLLAPSED = 'tif tif-chevron-right';
const ICON_CLASS_EXPANDED = 'tif tif-chevron-down';

/** Data attribute set on each bubble after JSON formatting has been applied. */
const ATTR_JSON_PROCESSED = 'data-cv-json-init';

/** CSS selector for chat bubbles and system blocks that have not yet been processed. */
const SELECTOR_UNPROCESSED = '.cv-bubble:not([' + ATTR_JSON_PROCESSED + ']), .cv-system-block:not([' + ATTR_JSON_PROCESSED + '])';

const JsonParser = Object.freeze({

  extractJsonAt(text, startIndex) {
    const openBrace  = text[startIndex];
    const closeBrace = openBrace === '{' ? '}' : ']';
    let braceDepth      = 0;
    let insideString    = false;
    let nextCharEscaped = false;

    for (let index = startIndex; index < text.length; index++) {
      const character = text[index];
      if (nextCharEscaped)                     { nextCharEscaped = false; continue; }
      if (character === '\\' && insideString)  { nextCharEscaped = true;  continue; }
      if (character === '"')                   { insideString = !insideString; continue; }
      if (insideString)                        { continue; }
      if (character === openBrace)             { braceDepth++; }
      else if (character === closeBrace) {
        braceDepth--;
        if (braceDepth === 0) {
          const jsonCandidate = text.slice(startIndex, index + 1);
          try {
            return { parsed: JSON.parse(jsonCandidate), endIndex: index + 1 };
          } catch (parseError) {
            return null;
          }
        }
      }
    }
    return null;
  },

  getValueType(value) {
    if (value === null)       return 'null';
    if (Array.isArray(value)) return 'array';
    return typeof value;
  }

});

const DomBuilder = Object.freeze({

  textSpan(cssClass, textContent) {
    const span = document.createElement('span');
    span.className = cssClass;
    if (textContent !== undefined) {
      span.appendChild(document.createTextNode(textContent));
    }
    return span;
  },

  keySpan(key) {
    return DomBuilder.textSpan('cv-jt-key', '"' + key + '"');
  },

  indexSpan(index) {
    return DomBuilder.textSpan('cv-jt-index', index);
  },

  colonSpan() {
    return DomBuilder.textSpan('cv-jt-colon', ': ');
  },

  /**
   * Creates the collapsed summary label shown before the toggle chevron,
   * e.g. "[ 4 items ]" or "{ 3 keys }".
   */
  summarySpan(valueType, itemCount) {
    let labelText;
    if (valueType === 'array') {
      labelText = itemCount === 0
        ? '[ ]'
        : '[ ' + itemCount + (itemCount === 1 ? ' item ]' : ' items ]');
    } else {
      labelText = itemCount === 0
        ? '{ }'
        : '{ ' + itemCount + (itemCount === 1 ? ' key }' : ' keys }');
    }
    return DomBuilder.textSpan('cv-jt-summary', labelText);
  },

  peekPreview(objectValue) {
    const keys = Object.keys(objectValue);
    if (keys.length === 0) { return null; }

    const firstKey       = keys[0];
    const firstValue     = objectValue[firstKey];
    const firstValueType = JsonParser.getValueType(firstValue);

    const container = DomBuilder.textSpan('cv-jt-peek');
    container.appendChild(DomBuilder.keySpan(firstKey));
    container.appendChild(DomBuilder.colonSpan());

    let valuePreview;
    if (firstValueType === 'string') {
      const truncated = firstValue.length > PEEK_STRING_MAX_CHARS
        ? firstValue.slice(0, PEEK_STRING_MAX_CHARS - PEEK_TRUNCATION_SUFFIX_LENGTH) + '\u2026'
        : firstValue;
      valuePreview = DomBuilder.textSpan('cv-jt-node--string', '"' + truncated + '"');
    } else if (firstValueType === 'object') {
      const keyCount = Object.keys(firstValue).length;
      valuePreview = DomBuilder.textSpan('cv-jt-summary',
        keyCount === 0 ? '{ }' : '{ ' + keyCount + (keyCount === 1 ? ' key }' : ' keys }'));
    } else if (firstValueType === 'array') {
      const itemCount = firstValue.length;
      valuePreview = DomBuilder.textSpan('cv-jt-summary',
        itemCount === 0 ? '[ ]' : '[ ' + itemCount + (itemCount === 1 ? ' item ]' : ' items ]'));
    } else {
      valuePreview = DomBuilder.textSpan('cv-jt-node--' + firstValueType, String(firstValue));
    }

    container.appendChild(valuePreview);
    return container;
  },

  toggle() {
    const icon = document.createElement('i');
    icon.className = ICON_CLASS_COLLAPSED;

    const span = document.createElement('span');
    span.className = 'cv-jt-toggle';
    span.appendChild(icon);

    return { toggleSpan: span, toggleIcon: icon };
  },

  childrenContainer() {
    return DomBuilder.textSpan('cv-jt-children');
  }

});

const TreeRenderer = Object.freeze({

  renderPrimitive(value, valueType) {
    const displayText = valueType === 'string' ? '"' + value + '"' : String(value);
    return DomBuilder.textSpan('cv-jt-node cv-jt-node--' + valueType, displayText);
  },

  wireToggle(toggleSpan, toggleIcon, nodeSpan, childrenContainer) {
    toggleSpan.addEventListener('click', function (event) {
      event.stopPropagation();
      const isCollapsed = nodeSpan.classList.contains(CSS_NODE_COLLAPSED);
      nodeSpan.classList.toggle(CSS_NODE_COLLAPSED, !isCollapsed);
      toggleIcon.className = isCollapsed ? ICON_CLASS_EXPANDED : ICON_CLASS_COLLAPSED;
      if (isCollapsed) {
        childrenContainer.classList.add(CSS_ANIMATION_ENTERING);
        childrenContainer.addEventListener('animationend', function removeAnimation() {
          childrenContainer.classList.remove(CSS_ANIMATION_ENTERING);
          childrenContainer.removeEventListener('animationend', removeAnimation);
        });
      }
    });
  },

  populateArray(items, container) {
    items.forEach(function (item, index) {
      const row = DomBuilder.textSpan('cv-jt-prop');
      row.appendChild(DomBuilder.indexSpan(index));
      row.appendChild(DomBuilder.colonSpan());
      row.appendChild(TreeRenderer.renderValue(item, { showPeekPreview: JsonParser.getValueType(item) === 'object' }));
      container.appendChild(row);
    });
  },

  populateObject(entries, container) {
    entries.forEach(function (entry) {
      const row = DomBuilder.textSpan('cv-jt-prop');
      row.appendChild(DomBuilder.keySpan(entry[0]));
      row.appendChild(DomBuilder.colonSpan());
      row.appendChild(TreeRenderer.renderValue(entry[1]));
      container.appendChild(row);
    });
  },

  assembleLayout(nodeSpan, toggleSpan, childrenContainer, showPeek, value, valueType, entryCount) {
    if (showPeek) {
      const peek = DomBuilder.peekPreview(value);
      if (peek) { nodeSpan.appendChild(peek); }
      nodeSpan.appendChild(toggleSpan);
    } else {
      nodeSpan.appendChild(toggleSpan);
      nodeSpan.appendChild(DomBuilder.summarySpan(valueType, entryCount));
    }
    nodeSpan.appendChild(childrenContainer);
  },

  renderCollapsible(value, valueType, options) {
    const isArray    = valueType === 'array';
    const entries    = isArray ? value : Object.entries(value);
    const entryCount = entries.length;
    const showPeek   = !isArray && options && options.showPeekPreview && entryCount > 0;

    const nodeSpan   = DomBuilder.textSpan('cv-jt-node cv-jt-node--' + (isArray ? 'arr' : 'obj') + ' ' + CSS_NODE_COLLAPSED);
    const toggle     = DomBuilder.toggle();
    const children   = DomBuilder.childrenContainer();

    TreeRenderer.wireToggle(toggle.toggleSpan, toggle.toggleIcon, nodeSpan, children);

    if (isArray) {
      TreeRenderer.populateArray(entries, children);
    } else {
      TreeRenderer.populateObject(entries, children);
    }

    TreeRenderer.assembleLayout(nodeSpan, toggle.toggleSpan, children, showPeek, value, valueType, entryCount);
    return nodeSpan;
  },

  renderValue(value, options) {
    const valueType = JsonParser.getValueType(value);
    if (valueType === 'object' || valueType === 'array') {
      return TreeRenderer.renderCollapsible(value, valueType, options);
    }
    return TreeRenderer.renderPrimitive(value, valueType);
  },

  buildTreePanel(parsedValue) {
    const panel = document.createElement('div');
    panel.className = 'cv-jt-root';
    panel.appendChild(TreeRenderer.renderValue(parsedValue));

    panel.addEventListener('click', function (event) {
      const collapsedRoot = event.currentTarget.querySelector(':scope > .' + CSS_NODE_COLLAPSED);
      if (collapsedRoot) {
        const toggleControl = collapsedRoot.querySelector(':scope > .cv-jt-toggle');
        if (toggleControl) { toggleControl.click(); }
      }
    });

    return panel;
  }

});

const Formatter = Object.freeze({
  replaceJsonWithTrees(container, rawText) {
    let scanPos     = 0;
    let lastTextPos = 0;
    let jsonFound   = false;

    while (container.firstChild) {
      container.removeChild(container.firstChild);
    }

    while (scanPos < rawText.length) {
      const ch = rawText[scanPos];
      if (ch === '{' || ch === '[') {
        const result = JsonParser.extractJsonAt(rawText, scanPos);
        if (result) {
          const preceding = rawText.slice(lastTextPos, scanPos).trim();
          if (preceding) {
            container.appendChild(document.createTextNode(preceding));
          }
          container.appendChild(TreeRenderer.buildTreePanel(result.parsed));
          jsonFound   = true;
          scanPos     = result.endIndex;
          lastTextPos = scanPos;
          continue;
        }
      }
      scanPos++;
    }

    const trailing = rawText.slice(lastTextPos).trim();
    if (trailing) {
      container.appendChild(document.createTextNode(trailing));
    }
    if (!jsonFound) {
      container.appendChild(document.createTextNode(rawText));
    }

    return jsonFound;
  },

  formatJsonInBubbles() {
    document.querySelectorAll(SELECTOR_UNPROCESSED).forEach(function (bubble) {
      bubble.setAttribute(ATTR_JSON_PROCESSED, '1');

      // For system blocks, format only the content div (skip the label div).
      const content = bubble.classList.contains('cv-system-block')
        ? bubble.querySelector(':scope > div:last-child')
        : bubble;
      if (!content) { return; }

      const rawText = content.textContent;
      if (!rawText.trim()) { return; }

      Formatter.replaceJsonWithTrees(content, rawText);
    });
  }

});

window.JsonViewer = Object.freeze({
  formatJsonInBubbles: Formatter.formatJsonInBubbles
});

document.addEventListener('DOMContentLoaded', function () {
  Formatter.formatJsonInBubbles();
  if (typeof $ !== 'undefined') {
    $(document).on('pfAjaxComplete', Formatter.formatJsonInBubbles);
  }
});
