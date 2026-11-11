import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { LogController } from 'log-controller';
import { msg, updateWhenLocaleChanges } from 'localization';

const METRICS = [
    { label: 'read', keys: ['READ_COUNT'] },
    { label: 'write', keys: ['WRITE_COUNT'] },
    { label: 'filter', keys: ['FILTER_COUNT'] },
    { label: 'skip', keys: ['READ_SKIP_COUNT', 'PROCESS_SKIP_COUNT', 'WRITE_SKIP_COUNT'] },
    { label: 'commit', keys: ['COMMIT_COUNT'] },
    { label: 'rollback', keys: ['ROLLBACK_COUNT'] }
];

/**
 * This component shows a live log of the Job and Step executions.
 */
export class QwcJberetLog extends LitElement {

    jsonRpc = new JsonRpc(this, false);
    logControl = new LogController(this);

    static styles = css`
        .logEntry {
            display: flex;
            gap: 6px;
            align-items: baseline;
        }
        .time {
            color: var(--lumo-secondary-text-color);
        }
        .text-error {
            color: var(--lumo-error-text-color);
        }
        .text-success {
            color: var(--lumo-success-text-color);
        }
        .text-warning {
            color: var(--lumo-warning-text-color, #d98a00);
        }
        .text-info {
            color: var(--lumo-primary-text-color);
        }
        .text-secondary {
            color: var(--lumo-secondary-text-color);
        }
        vaadin-icon {
            width: 0.9em;
            height: 0.9em;
            flex-shrink: 0;
        }
    `;

    static properties = {
        _logEntries: { state: true },
        _zoom: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);

        this.logControl
            .addToggle(msg('On/off switch', { id: 'quarkus-jberet-log-on-off-switch' }), true, (e) => this._toggleOnOff(e))
            .addItem(msg('Zoom out', { id: 'quarkus-jberet-log-zoom-out' }), 'font-awesome-solid:magnifying-glass-minus', 'grey', () => this._zoomBy(-0.05))
            .addItem(msg('Zoom in', { id: 'quarkus-jberet-log-zoom-in' }), 'font-awesome-solid:magnifying-glass-plus', 'grey', () => this._zoomBy(0.05))
            .addItem(msg('Clear', { id: 'quarkus-jberet-log-clear' }), 'font-awesome-solid:trash-can', '#FF004A', () => this._clear())
            .addFollow(msg('Follow log', { id: 'quarkus-jberet-log-follow-log' }), true, (e) => this._toggleFollow(e))
            .done();

