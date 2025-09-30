class TokenManager {
    static saveToken(token) {
        try {
            localStorage.setItem('reai_access_token', token);
            this.updateTokenStatus();
        } catch (error) {
            console.error('Failed to save token:', error);
        }
    }

    static getToken() {
        const storedToken = localStorage.getItem('reai_access_token');
        if (storedToken && !this.isTokenExpired(storedToken)) {
            return storedToken;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const urlToken = urlParams.get('access_token');

        if (urlToken && !this.isTokenExpired(urlToken)) {
            this.saveToken(urlToken);
            window.history.replaceState({}, document.title, window.location.pathname);
            return urlToken;
        }

        if (storedToken) {
            this.clearToken();
        }

        return null;
    }

    static clearToken() {
        localStorage.removeItem('reai_access_token');
        this.updateTokenStatus();
    }

    static isTokenExpired(token) {
        if (!token) return true;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expiry = payload.exp * 1000;
            return Date.now() >= expiry;
        } catch (error) {
            console.error('Invalid token format:', error);
            return true;
        }
    }

    static updateTokenStatus() {
        const statusElement = document.getElementById('token-status');
        if (!statusElement) return;

        const token = this.getToken();
        if (token) {
            statusElement.textContent = 'Valid';
            statusElement.variant = 'success';
        } else {
            statusElement.textContent = 'Invalid';
            statusElement.variant = 'danger';
        }
    }
}

class APIClient {
    static async makeRequest(url, options = {}) {
        const token = TokenManager.getToken();

        if (!token) {
            throw new Error('No valid access token available. Please login again.');
        }

        const headers = {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
            ...options.headers
        };

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (response.status === 401) {
            TokenManager.clearToken();
            showNotification('Authentication failed. Please login again.', 'danger');
            throw new Error('Authentication failed');
        }

        return response;
    }

    static async get(url) {
        return this.makeRequest(url, { method: 'GET' });
    }

    static async post(url, data = null) {
        const options = { method: 'POST' };
        if (data) {
            options.body = JSON.stringify(data);
        }
        return this.makeRequest(url, options);
    }
}

let selectedEmployeeId = null;
let selectedEmployeeName = null;
let durationInterval = null;
let currentTimerData = null;
let entriesCache = [];
const SELECTED_EMPLOYEE_KEY = 'reai_selected_employee_id';

document.addEventListener('DOMContentLoaded', function() {
    TokenManager.updateTokenStatus();

    const token = TokenManager.getToken();
    if (!token) {
        showNotification('No access token found. Please login first.', 'danger');
        return;
    }

    loadEmployees();

    setInterval(() => {
        TokenManager.updateTokenStatus();
    }, 30000);
});

async function loadEmployees() {
    try {
        showLoading(true);
        const response = await APIClient.get('/api/employees');

        if (!response.ok) {
            throw new Error(`Failed to load employees: ${response.status}`);
        }

        const employees = await response.json();
        populateEmployeeSelect(employees);
        restoreSelectedEmployee();

    } catch (error) {
        console.error('Error loading employees:', error);
        showNotification(`Failed to load employees: ${error.message}`, 'danger');
    } finally {
        showLoading(false);
    }
}

function populateEmployeeSelect(employees) {
    const employeeSelect = document.getElementById('employee-select');
    employeeSelect.innerHTML = '<option value="">-- Select --</option>';

    if (Array.isArray(employees) && employees.length > 0) {
        employees.forEach(emp => {
            const option = document.createElement('option');
            option.value = emp.id;
            option.textContent = `${emp.name} (${emp.email || 'No email'})`;
            option.dataset.name = emp.name;
            option.dataset.email = emp.email || '';
            option.dataset.department = emp.department || '';
            employeeSelect.appendChild(option);
        });
        showNotification(`Loaded ${employees.length} employees`, 'success');
    } else {
        showNotification('No employees found', 'warning');
    }
}

function restoreSelectedEmployee() {
    try {
        const storedId = localStorage.getItem(SELECTED_EMPLOYEE_KEY);
        if (storedId) {
            const employeeSelect = document.getElementById('employee-select');
            employeeSelect.value = storedId;
            handleEmployeeChange(employeeSelect);
        }
    } catch (error) {
        console.warn('Unable to restore selected employee:', error);
    }
}

