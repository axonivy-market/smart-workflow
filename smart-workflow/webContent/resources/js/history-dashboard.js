var HistoryDashboard = {
  navigateToViewer: function(caseId, taskId) {
    window.location.href = 'ConversationViewer.xhtml'
      + '?caseUuid=' + encodeURIComponent(caseId)
      + '&taskUuid=' + encodeURIComponent(taskId);
  }
};
