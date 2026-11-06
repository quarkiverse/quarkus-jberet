import { LitElement, html } from 'lit';
import { dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/dialog';
import '@vaadin/button';
import '@vaadin/text-field';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * A dialog to edit Job parameters as key / value pairs, used to start or restart a Job.
 *
 * Fires a <code>confirm</code> event with the parameters as <code>detail</code>, and a <code>closed</code> event when
 * the dialog closes.
 */
export class QwcJberetParametersDialog extends LitElement {

    static properties = {
        opened: { type: Boolean },
        headerTitle: { type: String },
        confirmLabel: { type: String },
        description: { type: String },
        parameters: { type: Object },
        _rows: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this.opened = false;
        this.parameters = {};
        this._rows = [];
    }

    willUpdate(changed) {
        if (changed.has('opened') && this.opened) {
            this._rows = Object.entries(this.parameters || {}).map(([key, value]) => ({ key, value }));
        }
    }

    render() {
        return html`
            <vaadin-dialog
                .headerTitle="${this.headerTitle}"
                .opened="${this.opened}"
                @opened-changed="${(e) => {
                    if (!e.detail.value && this.opened) {
                        this._close();
                    }
                }}"
                ${dialogRenderer(() => this._renderParameters(), [this._rows, this.description])}
                ${dialogFooterRenderer(() => this._renderFooter(), [this.confirmLabel])}>
            </vaadin-dialog>`;
    }

    _renderParameters() {
        return html`
            <div style="display: flex; flex-direction: column; gap: 4px; min-width: 520px;">
                ${this.description ? html`<span style="color: var(--lumo-secondary-text-color);">${this.description}</span>` : ''}
                ${this._rows.length === 0
                    ? html`<span style="color: var(--lumo-secondary-text-color);">${msg('No parameters', { id: 'quarkus-jberet-no-parameters' })}</span>`
                    : this._rows.map((row, index) => html`
                        <div style="display: flex; gap: 8px; align-items: baseline;">
                            <vaadin-text-field
                                style="flex: 1;"
                                .placeholder=${msg('Name', { id: 'quarkus-jberet-parameter-name' })}
                                .value="${row.key}"
                                @value-changed="${(e) => this._updateRow(index, 'key', e.detail.value)}">
                            </vaadin-text-field>
                            <vaadin-text-field
                                style="flex: 2;"
                                .placeholder=${msg('Value', { id: 'quarkus-jberet-parameter-value' })}
                                .value="${row.value}"
                                @value-changed="${(e) => this._updateRow(index, 'value', e.detail.value)}">
                            </vaadin-text-field>
                            <vaadin-button theme="tertiary icon error"
                                title=${msg('Remove parameter', { id: 'quarkus-jberet-remove-parameter' })}
                                @click="${() => this._removeRow(index)}">
                                <vaadin-icon icon="font-awesome-solid:trash"></vaadin-icon>
                            </vaadin-button>
                        </div>`)}
                <vaadin-button theme="tertiary small" style="align-self: flex-start;" @click="${() => this._addRow()}">
                    <vaadin-icon icon="font-awesome-solid:plus" slot="prefix"></vaadin-icon>
                    ${msg('Add parameter', { id: 'quarkus-jberet-add-parameter' })}
                </vaadin-button>
            </div>`;
    }

    _renderFooter() {
        return html`
            <vaadin-button theme="tertiary" @click="${() => this._close()}">
                ${msg('Cancel', { id: 'quarkus-jberet-cancel' })}
            </vaadin-button>
            <vaadin-button theme="primary" @click="${() => this._confirm()}">
                ${this.confirmLabel}
            </vaadin-button>`;
    }

    _updateRow(index, field, value) {
        // Mutate in place: the text field already shows the value, a new array would re-render and lose focus
        if (this._rows[index]) {
            this._rows[index][field] = value;
        }
    }

    _addRow() {
        this._rows = [...this._rows, { key: '', value: '' }];
    }

    _removeRow(index) {
        this._rows = this._rows.filter((_, i) => i !== index);
    }

    _confirm() {
        const parameters = {};
        for (const row of this._rows) {
            const key = (row.key || '').trim();
            if (key) {
                parameters[key] = row.value ?? '';
            }
        }
        this.dispatchEvent(new CustomEvent('confirm', { detail: parameters }));
        this._close();
    }

    _close() {
        this.opened = false;
        this.dispatchEvent(new CustomEvent('closed'));
    }
}

customElements.define('qwc-jberet-parameters-dialog', QwcJberetParametersDialog);