function handleEmployeeChange(selectElement) {
    selectedEmployeeId = selectElement.value;

    if (!selectedEmployeeId) {
        hideAllSections();
        return;
    }

    const selectedOption = selectElement.options[selectElement.selectedIndex];

    if (selectedOption) {
        selectedEmployeeName = selectedOption.dataset.name;
        localStorage.setItem(SELECTED_EMPLOYEE_KEY, selectedEmployeeId);

        updateEmployeeDisplay(selectedOption);
        showAllSections();

        loadProjects();
        loadCurrentTimer();
        loadEntries();
    }
}

function hideAllSections() {
    document.getElementById('selected-employee').style.display = 'none';
    document.getElementById('timer-section').style.display = 'none';
    document.getElementById('today-section').style.display = 'none';
    document.getElementById('entries-section').style.display = 'none';
}

function showAllSections() {
    document.getElementById('selected-employee').style.display = 'block';
    document.getElementById('timer-section').style.display = 'block';
    document.getElementById('today-section').style.display = 'block';
    document.getElementById('entries-section').style.display = 'block';
    document.getElementById('sync-btn').disabled = false;
}

function updateEmployeeDisplay(selectedOption) {
    document.getElementById('employee-name').textContent = selectedEmployeeName;
    const metaDetails = [
        `ID: ${selectedEmployeeId}`,
        selectedOption.dataset.email,
        selectedOption.dataset.department
    ].filter(Boolean).join(' · ');
    document.getElementById('employee-meta').textContent = metaDetails;
}

async function loadProjects() {
    try {
        const response = await APIClient.get('/api/employees/project');

        if (!response.ok) {
            console.warn('Failed to load projects');
            return;
        }

        const projects = await response.json();
        const projectSelect = document.getElementById('project-select');
        projectSelect.innerHTML = '<option value="">-- Select --</option>';

        if (Array.isArray(projects) && projects.length > 0) {
            projects.forEach(project => {
                const option = document.createElement('option');
                option.value = project.id;
                option.textContent = project.name;
                option.dataset.projectId = project.id;
                option.dataset.projectName = project.name;
                projectSelect.appendChild(option);
            });
        }

    } catch (error) {
        console.warn('Error loading projects:', error);
    }
}

async function loadCurrentTimer() {
    if (!selectedEmployeeId) return;

    try {
        const response = await APIClient.get(`/api/time/current?employeeId=${selectedEmployeeId}`);

        if (response.status === 404) {
            stopTimerDisplay();
            return;
        }

        if (!response.ok) {
            throw new Error(`Failed to load current timer: ${response.status}`);
        }

        const timer = await response.json();

        if (timer && timer.startTime) {
            currentTimerData = {
                projectId: timer.projectId || timer.project?.id || null,
                projectName: timer.projectName || timer.project?.name || 'Unnamed project',
                startTime: timer.startTime,
                employeeName: selectedEmployeeName || timer.employeeName || ''
            };

            displayRunningTimer(currentTimerData);
        } else {
            stopTimerDisplay();
        }

    } catch (error) {
        console.warn('Unable to load current timer:', error);
        stopTimerDisplay();
    }
}

async function loadEntries() {
    if (!selectedEmployeeId) return;

    try {
        const response = await APIClient.get(`/api/time/entries?employeeId=${selectedEmployeeId}`);

        if (!response.ok) {
            throw new Error(`Failed to load entries: ${response.status}`);
        }

        const entries = await response.json();
        entriesCache = Array.isArray(entries) ? entries : [];

        renderEntries(entriesCache);
        renderTodayEntries(entriesCache);

    } catch (error) {
        console.error('Error loading time entries:', error);
        renderEntries([]);
        renderTodayEntries([]);
    }
}

