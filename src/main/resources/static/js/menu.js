document.addEventListener('DOMContentLoaded', function() {
    // Если Bootstrap уже загружен и обрабатывает гамбургер – ничего не делаем
    if (typeof bootstrap !== 'undefined') {
        console.log('Bootstrap JS loaded, using native collapse');
        return;
    }

    // Иначе – наше ручное переключение
    var toggler = document.getElementById('menuToggler');
    var menu = document.getElementById('navbarNav');
    if (toggler && menu) {
        toggler.addEventListener('click', function() {
            menu.classList.toggle('show');
            var expanded = this.getAttribute('aria-expanded') === 'true' ? 'false' : 'true';
            this.setAttribute('aria-expanded', expanded);
        });
        console.log('Fallback menu.js activated');
    } else {
        console.warn('menu.js: elements not found');
    }
});