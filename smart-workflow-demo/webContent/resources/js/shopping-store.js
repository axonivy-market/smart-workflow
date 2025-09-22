let searchStartTime;

function performSearch() {
  const searchTerm = document.getElementById('mainSearchInput').value.trim();
  if (searchTerm) {
    searchStartTime = Date.now();
    // Copy search term to top search input
    document.getElementById('topSearchInput').value = searchTerm;
    
    // Trigger the actual search
    searchProducts();
    
    // Show search transition animation
    showSearchResults();
  }
}

function searchProducts() {
  // Trigger PrimeFaces search command
  PF('searchBtn')?.jq.click();
}

function showSearchResults() {
  const centeredSearch = document.getElementById('centeredSearch');
  const topSearchBar = document.getElementById('topSearchBar');
  const resultsSection = document.getElementById('resultsSection');
  
  // Add transition classes
  centeredSearch.style.transform = 'translateY(-50px)';
  centeredSearch.style.opacity = '0';
  
  setTimeout(() => {
    centeredSearch.style.display = 'none';
    topSearchBar.style.display = 'block';
    resultsSection.style.display = 'block';
    
    // Animate in top search bar
    topSearchBar.style.opacity = '0';
    topSearchBar.style.transform = 'translateY(-20px)';
    
    setTimeout(() => {
      topSearchBar.style.transition = 'all 0.4s ease';
      topSearchBar.style.opacity = '1';
      topSearchBar.style.transform = 'translateY(0)';
      
      // Animate in results
      setTimeout(() => {
        resultsSection.style.opacity = '0';
        resultsSection.style.transform = 'translateY(20px)';
        resultsSection.style.transition = 'all 0.4s ease';
        
        setTimeout(() => {
          resultsSection.style.opacity = '1';
          resultsSection.style.transform = 'translateY(0)';
          animateProductCards();
        }, 50);
      }, 200);
    }, 50);
  }, 400);
}

function addFocusEffect() {
  const searchBox = document.querySelector('.search-box');
  searchBox.classList.add('focused');
}

function removeFocusEffect() {
  const searchBox = document.querySelector('.search-box');
  searchBox.classList.remove('focused');
}