        this._logEntries = [];
        this._zoom = 1.0;
        this._followLog = true;
        this._observer = null;
        this._seen = new Set();
        this._clearedAt = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this._connect();
    }

    disconnectedCallback() {
        this._disconnect();
        super.disconnectedCallback();
    }

    _connect() {
        if (this._observer) {
            return;
        }
        this._observer = this.jsonRpc.streamEvents().onNext(jsonRpcResponse => this._addLogEntries([jsonRpcResponse.result]));
        // Events that happened before connecting (or while disconnected)
        this.jsonRpc.getRecentEvents().then(jsonRpcResponse => this._addLogEntries(jsonRpcResponse.result));
    }

    _disconnect() {
        if (this._observer) {
            this._observer.cancel();
            this._observer = null;
        }
    }

    render() {
        return html`${this._logEntries.map(entry => html`
            <code class="log" style="font-size: ${this._zoom}em;">
                <div class="logEntry">${this._renderLogEntry(entry)}</div>
            </code>`)}`;
    }

    _renderLogEntry(entry) {
        const time = new Date(entry.timestamp).toLocaleTimeString();
        const where = entry.stepName
            ? html`<span class="text-info">[${entry.jobName} #${entry.executionId} / ${entry.stepName}]</span>`
            : html`<span class="text-info">[${entry.jobName} #${entry.executionId}]</span>`;
        return html`
            <span class="time">${time}</span>
            ${this._renderIcon(entry)}
            ${this._renderMessage(entry)}
            ${where}
            ${this._renderDetails(entry)}`;
    }

    _renderIcon(entry) {
        if (entry.type.endsWith('_STARTED')) {
            return html`<vaadin-icon class="text-secondary" icon="font-awesome-solid:play"></vaadin-icon>`;
        }
        switch (entry.batchStatus) {
            case 'COMPLETED':
                return html`<vaadin-icon class="text-success" icon="font-awesome-solid:circle-check"></vaadin-icon>`;
            case 'FAILED':
                return html`<vaadin-icon class="text-error" icon="font-awesome-solid:circle-exclamation"></vaadin-icon>`;
            default:
                return html`<vaadin-icon class="text-warning" icon="font-awesome-solid:circle-stop"></vaadin-icon>`;
        }
    }

    _renderMessage(entry) {
        switch (entry.type) {
            case 'JOB_STARTED':
                return html`<span>${msg('Job started', { id: 'quarkus-jberet-log-job-started' })}</span>`;
            case 'STEP_STARTED':
                return html`<span>${msg('Step started', { id: 'quarkus-jberet-log-step-started' })}</span>`;
            case 'JOB_ENDED':
                return html`<span class="${this._statusClass(entry)}">${this._endedMessage('Job', entry.batchStatus)}</span>`;
            default:
                return html`<span class="${this._statusClass(entry)}">${this._endedMessage('Step', entry.batchStatus)}</span>`;
        }
    }

    _endedMessage(element, status) {
        if (element === 'Job') {
            switch (status) {
                case 'COMPLETED': return msg('Job completed', { id: 'quarkus-jberet-log-job-completed' });
                case 'FAILED': return msg('Job failed', { id: 'quarkus-jberet-log-job-failed' });
                default: return msg('Job stopped', { id: 'quarkus-jberet-log-job-stopped' });
            }
        }
        switch (status) {
            case 'COMPLETED': return msg('Step completed', { id: 'quarkus-jberet-log-step-completed' });
            case 'FAILED': return msg('Step failed', { id: 'quarkus-jberet-log-step-failed' });
            default: return msg('Step stopped', { id: 'quarkus-jberet-log-step-stopped' });
        }
    }

    _statusClass(entry) {
        switch (entry.batchStatus) {
            case 'COMPLETED': return 'text-success';
            case 'FAILED': return 'text-error';
            default: return 'text-warning';
        }
    }

    _renderDetails(entry) {
        if (!entry.type.endsWith('_ENDED')) {
            return '';
        }
        const details = [];
        if (entry.exitStatus && entry.exitStatus !== entry.batchStatus) {
            details.push(`exit status: ${entry.exitStatus}`);
        }
        if (entry.metrics) {
            for (const metric of METRICS) {
                const value = metric.keys.reduce((total, key) => total + (entry.metrics[key] || 0), 0);
                if (value > 0) {
                    details.push(`${metric.label}: ${value}`);
                }
            }
        }
        return html`
            ${details.length > 0 ? html`<span class="text-secondary">${details.join(', ')}</span>` : ''}
            ${entry.message ? html`<span class="text-error">${entry.message}</span>` : ''}`;
    }

    _addLogEntries(entries) {
        // The stream and the recent events overlap, and may arrive in any order. Event ids restart when the
        // application reloads, so the key includes the timestamp.
        const added = (entries || []).filter(entry => {
            const key = `${entry.timestamp}-${entry.id}`;
            if (entry.timestamp < this._clearedAt || this._seen.has(key)) {
                return false;
            }
            this._seen.add(key);
            return true;
        });
        if (added.length === 0) {
            return;
        }
        this._logEntries = [...this._logEntries, ...added]
            .sort((a, b) => a.timestamp - b.timestamp || a.id - b.id);
        this._scrollToBottom();
    }

    _clear() {
        this._logEntries = [];
        this._seen = new Set();
        this._clearedAt = Date.now();
    }

    _toggleOnOff(on) {
        if (on) {
            this._connect();
        } else {
            this._disconnect();
        }
    }

    _zoomBy(increment) {
        this._zoom = Math.max(0.5, parseFloat((this._zoom + increment).toFixed(2)));
    }

    _toggleFollow(follow) {
        this._followLog = follow;
        this._scrollToBottom();
    }

    async _scrollToBottom() {
        if (this._followLog) {
            await this.updateComplete;
            const last = Array.from(this.shadowRoot.querySelectorAll('.logEntry')).pop();
            if (last) {
                last.scrollIntoView({ behavior: 'smooth', block: 'end' });
            }
        }
    }
}

customElements.define('qwc-jberet-log', QwcJberetLog);
