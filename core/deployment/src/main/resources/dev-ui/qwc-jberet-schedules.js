import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';
import '@vaadin/progress-bar';
import '@qomponent/qui-alert';
import 'qui-badge';
import { msg, updateWhenLocaleChanges } from 'localization';
import { formatTime, parametersList, onBatchEvent } from './qwc-jberet-common.js';

/**
 * This component shows the Job schedules, and allows to pause, resume or cancel them.
 */
export class QwcJberetSchedules extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
        }
        .topBar {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            padding: 0 20px;
        }
        .schedules-table {
            height: 100%;
        }
        .secondary {
            color: var(--lumo-secondary-text-color);
            font-size: var(--lumo-font-size-s);
        }
        .empty {
            padding: 0 20px;
        }
        .actions {
            display: flex;
            gap: 4px;
        }
        code {
            font-size: 85%;
        }
    `;

    static properties = {
        _schedules: { state: true },
        _schedulerStarted: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._schedules = null;
        this._schedulerStarted = true;
        this._eventsObserver = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
        // Refresh the fire times and executions when a scheduled execution starts
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
        this._load();
    }

    render() {
        if (!this._schedules) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        if (!this._schedulerStarted) {
            return html`
                <qui-alert level="warning" permanent>
                    <span>${msg('The Quarkus Scheduler is disabled, so no jobs are scheduled. Enable it with quarkus.scheduler.enabled.', { id: 'quarkus-jberet-scheduler-disabled' })}</span>
                </qui-alert>`;
        }
        return html`
            <div class="topBar">
                <vaadin-button theme="tertiary" @click="${() => this._load()}">
                    <vaadin-icon icon="font-awesome-solid:arrows-rotate" slot="prefix"></vaadin-icon>
                    ${msg('Refresh', { id: 'quarkus-jberet-refresh' })}
                </vaadin-button>
            </div>
            ${this._schedules.length === 0
                ? html`<span class="secondary empty">${msg('No jobs are scheduled. Schedule a job with quarkus.jberet.job."job-name".cron, or with the JobScheduler.', { id: 'quarkus-jberet-no-schedules' })}</span>`
                : this._renderSchedules()}
        `;
    }

    _renderSchedules() {
        return html`
            <vaadin-grid .items="${this._schedules}" class="schedules-table" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width resizable path="jobName"
                    header=${msg('Job', { id: 'quarkus-jberet-job' })}
                    ${columnBodyRenderer((schedule) => this._renderJob(schedule), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Schedule', { id: 'quarkus-jberet-schedule' })}
                    ${columnBodyRenderer((schedule) => this._renderSchedule(schedule), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column width="8em" flex-grow="0"
                    header=${msg('Status', { id: 'quarkus-jberet-status' })}
                    ${columnBodyRenderer((schedule) => this._renderStatus(schedule), [])}>
                </vaadin-grid-column>
                <vaadin-grid-sort-column auto-width path="nextFireTime"
                    header=${msg('Next fire time', { id: 'quarkus-jberet-next-fire-time' })}
                    ${columnBodyRenderer((schedule) => schedule.paused ? '' : formatTime(schedule.nextFireTime), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width
                    header=${msg('Previous fire time', { id: 'quarkus-jberet-previous-fire-time' })}
                    ${columnBodyRenderer((schedule) => this._renderPreviousFireTime(schedule), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Parameters', { id: 'quarkus-jberet-parameters' })}
                    ${columnBodyRenderer((schedule) => parametersList(schedule.parameters), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width
                    header=${msg('Executions', { id: 'quarkus-jberet-executions' })}
                    ${columnBodyRenderer((schedule) => this._renderExecutions(schedule), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width frozen-to-end
                    ${columnBodyRenderer((schedule) => this._renderActions(schedule), [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderJob(schedule) {
        return html`
            <strong>${schedule.jobName}</strong>
            <div class="secondary"><code>${schedule.id}</code></div>`;
    }

    _renderSchedule(schedule) {
        if (schedule.cron) {
            return html`<code>${schedule.cron}</code>`;
        }
        if (schedule.intervalMinutes > 0) {
            return html`${msg('Every', { id: 'quarkus-jberet-every' })} ${schedule.intervalMinutes} min`;
        }
        return html`${msg('Once', { id: 'quarkus-jberet-once' })}`;
    }

    _renderPreviousFireTime(schedule) {
        // A cron trigger reports its start time as the previous fire time until it fires
        return (schedule.executionIds || []).length > 0 ? formatTime(schedule.previousFireTime) : '';
    }

    _renderStatus(schedule) {
        return schedule.paused
            ? html`<qui-badge small level="warning"><span>${msg('Paused', { id: 'quarkus-jberet-paused' })}</span></qui-badge>`
            : html`<qui-badge small level="success"><span>${msg('Active', { id: 'quarkus-jberet-active' })}</span></qui-badge>`;
    }

    _renderExecutions(schedule) {
        const ids = schedule.executionIds || [];
        if (ids.length === 0) {
            return html`<span class="secondary">${msg('None yet', { id: 'quarkus-jberet-none-yet' })}</span>`;
        }
        // Show the most recent executions
        const recent = ids.slice(-5).reverse();
        return html`${recent.map(id => html`<code>#${id}</code> `)}${ids.length > recent.length ? html`<span class="secondary">(${ids.length})</span>` : ''}`;
    }

    _renderActions(schedule) {
        return html`
            <div class="actions">
                ${schedule.paused ? html`
                    <vaadin-button theme="small" @click="${() => this._resume(schedule.id)}">
                        <vaadin-icon icon="font-awesome-solid:play" slot="prefix"></vaadin-icon>
                        ${msg('Resume', { id: 'quarkus-jberet-resume' })}
                    </vaadin-button>` : html`
                    <vaadin-button theme="small" @click="${() => this._pause(schedule.id)}">
                        <vaadin-icon icon="font-awesome-solid:pause" slot="prefix"></vaadin-icon>
                        ${msg('Pause', { id: 'quarkus-jberet-pause' })}
                    </vaadin-button>`}
                <vaadin-button theme="small error tertiary" @click="${() => this._cancel(schedule.id)}">
                    <vaadin-icon icon="font-awesome-solid:ban" slot="prefix"></vaadin-icon>
                    ${msg('Cancel', { id: 'quarkus-jberet-cancel' })}
                </vaadin-button>
            </div>`;
    }

    _load() {
        this.jsonRpc.getSchedules().then(jsonResponse => {
            this._schedulerStarted = jsonResponse.result.schedulerStarted;
            this._schedules = jsonResponse.result.schedules;
        });
    }

    _pause(scheduleId) {
        this.jsonRpc.pauseSchedule({ scheduleId }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _resume(scheduleId) {
        this.jsonRpc.resumeSchedule({ scheduleId }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _cancel(scheduleId) {
        this.jsonRpc.cancelSchedule({ scheduleId }).then(jsonResponse => this._handleAction(jsonResponse.result));
    }

    _handleAction(result) {
        if (result.success) {
            notifier.showSuccessMessage(result.message);
        } else {
            notifier.showErrorMessage(result.message, 'bottom-stretch');
        }
        this._load();
    }
}

customElements.define('qwc-jberet-schedules', QwcJberetSchedules);
