(function (global) {
    const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;

        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : null;
    }

    function getCsrfHeaderName() {
        const meta = document.querySelector('meta[name="_csrf_header"]');
        return meta && meta.content ? meta.content : 'X-CSRF-TOKEN';
    }

    function csrfHeaders() {
        const token = getCsrfToken();
        return token ? { [getCsrfHeaderName()]: token } : {};
    }

    // Добавление CSRF и credentials
    async function apiFetch(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = new Headers(options.headers || {});

        if (!SAFE_METHODS.has(method)) {
            const token = getCsrfToken();
            if (token) {
                headers.set(getCsrfHeaderName(), token);
            }
        }

        return fetch(url, {
            ...options,
            method,
            headers,
            credentials: 'same-origin',
        });
    }

    global.apiFetch = apiFetch;
    global.csrfHeaders = csrfHeaders;
})(window);