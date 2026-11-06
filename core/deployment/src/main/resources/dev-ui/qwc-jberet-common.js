import { html, css } from 'lit';
import 'qui-badge';
import '@qomponent/qui-dot';

const STATUS_LEVELS = {
    COMPLETED: 'success',
    FAILED: 'error',
    STOPPED: 'warning',
    STOPPING: 'warning',
    ABANDONED: 'contrast',
    STARTING: 'default',
    STARTED: 'default'
};

/**
 * Renders a Batch Status as a badge.
 */
export function statusBadge(status) {
    if (!status) {
        return html``;
    }
    return html`<qui-badge small level="${STATUS_LEVELS[status] || 'default'}"><span>${status}</span></qui-badge>`;
}

/**
 * Formats epoch millis as a local date time.
 */
export function formatTime(millis) {
    return millis ? new Date(millis).toLocaleString() : '';
}

/**
 * Formats the elapsed time between two epoch millis. If there is no end, the elapsed time is until now.
 */
export function formatDuration(start, end) {
    if (!start) {
        return '';
    }
    let seconds = Math.max(0, Math.round(((end || Date.now()) - start) / 1000));
    const hours = Math.floor(seconds / 3600);
    seconds -= hours * 3600;
    const minutes = Math.floor(seconds / 60);
    seconds -= minutes * 60;
    if (hours > 0) {
        return `${hours}h ${minutes}m ${seconds}s`;
    }
    if (minutes > 0) {
        return `${minutes}m ${seconds}s`;
    }
    return `${seconds}s`;
}

/**
 * Renders key / value parameters as a compact list.
 */
export function parametersList(parameters) {
    const entries = Object.entries(parameters || {});
    if (entries.length === 0) {
        return html``;
    }
    return html`${entries.map(([key, value]) => html`<div><code>${key}=${value}</code></div>`)}`;
}

/**
 * Styles for a Job flow diagram.
 */
export const diagramStyles = css`
    .diagram {
        max-height: 400px;
        overflow: auto;
        border: 1px solid var(--lumo-contrast-10pct);
        border-radius: var(--lumo-border-radius-m);
        padding: 8px;
    }
`;

/**
 * Renders a Job flow diagram in Graphviz DOT format.
 */
export function flowDiagram(dot) {
    if (!dot) {
        return html``;
    }
    return html`<div class="diagram"><qui-dot dot="${dot}"></qui-dot></div>`;
}

/**
 * Calls the callback (at most every delay millis) when a Job or Step execution starts or ends. Returns the stream
 * observer, to cancel when no longer required.
 */
export function onBatchEvent(jsonRpc, callback, delay = 300) {
    let timeout = null;
    return jsonRpc.streamEvents().onNext(() => {
        if (!timeout) {
            timeout = setTimeout(() => {
                timeout = null;
                callback();
            }, delay);
        }
    });
}
