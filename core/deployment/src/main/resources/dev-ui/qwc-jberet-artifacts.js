import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/text-field';
import '@vaadin/progress-bar';
import '@qomponent/qui-alert';
import 'qui-badge';
import 'qui-ide-link';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the batch artifacts used by the Jobs, and the ones available to use.
 */
export class QwcJberetArtifacts extends QwcHotReloadElement {

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
            gap: 20px;
            align-items: center;
            padding: 0 20px;
        }
        .searchField {
            width: 30%;
        }
        .spacer {
            flex-grow: 1;
        }
        .artifacts-table {
            height: 100%;
        }
        .ref {
            display: flex;
            gap: 6px;
            align-items: center;
            flex-wrap: wrap;
        }
        .badges {
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
    `;

    static properties = {
        _artifacts: { state: true },
        _filteredArtifacts: { state: true },
        _searchTerm: { state: true },
        _showUnused: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._artifacts = null;
        this._filteredArtifacts = [];
        this._searchTerm = '';
        this._showUnused = true;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    hotReload() {
        this._load();
    }

    render() {
        if (!this._artifacts) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        return html`
            ${this._renderUnresolvedWarning()}
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
                <vaadin-checkbox
                    .label=${msg('Show artifacts not used by any job', { id: 'quarkus-jberet-show-unused' })}
                    .checked="${this._showUnused}"
                    @checked-changed="${(e) => {
                        this._showUnused = e.detail.value;
                        this._filter();
                    }}">
                </vaadin-checkbox>
                <span class="spacer"></span>
                <vaadin-button theme="tertiary" @click="${() => this._load()}">
                    <vaadin-icon icon="font-awesome-solid:arrows-rotate" slot="prefix"></vaadin-icon>
                    ${msg('Refresh', { id: 'quarkus-jberet-refresh' })}
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._filteredArtifacts}" class="artifacts-table" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width resizable path="ref"
                    header=${msg('Ref', { id: 'quarkus-jberet-ref' })}
                    ${columnBodyRenderer((artifact) => this._renderRef(artifact), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column width="12em" flex-grow="0" resizable
                    header=${msg('Type', { id: 'quarkus-jberet-type' })}
                    ${columnBodyRenderer((artifact) => this._renderTypes(artifact), [])}>
                </vaadin-grid-column>
                <vaadin-grid-sort-column width="32em" resizable path="className"
                    header=${msg('Class', { id: 'quarkus-jberet-class' })}
                    ${columnBodyRenderer((artifact) => this._renderClass(artifact), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column auto-width resizable
                    header=${msg('Used by', { id: 'quarkus-jberet-used-by' })}
                    ${columnBodyRenderer((artifact) => this._renderUsages(artifact), [])}>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _renderUnresolvedWarning() {
        const unresolved = this._artifacts.filter(artifact => artifact.status === 'UNRESOLVED');
        if (unresolved.length === 0) {
            return html``;
        }
        const refs = unresolved.map(artifact => artifact.ref).join(', ');
        return html`
            <qui-alert level="error" permanent>
                <span>${msg(str`These refs do not match any bean: ${refs}. Jobs that use them fail when they run. Check the ref in the job, or add @Named to the artifact class.`, { id: 'quarkus-jberet-unresolved-warning' })}</span>
            </qui-alert>`;
    }

    _renderRef(artifact) {
        return html`
            <div class="ref">
                <code><strong>${artifact.ref}</strong></code>
                ${artifact.builtIn ? html`<qui-badge small pill level="contrast"><span>${msg('Built-in', { id: 'quarkus-jberet-built-in' })}</span></qui-badge>` : ''}
                ${this._renderStatus(artifact)}
            </div>`;
    }

    _renderStatus(artifact) {
        switch (artifact.status) {
            case 'UNRESOLVED':
                return html`<qui-badge small level="error"><span>${msg('Unresolved', { id: 'quarkus-jberet-unresolved' })}</span></qui-badge>`;
            case 'EXPRESSION':
                return html`<qui-badge small level="warning" title=${msg('Resolved when the job runs', { id: 'quarkus-jberet-expression-title' })}><span>${msg('Expression', { id: 'quarkus-jberet-expression' })}</span></qui-badge>`;
            default:
                return artifact.usages.length === 0
                    ? html`<qui-badge small><span>${msg('Not used', { id: 'quarkus-jberet-not-used' })}</span></qui-badge>`
                    : '';
        }
    }

    _renderTypes(artifact) {
        return html`
            <div class="badges">
                ${artifact.types.map(type => html`<qui-badge small><span>${type}</span></qui-badge>`)}
            </div>`;
    }

    _renderClass(artifact) {
        if (!artifact.className) {
            return html``;
        }
        return html`<qui-ide-link fileName="${artifact.className}"><code>${artifact.className}</code></qui-ide-link>`;
    }

    _renderUsages(artifact) {
        return html`${artifact.usages.map(usage => html`
            <div>
                <strong>${usage.jobName}</strong>${usage.element ? html` / ${usage.element}` : ''}
                <span class="secondary">${usage.role}</span>
            </div>`)}`;
    }

    _load() {
        this.jsonRpc.getBatchArtifacts().then(jsonResponse => {
            this._artifacts = jsonResponse.result;
            this._filter();
        });
    }

    _filter() {
        const term = this._searchTerm;
        this._filteredArtifacts = this._artifacts.filter(artifact =>
            (this._showUnused || artifact.usages.length > 0)
            && (!term
                || artifact.ref.toLowerCase().includes(term)
                || (artifact.className || '').toLowerCase().includes(term)
                || artifact.types.some(type => type.toLowerCase().includes(term))
                || artifact.usages.some(usage => usage.jobName.toLowerCase().includes(term)
                    || (usage.element || '').toLowerCase().includes(term))));
    }
}

customElements.define('qwc-jberet-artifacts', QwcJberetArtifacts);
