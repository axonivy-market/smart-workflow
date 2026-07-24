window.isHideTaskName = true;
window.isHideTaskAction = true;
window.isHideCaseInfo = true;

var HistoryDashboard = {
  navigateToViewer: function(caseId, taskId) {
    window.location.href = 'ConversationViewer.xhtml'
      + '?caseUuid=' + encodeURIComponent(caseId)
      + '&taskUuid=' + encodeURIComponent(taskId);
  }
};
