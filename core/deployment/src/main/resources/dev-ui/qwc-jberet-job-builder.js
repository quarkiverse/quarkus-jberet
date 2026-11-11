import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import '@vaadin/text-area';
import '@vaadin/button';
import '@vaadin/progress-bar';
import 'qui-assistant-button';
import 'qui-themed-code-block';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component uses the Dev Assistant to generate a batch Job from a description.
 */
export class QwcJberetJobBuilder extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            padding: 0 10px;
        }
        .input {
            display: flex;
            gap: 10px;
            align-items: flex-end;
        }
        vaadin-text-area {
            flex-grow: 1;
        }
        .progress {
            margin-top: 1em;
        }
        .section {
            display: flex;
            flex-direction: column;
            gap: 6px;
            margin-top: 1em;
        }
        .heading {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .heading h3 {
            margin: 0;
        }
        .secondary {
            color: var(--lumo-secondary-text-color);
            font-size: var(--lumo-font-size-s);
        }
    `;

    static properties = {
        _description: { state: true },
        _loading: { state: true },
        _job: { state: true }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._description = '';
        this._loading = false;
        this._job = null;
        this.md = new MarkdownIt();
    }

    render() {
        return html`
            <div class="input">
                <vaadin-text-area
                    min-rows="3"
                    max-rows="8"
                    .label=${msg('Describe the batch job you want to create', { id: 'quarkus-jberet-describe-job' })}
                    .placeholder=${msg('For example: read the orders from orders.csv, skip the ones without a customer, and write them to the orders table', { id: 'quarkus-jberet-describe-job-placeholder' })}
                    .value=${this._description}
                    @value-changed=${(e) => this._description = e.detail.value}>
                </vaadin-text-area>
                <qui-assistant-button
                    ?disabled=${this._loading || !this._description.trim()}
                    title=${msg('Use the Quarkus Assistant to generate the job', { id: 'quarkus-jberet-generate-job-title' })}
                    @click=${() => this._generate()}>
                    ${msg('Generate job', { id: 'quarkus-jberet-generate-job' })}
                </qui-assistant-button>
            </div>
            ${this._renderProgress()}
            ${this._renderJob()}
        `;
    }

    _renderProgress() {
        if (!this._loading) {
            return html``;
        }
        return html`
            <div class="progress">
                <div class="secondary">${msg('Talking to the Dev Assistant ... please wait', { id: 'quarkus-jberet-talking-to-assistant' })}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
    }

    _renderJob() {
        if (!this._job || this._loading) {
            return html``;
        }
        const jobName = this._job.jobName || 'job';
        return html`
            ${this._job.markdown ? html`<div class="section">${unsafeHTML(this.md.render(this._job.markdown))}</div>` : ''}
            ${this._renderCode(`src/main/resources/META-INF/batch-jobs/${jobName}.xml`, 'xml', this._job.jobXml)}
            ${this._renderCode(msg('Batch artifacts', { id: 'quarkus-jberet-batch-artifacts' }), 'java', this._job.java)}
            ${this._renderCode('application.properties', 'properties', this._job.configuration)}
        `;
    }

    _renderCode(title, mode, content) {
        if (!content || !content.trim()) {
            return html``;
        }
        return html`
            <div class="section">
                <div class="heading">
                    <h3>${title}</h3>
                    <vaadin-button theme="tertiary" @click=${() => this._copy(content)}>
                        <vaadin-icon icon="font-awesome-solid:copy" slot="prefix"></vaadin-icon>
                        ${msg('Copy', { id: 'quarkus-jberet-copy' })}
                    </vaadin-button>
                </div>
                <qui-themed-code-block mode="${mode}" content="${this._stripFences(content)}" showLineNumbers></qui-themed-code-block>
            </div>`;
    }

    // Models sometimes wrap the code in markdown fences
    _stripFences(content) {
        return content.trim().replace(/^```[a-z]*\n/, '').replace(/\n```$/, '');
    }

    async _generate() {
        // qui-assistant-button fires the click event twice (its own, and the native one)
        if (this._loading) {
            return;
        }
        this._loading = true;
        try {
            const jsonResponse = await this.jsonRpc.generateJob({ description: this._description });
            this._job = jsonResponse.result;
        } catch (e) {
            const error = e?.error?.message || e;
            notifier.showErrorMessage(msg(str`Failed to generate the job: ${error}`, { id: 'quarkus-jberet-generate-job-failed' }));
        } finally {
            this._loading = false;
        }
    }

    _copy(content) {
        navigator.clipboard.writeText(this._stripFences(content))
            .then(() => notifier.showInfoMessage(msg('Copied to clipboard', { id: 'quarkus-jberet-copied' })))
            .catch(err => notifier.showErrorMessage(msg(str`Failed to copy: ${err}`, { id: 'quarkus-jberet-copy-failed' })));
    }
}

customElements.define('qwc-jberet-job-builder', QwcJberetJobBuilder);
