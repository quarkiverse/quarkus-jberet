import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/progress-bar';
import 'qui-badge';
import { xmlJobs } from 'build-time-data';
import { msg, updateWhenLocaleChanges } from 'localization';
import { statusBadge, formatTime, parametersList, flowDiagram, diagramStyles, onBatchEvent } from './qwc-jberet-common.js';
import './qwc-jberet-parameters-dialog.js';

/**
 * This component shows the batch Jobs and allows to start them.
 */
export class QwcJberetJobs extends QwcHotReloadElement {

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
        .jobs-table {
            height: 100%;
        }
        .name {
            display: flex;
            gap: 6px;
            align-items: center;
        }
        .steps {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
        }
        .secondary {
            color: var(--lumo-secondary-text-color);
            font-size: var(--lumo-font-size-s);
        }
        code {
            font-size: 85%;
        }
        .details {
            padding: 8px 16px 16px 16px;
        }
    `];

    static properties = {
        _jobs: { state: true },
        _filteredJobs: { state: true },
        _searchTerm: { state: true },
        _detailsOpened: { state: true },
        _diagrams: { state: true },
        _startJob: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._jobs = null;
        this._filteredJobs = [];
        this._searchTerm = '';
        this._detailsOpened = [];
        this._diagrams = {};
        this._startJob = null;
        this._eventsObserver = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
        // Refresh the last execution of each job as soon as an execution starts or ends
        this._eventsObserver = onBatchEvent(this.jsonRpc, () => this._load());
    }

    disconnectedCallback() {
        if (this._eventsObserver) {
            this._eventsObserver.cancel();
            this._eventsObserver = null;
        }
        super.disconnectedCallback();
    }

    hotReload() {
        // The jobs may have changed
        this._diagrams = {};
        this._load();
    }

    render() {
        if (!this._jobs) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        return html`
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
            <vaadin-grid
                .items="${this._filteredJobs}"
                .itemIdPath="${'name'}"
                .detailsOpenedItems="${this._detailsOpened}"
                @active-item-changed="${(e) => this._toggleDetails(e.detail.value)}"
                ${gridRowDetailsRenderer((job) => this._renderDetails(job), [this._diagrams])}
                class="jobs-table"
                theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width resizable path="name"
                    header=${msg('Job', { id: 'quarkus-jberet-job' })}
                    ${columnBodyRenderer((job) => this._renderName(job), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Steps', { id: 'quarkus-jberet-steps' })}
                    ${columnBodyRenderer((job) => this._renderSteps(job), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Schedule', { id: 'quarkus-jberet-schedule' })}
                    ${columnBodyRenderer((job) => job.cron ? html`<code>${job.cron}</code>` : '', [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Parameters', { id: 'quarkus-jberet-parameters' })}
                    ${columnBodyRenderer((job) => parametersList(job.parameters), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column width="22em" flex-grow="0" resizable
                    header=${msg('Last execution', { id: 'quarkus-jberet-last-execution' })}
                    ${columnBodyRenderer((job) => this._renderLastExecution(job), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width frozen-to-end
                    ${columnBodyRenderer((job) => this._renderActions(job), [])}>
                </vaadin-grid-column>
            </vaadin-grid>
            <qwc-jberet-parameters-dialog
                .opened="${this._startJob !== null}"
                .headerTitle="${this._startJob ? `${msg('Start job', { id: 'quarkus-jberet-start-job' })} ${this._startJob.name}` : ''}"
                .confirmLabel="${msg('Start', { id: 'quarkus-jberet-start' })}"
                .parameters="${this._startJob ? this._startJob.parameters : {}}"
                @confirm="${(e) => this._start(this._startJob.name, e.detail)}"
                @closed="${() => this._startJob = null}">
            </qwc-jberet-parameters-dialog>
        `;
    }

    _renderName(job) {
        const source = xmlJobs.includes(job.name)
            ? msg('XML', { id: 'quarkus-jberet-source-xml' })
            : msg('CDI bean', { id: 'quarkus-jberet-source-bean' });
        return html`
            <div class="name">
                <strong>${job.name}</strong>
                <qui-badge small pill level="contrast"><span>${source}</span></qui-badge>
            </div>
            ${job.id !== job.name ? html`<div class="secondary">id: <code>${job.id}</code></div>` : ''}`;
    }

    _renderSteps(job) {
        return html`
            <div class="steps">
                ${job.steps.map(step => html`<qui-badge small><span>${step}</span></qui-badge>`)}
            </div>`;
    }

    _renderLastExecution(job) {
        const execution = job.lastExecution;
        if (!execution) {
            return html`<span class="secondary">${msg('Never executed', { id: 'quarkus-jberet-never-executed' })}</span>`;
        }
        return html`
            ${statusBadge(execution.batchStatus)}
            <span class="secondary">#${execution.executionId} ${formatTime(execution.endTime || execution.startTime || execution.createTime)}</span>`;
    }

    _renderDetails(job) {
        const diagram = this._diagrams[job.name];
        if (diagram === undefined) {
            return html`<div class="details"><vaadin-progress-bar indeterminate></vaadin-progress-bar></div>`;
        }
        return html`<div class="details">${flowDiagram(diagram)}</div>`;
    }

    _toggleDetails(job) {
        if (!job) {
            this._detailsOpened = [];
            return;
        }
        this._detailsOpened = [job];
        if (this._diagrams[job.name] === undefined) {
            this.jsonRpc.getJobDiagram({ jobName: job.name }).then(jsonResponse => {
                this._diagrams = { ...this._diagrams, [job.name]: jsonResponse.result };
            });
        }
    }

    _renderActions(job) {
        // Stop clicks from toggling the row details
        return html`
            <vaadin-button theme="small" @click="${(e) => { e.stopPropagation(); this._startJob = job; }}">
                <vaadin-icon icon="font-awesome-solid:play" slot="prefix"></vaadin-icon>
                ${msg('Start', { id: 'quarkus-jberet-start' })}
            </vaadin-button>`;
    }

    _load() {
        this.jsonRpc.getJobs().then(jsonResponse => {
            this._jobs = jsonResponse.result;
            this._filter();
            // Keep the opened details pointing to the refreshed item
            if (this._detailsOpened.length > 0) {
                const opened = this._jobs.find(job => job.name === this._detailsOpened[0].name);
                this._detailsOpened = opened ? [opened] : [];
            }
        });
    }

    _filter() {
        const term = this._searchTerm;
        this._filteredJobs = !term
            ? this._jobs
            : this._jobs.filter(job =>
                job.name.toLowerCase().includes(term)
                || job.id.toLowerCase().includes(term)
                || job.steps.some(step => step.toLowerCase().includes(term)));
    }

    _start(jobName, parameters) {
        this.jsonRpc.startJob({ jobName, parameters }).then(jsonResponse => {
            const result = jsonResponse.result;
            if (result.success) {
                notifier.showSuccessMessage(result.message);
                // Give the execution a moment to start before refreshing the last execution
                setTimeout(() => this._load(), 500);
            } else {
                notifier.showErrorMessage(result.message, 'bottom-stretch');
            }
        });
    }
}

customElements.define('qwc-jberet-jobs', QwcJberetJobs);
