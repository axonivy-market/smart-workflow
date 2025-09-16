function showRunningText() {
    $('.js-processing-text').removeClass('hidden');
    $('.js-proceed-button').addClass('hidden');
    $('.js-file-upload').addClass('hidden');
    startAIAnimation();
  }
  
  function hideRunningText() {
    $('.js-processing-text').addClass('hidden');
    stopAIAnimation();
  }
  
  function updateUploadStatus(type, status) {
    const statusElement = document.getElementById(type + '-status');
    const summaryElement = document.getElementById('upload-summary');
    
    if (status === 'success') {
      statusElement.innerHTML = '<i class="pi pi-check-circle text-green-500"></i>';
      summaryElement.style.display = 'block';
    } else if (status === 'error') {
      statusElement.innerHTML = '<i class="pi pi-times-circle text-red-500"></i>';
    }
    
    checkUploadComplete();
  }
  
  function checkUploadComplete() {
    const docStatus = document.getElementById('doc-status').innerHTML.includes('check-circle');
    const imageStatus = document.getElementById('image-status').innerHTML.includes('check-circle');
    
    if (docStatus && imageStatus) {
      document.getElementById('proceed-button').disabled = false;
      document.getElementById('proceed-button').classList.add('pulse-animation');
    }
  }
  
  function resetUploadForm() {
    document.getElementById('doc-status').innerHTML = '<i class="pi pi-clock text-gray-400"></i>';
    document.getElementById('image-status').innerHTML = '<i class="pi pi-clock text-gray-400"></i>';
    document.getElementById('upload-summary').style.display = 'none';
    document.getElementById('proceed-button').disabled = true;
    document.getElementById('proceed-button').classList.remove('pulse-animation');
  }
  
  function startAIAnimation() {
    const steps = document.querySelectorAll('.ai-step');
    let currentStep = 0;
    
    const animateStep = () => {
      if (currentStep < steps.length) {
        steps[currentStep].classList.add('active');
        currentStep++;
        setTimeout(animateStep, 1500);
      }
    };
    
    setTimeout(animateStep, 500);
  }
  
  function stopAIAnimation() {
    document.querySelectorAll('.ai-step').forEach(step => {
      step.classList.remove('active');
    });
  }
  
  // Initialize upload form
  document.addEventListener('DOMContentLoaded', function() {
    // Add drag and drop visual feedback
    const dropzones = document.querySelectorAll('.upload-dropzone');
    
    dropzones.forEach(zone => {
      zone.addEventListener('dragover', function(e) {
        e.preventDefault();
        this.classList.add('drag-over');
      });
      
      zone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        this.classList.remove('drag-over');
      });
      
      zone.addEventListener('drop', function(e) {
        e.preventDefault();
        this.classList.remove('drag-over');
      });
    });
  });