async function startTimer() {
    if (!selectedEmployeeId) {
        showNotification('Please select an employee first', 'warning');
        return;
    }

    const projectSelect = document.getElementById('project-select');
    const projectId = projectSelect.value;

    if (!projectId) {
        showNotification('Please select a project', 'warning');
        return;
    }

    const selectedOption = projectSelect.options[projectSelect.selectedIndex];
    const projectName = selectedOption?.dataset.projectName || selectedOption?.textContent;

    try {
        showLoading(true);

        const url = `/api/time/start?projectId=${encodeURIComponent(projectId)}&employeeId=${selectedEmployeeId}`;
        const response = await APIClient.post(url);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to start timer: ${response.status} - ${errorText}`);
        }

        const result = await response.json();

        currentTimerData = {
            projectId: projectId,
            projectName: projectName,
            startTime: result.startTime || new Date().toISOString(),
            employeeName: selectedEmployeeName
        };

        displayRunningTimer(currentTimerData);
        projectSelect.value = '';
        showNotification(`Timer started for ${projectName}!`, 'success');
        loadEntries();

    } catch (error) {
        console.error('Error starting timer:', error);
        showNotification(`Failed to start timer: ${error.message}`, 'danger');
    } finally {
        showLoading(false);
    }
}

async function stopTimer() {
    if (!selectedEmployeeId) {
        showNotification('Please select an employee first', 'warning');
        return;
    }

    if (!currentTimerData) {
        showNotification('No active timer found', 'warning');
        return;
    }

    try {
        showLoading(true);

        const projectId = currentTimerData.projectId;
        if (!projectId) {
            showNotification('Unable to determine current project', 'danger');
            return;
        }

        const url = `/api/time/stop?employeeId=${selectedEmployeeId}&projectId=${encodeURIComponent(projectId)}`;
        const response = await APIClient.post(url);

        if (!response.ok) {
            if (response.status === 404) {
                showNotification('No active timer found', 'warning');
                return;
            }
            const errorText = await response.text();
            throw new Error(`Failed to stop timer: ${response.status} - ${errorText}`);
        }

        const entry = await response.json();
        stopTimerDisplay();
        const duration = formatDuration(entry.startTime, entry.endTime);
        showNotification(`Timer stopped! Duration: ${duration}`, 'success');
        loadEntries();

    } catch (error) {
        console.error('Error stopping timer:', error);
        showNotification(`Failed to stop timer: ${error.message}`, 'danger');
    } finally {
        showLoading(false);
    }
}

function displayRunningTimer(timerData) {
    const currentTimerDiv = document.getElementById('current-timer');
    const startTime = new Date(timerData.startTime);

    currentTimerDiv.className = 'timer active';
    currentTimerDiv.innerHTML = `
        <div class="timer-time" id="live-duration">00:00:00</div>
        <div class="timer-project">${timerData.projectName}</div>
        <div class="timer-meta">Started ${formatDateTime(timerData.startTime)} · ${timerData.employeeName}</div>
    `;

    currentTimerData = timerData;
    startLiveDurationUpdate(startTime);
}

function stopTimerDisplay() {
    const currentTimerDiv = document.getElementById('current-timer');

    currentTimerDiv.className = 'timer';
    currentTimerDiv.innerHTML = `
        <div class="empty-icon">
            <wa-icon name="hourglass-half"></wa-icon>
        </div>
        <strong>No active timer</strong>
        <p style="font-size: 0.875rem; color: #64748b; margin-top: 0.5rem;">Select project to start</p>
    `;

    stopLiveDurationUpdate();
    currentTimerData = null;
}

function startLiveDurationUpdate(startTime) {
    stopLiveDurationUpdate();

    durationInterval = setInterval(() => {
        const element = document.getElementById('live-duration');
        if (element) {
            const now = new Date();
            const diff = Math.floor((now - startTime) / 1000);

            const hours = Math.floor(diff / 3600);
            const minutes = Math.floor((diff % 3600) / 60);
            const seconds = diff % 60;

            element.textContent = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        } else {
            stopLiveDurationUpdate();
        }
    }, 1000);
}

function stopLiveDurationUpdate() {
    if (durationInterval) {
        clearInterval(durationInterval);
        durationInterval = null;
    }
}

function renderEntries(entries) {
    const container = document.getElementById('entries-container');
    if (!container) return;

    if (!Array.isArray(entries) || entries.length === 0) {
        container.innerHTML = `
            <div class="empty">
                <wa-icon name="inbox"></wa-icon>
                <h3>No Time Entries</h3>
                <p>Start tracking to see entries</p>
            </div>
        `;
        return;
    }

    const recentEntries = entries.slice(0, 5);
    const totalEntries = entries.length;

    const entriesHtml = recentEntries.map(entry => {
        const projectName = entry.projectName || entry.project?.name || 'Unnamed project';
        const isActive = !entry.endTime;
        const duration = isActive ? formatCurrentDuration(entry.startTime) : formatDuration(entry.startTime, entry.endTime);
        const startDisplay = formatDateTime(entry.startTime);
        const endDisplay = entry.endTime ? formatDateTime(entry.endTime) : 'In progress';

        return `
            <div class="entry ${isActive ? 'active' : ''}">
                <div class="entry-head">
                    <div class="entry-title">${projectName}</div>
                    <wa-badge variant="${isActive ? 'danger' : 'success'}" pill>
                        ${isActive ? 'Running' : 'Done'}
                    </wa-badge>
                </div>
                <div class="entry-grid">
                    <div class="entry-item">
                        <wa-icon name="clock"></wa-icon>
                        <span><span class="entry-label">Duration:</span> ${duration}</span>
                    </div>
                    <div class="entry-item">
                        <wa-icon name="play"></wa-icon>
                        <span><span class="entry-label">Started:</span> ${startDisplay}</span>
                    </div>
                    ${entry.endTime ? `
                    <div class="entry-item">
                        <wa-icon name="stop"></wa-icon>
                        <span><span class="entry-label">Ended:</span> ${endDisplay}</span>
                    </div>
                    ` : ''}
                    <div class="entry-item">
                        <wa-icon name="${entry.billable ? 'dollar-sign' : 'minus-circle'}"></wa-icon>
                        <span><span class="entry-label">Billable:</span> ${entry.billable ? 'Yes' : 'No'}</span>
                    </div>
                    <div class="entry-item">
                        <wa-icon name="${entry.synced ? 'check-circle' : 'exclamation-circle'}"></wa-icon>
                        <span><span class="entry-label">Synced:</span> ${entry.synced ? 'Yes' : 'No'}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    let summaryHtml = '';
    if (totalEntries > 5) {
        summaryHtml = `
            <div style="text-align: center; padding: 1rem; color: var(--text-secondary); font-size: 0.875rem;">
                Showing 5 most recent of ${totalEntries} total entries
            </div>
        `;
    }

    container.innerHTML = entriesHtml + summaryHtml;
}

function renderTodayEntries(entries) {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const endOfDay = new Date(startOfDay);
    endOfDay.setDate(endOfDay.getDate() + 1);

    const todaysEntries = (entries || []).filter(entry => {
        if (!entry.startTime) return false;
        const startTime = new Date(entry.startTime);
        return startTime >= startOfDay && startTime < endOfDay;
    });

    const todayTableWrapper = document.getElementById('today-table-wrapper');
    const todayPlaceholder = document.getElementById('today-placeholder');
    const todayEntriesBody = document.getElementById('today-entries-body');
    const todayTotal = document.getElementById('today-total');
    const todayCount = document.getElementById('today-count');

    if (!todaysEntries.length) {
        todayPlaceholder.style.display = 'block';
        todayTableWrapper.style.display = 'none';
        todayEntriesBody.innerHTML = '';
        todayTotal.textContent = '0h 0m';
        todayCount.textContent = '0';
        return;
    }

    todayPlaceholder.style.display = 'none';
    todayTableWrapper.style.display = 'block';

    const rows = todaysEntries.map(entry => {
        const projectName = entry.projectName || entry.project?.name || 'Unnamed project';
        const isActive = !entry.endTime;
        const duration = isActive ? formatCurrentDuration(entry.startTime) : formatDuration(entry.startTime, entry.endTime);
        const start = formatTime(entry.startTime);
        const end = entry.endTime ? formatTime(entry.endTime) : '—';

        return `
            <tr>
                <td><strong>${projectName}</strong></td>
                <td>${start}</td>
                <td>${end}</td>
                <td><strong>${duration}</strong></td>
                <td><wa-badge variant="${isActive ? 'danger' : 'success'}" pill>${isActive ? 'Running' : 'Done'}</wa-badge></td>
            </tr>
        `;
    }).join('');

    todayEntriesBody.innerHTML = rows;

    const totalMinutes = todaysEntries.reduce((sum, entry) => sum + getDurationMinutes(entry.startTime, entry.endTime), 0);
    todayTotal.textContent = formatMinutes(totalMinutes);
    todayCount.textContent = todaysEntries.length.toString();
}

document.addEventListener('DOMContentLoaded', function() {
    const syncBtn = document.getElementById('sync-btn');
    if (syncBtn) {
        syncBtn.addEventListener('click', async function() {
            if (!selectedEmployeeId) {
                showNotification('Please select an employee first', 'warning');
                return;
            }

            try {
                showLoading(true);

                const url = `/api/time/sync?employeeId=${selectedEmployeeId}`;
                const response = await APIClient.post(url);

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`Failed to sync: ${response.status} - ${errorText}`);
                }

                const result = await response.text();
                showNotification(result, 'success');
                loadEntries();

            } catch (error) {
                console.error('Error syncing:', error);
                showNotification(`Failed to sync: ${error.message}`, 'danger');
            } finally {
                showLoading(false);
            }
        });
    }
});

