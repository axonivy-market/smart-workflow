function soApplyScoreBars(container) {
  (container || document).querySelectorAll('.so-score-bar-fill[data-score]').forEach(function(el) {
    el.style.width = el.dataset.score + '%';
  });
}

function soToggleBannerScore(link) {
  var $details = $(link).closest('.so-clarification-banner-main').find('.so-banner-score-details');
  var isHidden = $details.hasClass('hidden');
  $details.toggleClass('hidden', !isHidden);
  $(link).text(isHidden ? 'hide details' : 'show details');
  if (isHidden) {
    soApplyScoreBars($details[0]);
  }
}
