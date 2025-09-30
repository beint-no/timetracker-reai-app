let selectedEmployeeId = null;
let selectedEmployeeName = null;
let durationInterval = null;
let refreshInterval = null;

const employeeSelect = document.getElementById('employee-select');
const selectedEmployeeDiv = document.getElementById('selected-employee');
const timerSection = document.getElementById('timer-section');
const entriesSection = document.getElementById('entries-section');
const currentTimerDiv = document.getElementById('current-timer');
const projectNameInput = document.getElementById('project-name');
const entriesDiv = document.getElementById('entries');
const loadingDiv = document.getElementById('loading');

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    loadEmployees();

    // Add enter key handler for project input
    projectNameInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            startTimer();
        }
    });
});

// Load employees from API
async function loadEmployees() {
    try {
        showLoading(true);
        const response = await fetch('/api/employees');

        if (!response.ok) {
            throw new Error('Failed to load employees');
        }

        const employees = await response.json();

        // Clear existing options
        employeeSelect.innerHTML = '<option value="">-- Select Employee --</option>';

        // Add employee options
        employees.forEach(emp => {
            const option = document.createElement('option');
            option.value = emp.id;
            option.textContent = emp.displayName || `${emp.name} (${emp.email})`;
            employeeSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Error loading employees:', error);
        showError('Failed to load employees. Please refresh the page.');
    } finally {
        showLoading(false);
    }
}

// Handle employee selection
function selectEmployee() {
    selectedEmployeeId = employeeSelect.value;
    selectedEmployeeName = employeeSelect.options[employeeSelect.selectedIndex].text;

    if (selectedEmployeeId) {
        selectedEmployeeDiv.innerHTML = `<strong>Tracking time for:</strong> ${selectedEmployeeName}`;
        timerSection.style.display = 'block';
        entriesSection.style.display = 'block';

        // Load current timer and entries
        loadCurrentTimer();
        loadEntries();

        // Start auto-refresh
        startAutoRefresh();

    } else {
        selectedEmployeeDiv.innerHTML = '';
        timerSection.style.display = 'none';
        entriesSection.style.display = 'none';

        // Stop auto-refresh
        stopAutoRefresh();
    }
}

// Start timer for project
async function startTimer() {
    if (!selectedEmployeeId) {
        showError('Please select an employee first');
        return;
    }

    const project = projectNameInput.value.trim();
    if (!project) {
        showError('Please enter a project name');
        projectNameInput.focus();
        return;
    }

    try {
        const response = await fetch(`/api/time/start?projectName=${encodeURIComponent(project)}&employeeId=${selectedEmployeeId}`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error('Failed to start timer');
        }

        const entry = await response.json();
        console.log('Timer started:', entry);

        projectNameInput.value = '';
        loadCurrentTimer();
        loadEntries();

        showSuccess('Timer started successfully!');

    } catch (error) {
        console.error('Error starting timer:', error);
        showError('Failed to start timer. Please try again.');
    }
}

// Stop current timer
async function stopTimer() {
    if (!selectedEmployeeId) return;

    try {
        const response = await fetch(`/api/time/stop?employeeId=${selectedEmployeeId}`, {
            method: 'POST'
        });

        if (!response.ok) {
            if (response.status === 404) {
                showError('No active timer found');
                return;
            }
            throw new Error('Failed to stop timer');
        }

        const entry = await response.json();
        console.log('Timer stopped:', entry);

        loadCurrentTimer();
        loadEntries();

        showSuccess(`Timer stopped! Duration: ${formatDuration(entry.startTime, entry.endTime)}`);

    } catch (error) {
        console.error('Error stopping timer:', error);
        showError('Failed to stop timer. Please try again.');
    }
}

// Load current active timer
async function loadCurrentTimer() {
    if (!selectedEmployeeId) return;

    try {
        const response = await fetch(`/api/time/current?employeeId=${selectedEmployeeId}`);

        if (response.ok) {
            const timer = await response.json();
            displayCurrentTimer(timer);
        } else if (response.status === 404) {
            displayCurrentTimer(null);
        } else {
            throw new Error('Failed to load current timer');
        }

    } catch (error) {
        console.error('Error loading current timer:', error);
        displayCurrentTimer(null);
    }
}

// Display current timer in UI
function displayCurrentTimer(timer) {
    if (timer) {
        timerSection.classList.add('active');
        currentTimerDiv.classList.add('active');

        const duration = formatCurrentDuration(timer.startTime);
        currentTimerDiv.innerHTML = `
            <div><strong>Project:</strong> ${timer.projectName}</div>
            <div><strong>Started:</strong> ${formatDateTime(timer.startTime)}</div>
            <div class="live-timer" id="live-duration">${duration}</div>
        `;

        startLiveDurationUpdate(timer.startTime);
    } else {
        timerSection.classList.remove('active');
        currentTimerDiv.classList.remove('active');
        currentTimerDiv.innerHTML = 'No active timer';
        stopLiveDurationUpdate();
    }
}

// Load time entries
async function loadEntries() {
    if (!selectedEmployeeId) return;

    try {
        const response = await fetch(`/api/time/entries?employeeId=${selectedEmployeeId}`);

        if (!response.ok) {
            throw new Error('Failed to load entries');
        }

        const entries = await response.json();
        displayEntries(entries);

    } catch (error) {
        console.error('Error loading entries:', error);
        entriesDiv.innerHTML = '<div class="error">Failed to load time entries</div>';
    }
}

// Display entries in UI
function displayEntries(entries) {
    if (entries.length === 0) {
        entriesDiv.innerHTML = '<div style="text-align: center; color: #6c757d; padding: 20px;">No time entries found</div>';
        return;
    }

    const entriesHtml = entries.map(entry => {
        const isActive = !entry.endTime;
        const duration = isActive ?
            formatCurrentDuration(entry.startTime) :
            formatDuration(entry.startTime, entry.endTime);

        return `
            <div class="entry ${isActive ? 'active' : ''}">
                <div class="entry-header">
                    <div class="project-name">${entry.projectName}</div>
                    <div class="entry-status ${isActive ? 'status-running' : 'status-completed'}">
                        ${isActive ? 'Running' : 'Completed'}
                    </div>
                </div>
                <div class="entry-meta">
                    <div><strong>Duration:</strong> <span class="duration">${duration}</span></div>
                    <div><strong>Started:</strong> ${formatDateTime(entry.startTime)}</div>
                    ${entry.endTime ? `<div><strong>Ended:</strong> ${formatDateTime(entry.endTime)}</div>` : ''}
                    ${entry.description ? `<div><strong>Description:</strong> ${entry.description}</div>` : ''}
                    <div><strong>Billable:</strong> ${entry.billable ? 'Yes' : 'No'}</div>
                    <div><strong>Synced:</strong> ${entry.synced ? 'Yes' : 'No'}</div>
                </div>
            </div>
        `;
    }).join('');

    entriesDiv.innerHTML = entriesHtml;
}

// Sync entries to ReAI
async function syncToReai() {
    try {
        showLoading(true);

        const response = await fetch('/api/time/sync', {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error('Failed to sync entries');
        }

        const result = await response.text();
        showSuccess(result);

        // Reload entries to show updated sync status
        loadEntries();

    } catch (error) {
        console.error('Error syncing entries:', error);
        showError('Failed to sync entries to ReAI');
    } finally {
        showLoading(false);
    }
}

// Live duration update
function startLiveDurationUpdate(startTime) {
    stopLiveDurationUpdate();

    durationInterval = setInterval(() => {
        const element = document.getElementById('live-duration');
        if (element) {
            element.textContent = formatCurrentDuration(startTime);
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

// Auto-refresh functionality
function startAutoRefresh() {
    stopAutoRefresh();

    // Refresh current timer every 30 seconds
    refreshInterval = setInterval(() => {
        loadCurrentTimer();
    }, 30000);
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Utility functions
function formatCurrentDuration(startTime) {
    const start = new Date(startTime);
    const now = new Date();
    const diff = Math.floor((now - start) / 1000); // seconds

    const hours = Math.floor(diff / 3600);
    const minutes = Math.floor((diff % 3600) / 60);
    const seconds = diff % 60;

    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

function formatDuration(startTime, endTime) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diff = Math.floor((end - start) / 1000 / 60); // minutes

    const hours = Math.floor(diff / 60);
    const minutes = diff % 60;

    return `${hours}h ${minutes}m`;
}

function formatDateTime(dateTimeString) {
    return new Date(dateTimeString).toLocaleString();
}

function showLoading(show) {
    loadingDiv.style.display = show ? 'block' : 'none';
}

function showError(message) {
    // Remove existing messages
    removeMessages();

    const errorDiv = document.createElement('div');
    errorDiv.className = 'error';
    errorDiv.textContent = message;

    document.querySelector('.container').insertBefore(errorDiv, document.querySelector('header').nextSibling);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (errorDiv.parentNode) {
            errorDiv.parentNode.removeChild(errorDiv);
        }
    }, 5000);
}

function showSuccess(message) {
    // Remove existing messages
    removeMessages();

    const successDiv = document.createElement('div');
    successDiv.className = 'success';
    successDiv.textContent = message;

    document.querySelector('.container').insertBefore(successDiv, document.querySelector('header').nextSibling);

    // Auto-remove after 3 seconds
    setTimeout(() => {
        if (successDiv.parentNode) {
            successDiv.parentNode.removeChild(successDiv);
        }
    }, 3000);
}

function removeMessages() {
    const messages = document.querySelectorAll('.error, .success');
    messages.forEach(msg => {
        if (msg.parentNode) {
            msg.parentNode.removeChild(msg);
        }
    });
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    stopLiveDurationUpdate();
    stopAutoRefresh();
});