function logout() {
    TokenManager.clearToken();
    stopTimerDisplay();

    selectedEmployeeId = null;
    selectedEmployeeName = null;
    currentTimerData = null;

    hideAllSections();

    document.getElementById('employee-select').innerHTML = '<option value="">-- Select --</option>';
    document.getElementById('project-select').innerHTML = '<option value="">-- Select --</option>';

    localStorage.removeItem(SELECTED_EMPLOYEE_KEY);
    showNotification('Logged out successfully', 'success');
}

function formatDuration(startTime, endTime) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diff = Math.max(Math.floor((end - start) / 1000 / 60), 0);

    const hours = Math.floor(diff / 60);
    const minutes = diff % 60;

    return `${hours}h ${minutes}m`;
}

function formatCurrentDuration(startTime) {
    if (!startTime) return '00:00:00';

    const start = new Date(startTime);
    const now = new Date();
    const diff = Math.max(now - start, 0);

    const hours = Math.floor(diff / 3600000);
    const minutes = Math.floor((diff % 3600000) / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);

    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

function formatDateTime(dateTimeString) {
    try {
        return new Date(dateTimeString).toLocaleString();
    } catch (error) {
        return 'Invalid date';
    }
}

function formatTime(dateTimeString) {
    try {
        return new Date(dateTimeString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (error) {
        return '—';
    }
}

function getDurationMinutes(startTime, endTime) {
    const start = new Date(startTime);
    const end = endTime ? new Date(endTime) : new Date();
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
        return 0;
    }

    const diff = Math.max(end - start, 0);
    return Math.round(diff / 60000);
}

function formatMinutes(totalMinutes) {
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return `${hours}h ${minutes}m`;
}

function showLoading(show) {
    const loadingDiv = document.getElementById('loading');
    if (loadingDiv) {
        loadingDiv.className = show ? 'loading show' : 'loading';
    }
}

function showNotification(message, variant = 'success') {
    const notification = document.getElementById('notification');
    if (!notification) return;

    notification.variant = variant;
    notification.textContent = message;
    notification.style.display = 'block';

    setTimeout(() => {
        notification.style.display = 'none';
    }, 4000);
}

window.addEventListener('beforeunload', function() {
    stopLiveDurationUpdate();
});

window.addEventListener('error', function(event) {
    console.error('Global error:', event.error);
    showNotification('An unexpected error occurred', 'danger');
});

window.addEventListener('unhandledrejection', function(event) {
    console.error('Unhandled promise rejection:', event.reason);
    showNotification('An unexpected error occurred', 'danger');
});