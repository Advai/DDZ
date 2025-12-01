// Dou Dizhu Game Configuration
// This file can be modified at deployment time to point to the correct backend

window.DDZ_CONFIG = {
    // Backend API URL - change this for your deployment
    // Local development: 'http://localhost:8080'
    // Production: window.location.origin (same origin as frontend)
    // Or specify explicit URL: 'https://your-backend.fly.dev'
    BACKEND_URL: window.location.protocol === 'file:'
        ? 'http://localhost:8080'  // Local file testing
        : window.location.origin,   // Production: same origin

    // WebSocket URL is derived from BACKEND_URL
    get WS_URL() {
        return this.BACKEND_URL.replace('http://', 'ws://').replace('https://', 'wss://');
    }
};

// For debugging
console.log('DDZ Config loaded:', window.DDZ_CONFIG);
