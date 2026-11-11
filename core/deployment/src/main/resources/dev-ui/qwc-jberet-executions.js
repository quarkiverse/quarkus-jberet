import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/progress-bar';
import '@qomponent/qui-alert';
import '@vaadin/dialog';
import { dialogRenderer, dialogFooterRenderer } from '@vaadin/dialog/lit.js';
import 'qui-assistant-button';
import 'qui-ide-link';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { observeState } from 'lit-element-state';
import { assistantState } from 'assistant-state';
import { inMemoryRepository } from 'build-time-data';
import { msg, str, updateWhenLocaleChanges } from 'localization';
import { statusBadge, formatTime, formatDuration, parametersList, flowDiagram, diagramStyles, onBatchEvent } from './qwc-jberet-common.js';
import './qwc-jberet-parameters-dialog.js';

const POLL_INTERVAL = 2000;

const METRICS = [
    { label: 'Read', keys: ['READ_COUNT'] },
    { label: 'Write', keys: ['WRITE_COUNT'] },
    { label: 'Filter', keys: ['FILTER_COUNT'] },
    { label: 'Skip', keys: ['READ_SKIP_COUNT', 'PROCESS_SKIP_COUNT', 'WRITE_SKIP_COUNT'] },
    { label: 'Commit', keys: ['COMMIT_COUNT'] },
    { label: 'Rollback', keys: ['ROLLBACK_COUNT'] }
];

/**
 * This component shows the Job executions, their steps, and allows to stop, restart or abandon them.
 */
export class QwcJberetExecutions extends observeState(QwcHotReloadElement) {

    jsonRpc = new JsonRpc(this);

