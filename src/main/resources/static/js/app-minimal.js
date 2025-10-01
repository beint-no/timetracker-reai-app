// Timer state
let timerInterval = null;
let timerStartTime = null;
let activeTab = 'today';

// Token management
function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('access_token');
    if (urlToken) {
        localStorage.setItem('reai_access_token', 'Bearer ' + urlToken);
        window.history.replaceState({}, document.title, window.location.pathname);
    }
    return localStorage.getItem('reai_access_token') || '';
}

function isTokenExpired(token) {
    if (!token || !token.startsWith('Bearer ')) return true;
    try {
        const jwtToken = token.replace('Bearer ', '');
        const payload = JSON.parse(atob(jwtToken.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
    } catch (error) {
        return true;
    }
}

function checkToken() {
    const token = getAuthToken();
    if (!token || isTokenExpired(token)) {
        showTokenDialog();
        return false;
    }
    return true;
}

function showTokenDialog() {
    const dialog = document.getElementById('token-dialog');
    if (dialog) dialog.open = true;
}

function closeTokenDialog() {
    const dialog = document.getElementById('token-dialog');
    if (dialog) dialog.open = false;
}

function goToReai() {
    window.location.href = document.body.dataset.reaiUrl || 'http://localhost:8080/login';
}

// Get selected values from DOM
function getSelectedEmployee() {
    const select = document.getElementById('employee-select');
    const value = select?.value;
    return value && value !== '' ? Number(value) : null;
}

function getSelectedProject() {
    const select = document.getElementById('timer-project-select');
    const value = select?.value;
    if (value === 'all' || !value) return null;
    return Number(value);
}

function getSelectedProjectName() {
    const select = document.getElementById('timer-project-select');
    if (!select || !select.value) return '';
    const option = select.querySelector(`wa-option[value="${select.value}"]`);
    return option ? option.textContent : '';
}

// Timer display functions
function startTimerDisplay(startTime) {
    timerStartTime = new Date(startTime);
    if (timerInterval) clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        const now = new Date();
        const diff = now - timerStartTime;
        const hours = Math.floor(diff / 3600000);
        const minutes = Math.floor((diff % 3600000) / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);

        const display = document.getElementById('timer-time');
        if (display) {
            display.textContent = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
            display.classList.add('running');
        }
    }, 1000);

    document.getElementById('timer-input-area')?.classList.add('active');
    const startBtn = document.getElementById('start-btn');
    const stopBtn = document.getElementById('stop-btn');
    if (startBtn) startBtn.style.display = 'none';
    if (stopBtn) stopBtn.style.display = 'inline-flex';
}

function stopTimerDisplay() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
    const display = document.getElementById('timer-time');
    if (display) {
        display.textContent = '00:00:00';
        display.classList.remove('running');
    }
    document.getElementById('timer-input-area')?.classList.remove('active');
    const startBtn = document.getElementById('start-btn');
    const stopBtn = document.getElementById('stop-btn');
    if (startBtn) startBtn.style.display = 'inline-flex';
    if (stopBtn) stopBtn.style.display = 'none';
}

// Start timer button click
function startTimerClick() {
    if (!checkToken()) return;

    const employeeId = getSelectedEmployee();
    const projectId = getSelectedProject();

    if (employeeId == null) {
        showNotification('Please select an employee first', 'danger');
        return;
    }

    if (projectId == null) {
        showNotification('Please select a project', 'warning');
        return;
    }

    const projectName = getSelectedProjectName();
    htmx.ajax('POST', '/htmx/timer/start', {
        target: '#timer-display',
        swap: 'innerHTML',
        values: { employeeId, projectId, projectName }
    });
}

// Notification
function showNotification(message, variant = 'success') {
    const notification = document.createElement('wa-callout');
    notification.variant = variant;
    notification.closable = true;
    notification.style.cssText = 'position: fixed; top: 1rem; right: 1rem; z-index: 10000; min-width: 300px; animation: slideIn 0.3s ease-out;';
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.transition = 'opacity 0.5s';
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 500);
    }, 4000);
}

// Update stats
function updateStatsDashboard() {
    const employeeId = getSelectedEmployee();
    if (employeeId == null) {
        const dash = document.getElementById('stats-dashboard');
        if (dash) dash.style.display = 'none';
        return;
    }

    const projectId = getSelectedProject();
    const values = { employeeId, tab: activeTab };
    if (projectId != null) values.projectId = projectId;

    htmx.ajax('GET', '/htmx/stats', {
        target: '#stats-content',
        swap: 'innerHTML',
        values
    }).then(() => {
        const dash = document.getElementById('stats-dashboard');
        if (dash) dash.style.display = 'block';
    });
}

// Load entries for tab
function loadEntriesForTab(tabName) {
    const employeeId = getSelectedEmployee();
    if (employeeId == null) return;

    const projectId = getSelectedProject();
    const values = { employeeId, limit: 10, offset: 0 };
    if (projectId != null) values.projectId = projectId;

    let endpoint = '/htmx/entries/today';
    let targetId = 'entries-content-today';

    if (tabName === 'all') {
        endpoint = '/htmx/entries/all';
        targetId = 'entries-content-all';
    }

    htmx.ajax('GET', endpoint, {
        target: `#${targetId}`,
        swap: 'innerHTML',
        values
    });
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    checkToken();

    // Load entries for current tab on page load
    const employeeId = getSelectedEmployee();
    if (employeeId) {
        loadEntriesForTab(activeTab);
    }

    // Tab changes
    const tabGroup = document.getElementById('entries-tab-group');
    if (tabGroup) {
        tabGroup.addEventListener('wa-tab-show', (event) => {
            activeTab = event.detail.name;
            loadEntriesForTab(activeTab);
        });
    }
});

// HTMX events
document.body.addEventListener('htmx:afterSwap', function(evt) {
    // Timer display updates
    if (evt.detail.target.id === 'timer-display') {
        const timerDiv = evt.detail.target.querySelector('[data-running]');
        if (timerDiv) {
            const running = timerDiv.getAttribute('data-running');
            const startTime = timerDiv.getAttribute('data-start-time');
            if (running === 'true' && startTime) {
                startTimerDisplay(startTime);
            } else {
                stopTimerDisplay();
            }
        } else {
            stopTimerDisplay();
        }
    }

    // Update stats when entries load
    if (evt.detail.target.id.startsWith('entries-content')) {
        updateStatsDashboard();
    }
});

// Add auth header to all HTMX requests
document.body.addEventListener('htmx:configRequest', function(evt) {
    const token = getAuthToken();
    if (!token || isTokenExpired(token)) {
        evt.preventDefault();
        showTokenDialog();
        return;
    }
    evt.detail.headers['Authorization'] = token;
});

// Handle auth errors
document.body.addEventListener('htmx:responseError', function(evt) {
    if (evt.detail.xhr.status === 401) {
        showTokenDialog();
    }
});