    static styles = [diagramStyles, css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
        }
        .topBar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 20px;
        }
        .searchField {
            width: 30%;
        }
        .executions-table {
            height: 100%;
        }
        .secondary {
            color: var(--lumo-secondary-text-color);
            font-size: var(--lumo-font-size-s);
        }
        .actions {
            display: flex;
            gap: 4px;
        }
        .details {
            padding: 8px 16px 16px 16px;
        }
        table.steps {
            border-collapse: collapse;
            width: 100%;
        }
        table.steps th, table.steps td {
            text-align: left;
            padding: 4px 12px 4px 0;
            border-bottom: 1px solid var(--lumo-contrast-10pct);
            vertical-align: top;
        }
        table.steps th {
            color: var(--lumo-secondary-text-color);
            font-weight: normal;
            font-size: var(--lumo-font-size-s);
        }
        table.steps td.metric, table.steps th.metric {
            text-align: right;
        }
        pre.exception {
            max-height: 300px;
            overflow: auto;
            margin: 4px 0;
            padding: 8px;
            font-size: var(--lumo-font-size-xs);
            background: var(--lumo-contrast-5pct);
            border-radius: var(--lumo-border-radius-m);
        }
        code {
            font-size: 85%;
        }
        .diagram {
            margin-bottom: 12px;
        }
        .detailsActions {
            display: flex;
            justify-content: flex-end;
            margin-bottom: 8px;
        }
        .explanation {
            max-width: 900px;
        }
    `];

    static properties = {
        _executions: { state: true },
        _filteredExecutions: { state: true },
        _searchTerm: { state: true },
        _detailsOpened: { state: true },
        _steps: { state: true },
        _diagrams: { state: true },
        _restartExecution: { state: true },
        _explaining: { state: true },
        _explanation: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._executions = null;
        this._filteredExecutions = [];
        this._searchTerm = '';
        this._detailsOpened = [];
        this._steps = {};
        this._diagrams = {};
        this._restartExecution = null;
        this._explaining = null;
        this._explanation = null;
        this.md = new MarkdownIt();
        this._pollTimeout = null;
        this._eventsObserver = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
        // Refresh as soon as an execution starts or ends, for instance from a schedule
        this._eventsObserver = onBatchEvent(this.jsonRpc, () => this._load());
    }

    disconnectedCallback() {
        clearTimeout(this._pollTimeout);
        if (this._eventsObserver) {
            this._eventsObserver.cancel();
            this._eventsObserver = null;
        }
        super.disconnectedCallback();
    }

    hotReload() {
        this._load();
    }

    render() {
        if (!this._executions) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        return html`
            ${this._renderInMemoryWarning()}
            <div class="topBar">
                <vaadin-text-field
                    class="searchField"
                    .placeholder=${msg('Search', { id: 'quarkus-jberet-search' })}
                    .value="${this._searchTerm}"
                    clear-button-visible
                    @value-changed="${(e) => {
                        this._searchTerm = (e.detail.value || '').trim().toLowerCase();
                        this._filter();
                    }}">
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-button theme="tertiary" @click="${() => this._load()}">
                    <vaadin-icon icon="font-awesome-solid:arrows-rotate" slot="prefix"></vaadin-icon>
                    ${msg('Refresh', { id: 'quarkus-jberet-refresh' })}
                </vaadin-button>
            </div>
            ${this._renderExecutions()}
            <qwc-jberet-parameters-dialog
                .opened="${this._restartExecution !== null}"
                .headerTitle="${this._restartExecution
                    ? `${msg('Restart execution', { id: 'quarkus-jberet-restart-execution' })} #${this._restartExecution.executionId}`
                    : ''}"
                .description="${msg('The restart parameters replace the parameters of the original execution.', { id: 'quarkus-jberet-restart-parameters-description' })}"
                .confirmLabel="${msg('Restart', { id: 'quarkus-jberet-restart' })}"
                .parameters="${this._restartExecution ? this._restartExecution.parameters : {}}"
                @confirm="${(e) => this._restart(this._restartExecution.executionId, e.detail)}"
                @closed="${() => this._restartExecution = null}">
            </qwc-jberet-parameters-dialog>
            ${this._renderExplanationDialog()}
        `;
    }

    _renderInMemoryWarning() {
        if (inMemoryRepository) {
            return html`
                <qui-alert level="info" permanent>
                    <span>${msg('Executions are stored in the in-memory Job Repository, and are lost when the application restarts or reloads. Configure a persistent repository with quarkus.jberet.repository.type to keep them.', { id: 'quarkus-jberet-in-memory-warning' })}</span>
                </qui-alert>`;
        }
        return html``;
    }

    _renderExecutions() {
        if (this._executions.length === 0) {
            return html`<span class="secondary" style="padding: 0 20px;">${msg('No executions yet. Start a job from the Jobs page.', { id: 'quarkus-jberet-no-executions' })}</span>`;
        }
        return html`
            <vaadin-grid
                .items="${this._filteredExecutions}"
                .itemIdPath="${'executionId'}"
                .detailsOpenedItems="${this._detailsOpened}"
                @active-item-changed="${(e) => this._toggleDetails(e.detail.value)}"
                ${gridRowDetailsRenderer((execution) => this._renderDetails(execution), [this._steps, this._diagrams, this._explaining, assistantState.current])}
                class="executions-table"
                theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width path="executionId" header="#"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width resizable path="jobName"
                    header=${msg('Job', { id: 'quarkus-jberet-job' })}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width path="instanceId"
                    header=${msg('Instance', { id: 'quarkus-jberet-instance' })}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column width="9em" flex-grow="0" path="batchStatus"
                    header=${msg('Status', { id: 'quarkus-jberet-status' })}
                    ${columnBodyRenderer((execution) => statusBadge(execution.batchStatus), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width resizable path="exitStatus"
                    header=${msg('Exit status', { id: 'quarkus-jberet-exit-status' })}>
                </vaadin-grid-column>
                <vaadin-grid-sort-column auto-width path="startTime"
                    header=${msg('Started', { id: 'quarkus-jberet-started' })}
                    ${columnBodyRenderer((execution) => formatTime(execution.startTime || execution.createTime), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width
                    header=${msg('Duration', { id: 'quarkus-jberet-duration' })}
                    ${columnBodyRenderer((execution) => formatDuration(execution.startTime, execution.endTime), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Parameters', { id: 'quarkus-jberet-parameters' })}
                    ${columnBodyRenderer((execution) => parametersList(execution.parameters), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width frozen-to-end
                    ${columnBodyRenderer((execution) => this._renderActions(execution), [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderActions(execution) {
        // Stop clicks from toggling the row details
        return html`
            <div class="actions" @click="${(e) => e.stopPropagation()}">
                ${execution.stoppable ? html`
                    <vaadin-button theme="small error" @click="${() => this._stop(execution.executionId)}">
                        <vaadin-icon icon="font-awesome-solid:stop" slot="prefix"></vaadin-icon>
                        ${msg('Stop', { id: 'quarkus-jberet-stop' })}
                    </vaadin-button>` : ''}
                ${execution.restartable ? html`
                    <vaadin-button theme="small" @click="${() => this._restartExecution = execution}">
                        <vaadin-icon icon="font-awesome-solid:rotate-right" slot="prefix"></vaadin-icon>
                        ${msg('Restart', { id: 'quarkus-jberet-restart' })}
                    </vaadin-button>` : ''}
                ${execution.abandonable ? html`
                    <vaadin-button theme="small tertiary" @click="${() => this._abandon(execution.executionId)}">
                        <vaadin-icon icon="font-awesome-solid:ban" slot="prefix"></vaadin-icon>
                        ${msg('Abandon', { id: 'quarkus-jberet-abandon' })}
                    </vaadin-button>` : ''}
            </div>`;
    }

    _renderDetails(execution) {
        const steps = this._steps[execution.executionId];
        if (!steps) {
            return html`<div class="details"><vaadin-progress-bar indeterminate></vaadin-progress-bar></div>`;
        }
        const diagram = html`${this._renderExplainButton(execution)}${flowDiagram(this._diagrams[execution.executionId])}`;
        if (steps.length === 0) {
            return html`
                <div class="details">
                    ${diagram}
                    <span class="secondary">${msg('No steps executed', { id: 'quarkus-jberet-no-steps' })}</span>
                </div>`;
        }
        return html`
            <div class="details">
                ${diagram}
                <table class="steps">
                    <thead>
                        <tr>
                            <th>${msg('Step', { id: 'quarkus-jberet-step' })}</th>
                            <th>${msg('Status', { id: 'quarkus-jberet-status' })}</th>
                            <th>${msg('Exit status', { id: 'quarkus-jberet-exit-status' })}</th>
                            <th>${msg('Duration', { id: 'quarkus-jberet-duration' })}</th>
                            ${METRICS.map(metric => html`<th class="metric">${msg(metric.label, { id: `quarkus-jberet-metric-${metric.label.toLowerCase()}` })}</th>`)}
                        </tr>
                    </thead>
                    <tbody>
                        ${steps.map(step => html`
                            <tr>
                                <td><strong>${step.stepName}</strong></td>
                                <td>${statusBadge(step.batchStatus)}</td>
                                <td>${step.exitStatus}</td>
                                <td>${formatDuration(step.startTime, step.endTime)}</td>
                                ${METRICS.map(metric => html`<td class="metric">${this._metric(step, metric)}</td>`)}
                            </tr>
                            ${step.exception ? html`
                                <tr>
                                    <td colspan="${4 + METRICS.length}">
                                        <pre class="exception">${this._renderStackTrace(step.exception)}</pre>
                                    </td>
                                </tr>` : ''}
                        `)}
                    </tbody>
                </table>
            </div>`;
    }

    _renderStackTrace(exception) {
        // Lines of the application classes open in the IDE
        return exception.split('\n').map(line => {
            const trimmed = line.trim();
            if (!trimmed.startsWith('at ')) {
                return html`${line}${'\n'}`;
            }
            const indentation = line.substring(0, line.indexOf('at '));
            return html`${indentation}<qui-ide-link stackTraceLine="${trimmed}">${trimmed}</qui-ide-link>${'\n'}`;
        });
    }

    _renderExplainButton(execution) {
        if (execution.batchStatus !== 'FAILED' || !assistantState.current.isConfigured) {
            return html``;
        }
        const explaining = this._explaining === execution.executionId;
        return html`
            <div class="detailsActions">
                <qui-assistant-button
                    ?disabled=${explaining}
                    title=${msg('Use the Quarkus Assistant to explain why the execution failed', { id: 'quarkus-jberet-explain-failure-title' })}
                    @click=${() => this._explainFailure(execution.executionId)}>
                    ${explaining
                        ? msg('Talking to the Dev Assistant ... please wait', { id: 'quarkus-jberet-talking-to-assistant' })
                        : msg('Explain failure', { id: 'quarkus-jberet-explain-failure' })}
                </qui-assistant-button>
            </div>`;
    }

    _renderExplanationDialog() {
        return html`
            <vaadin-dialog
                .headerTitle="${this._explanation ? `${msg('Explain failure', { id: 'quarkus-jberet-explain-failure' })} #${this._explanation.executionId}` : ''}"
                .opened="${this._explanation !== null}"
                @opened-changed="${(e) => { if (!e.detail.value) this._explanation = null; }}"
                ${dialogRenderer(() => html`
                    <div class="explanation">${unsafeHTML(this.md.render(this._explanation?.markdown || ''))}</div>`,
                    [this._explanation])}
                ${dialogFooterRenderer(() => html`
                    <vaadin-button theme="primary" @click="${() => this._explanation = null}">
                        ${msg('Close', { id: 'quarkus-jberet-close' })}
                    </vaadin-button>`, [])}>
            </vaadin-dialog>`;
    }

    _explainFailure(executionId) {
        // qui-assistant-button fires the click event twice (its own, and the native one)
        if (this._explaining) {
            return;
        }
        this._explaining = executionId;
        this.jsonRpc.explainFailure({ executionId }).then(jsonResponse => {
            this._explanation = { executionId, markdown: jsonResponse.result.markdown };
        }).catch(e => {
            const error = e?.error?.message || e;
            notifier.showErrorMessage(msg(str`Failed to explain the failure: ${error}`, { id: 'quarkus-jberet-explain-failure-failed' }));
        }).finally(() => {
            this._explaining = null;
        });
    }

    _metric(step, metric) {
        const metrics = step.metrics || {};
        return metric.keys.reduce((total, key) => total + (metrics[key] || 0), 0);
    }

    _toggleDetails(execution) {
        if (!execution) {
            this._detailsOpened = [];
            return;
        }
        this._detailsOpened = [execution];
        this._loadSteps(execution.executionId);
    }

    _load() {
        clearTimeout(this._pollTimeout);
        this.jsonRpc.getExecutions({ jobName: '' }).then(jsonResponse => {
            this._executions = jsonResponse.result;
            this._filter();

            // Keep the opened details pointing to the refreshed item, and refresh its steps
            if (this._detailsOpened.length > 0) {
                const opened = this._executions.find(e => e.executionId === this._detailsOpened[0].executionId);
                this._detailsOpened = opened ? [opened] : [];
                if (opened) {
                    this._loadSteps(opened.executionId);
                }
            }

            // Poll while there are running executions
            if (this._executions.some(e => e.stoppable || e.batchStatus === 'STOPPING')) {
                this._pollTimeout = setTimeout(() => this._load(), POLL_INTERVAL);
            }
        });
    }

    _loadSteps(executionId) {
        this.jsonRpc.getStepExecutions({ executionId }).then(jsonResponse => {
            this._steps = { ...this._steps, [executionId]: jsonResponse.result };
        });
        this.jsonRpc.getExecutionDiagram({ executionId }).then(jsonResponse => {
            // Only update when the diagram changes, to keep the zoom and position
            if (this._diagrams[executionId] !== jsonResponse.result) {
                this._diagrams = { ...this._diagrams, [executionId]: jsonResponse.result };
            }
        });
    }

    _filter() {
        const term = this._searchTerm;
        this._filteredExecutions = !term
            ? this._executions
            : this._executions.filter(execution =>
                execution.jobName.toLowerCase().includes(term)
                || (execution.batchStatus || '').toLowerCase().includes(term)
                || (execution.exitStatus || '').toLowerCase().includes(term)
                || String(execution.executionId) === term);
    }

    _stop(executionId) {
        this.jsonRpc.stopExecution({ executionId }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _restart(executionId, parameters) {
        this.jsonRpc.restartExecution({ executionId, parameters }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _abandon(executionId) {
        this.jsonRpc.abandonExecution({ executionId }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _handleAction(result) {
        if (result.success) {
            notifier.showSuccessMessage(result.message);
        } else {
            notifier.showErrorMessage(result.message, 'bottom-stretch');
        }
        // Give the operation a moment to take effect before refreshing
        setTimeout(() => this._load(), 500);
    }
}

customElements.define('qwc-jberet-executions', QwcJberetExecutions);
