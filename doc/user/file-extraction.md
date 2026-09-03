# File Extraction

An agent's **User message** accepts more than text. Reference an image or a PDF in it and Smart Workflow forwards the file to the model as multimodal input — the model reads the document and extracts whatever you asked for, typically into a typed object via [Structured Output](agent-setup.md#structured-output).

There is no separate "file" field. File extraction is driven entirely by what the `<%=...%>` expressions in the User message resolve to.

## Supported formats

| Format | Types |
| --- | --- |
| Image | PNG, JPG, JPEG |
| PDF | PDF |

Anything else is not converted to model content. Images are always sent at `DetailLevel.HIGH`, which costs more tokens than a low-detail request — relevant when you process documents in bulk.

Whether a given provider can actually accept an image or a PDF is a separate question. See the [file extraction support matrix](reference/capabilities.md#file-extraction).

> **Important:** Smart Workflow does not check provider capability before sending. It builds the image or PDF content unconditionally, so an unsupported combination fails at the provider, not locally, and the error text comes from the provider's API.

## Passing a file from the CMS

Reference a CMS content object with `ivy.cms.co()` inside the User message:

```text
Extract the invoice data from this document:
<%=ivy.cms.co("/Files/Documents/InvoiceSample")%>
```

This form is special-cased. The expression is matched against a literal pattern and the path is extracted as text — `ivy.cms.co(...)` is **not** executed. Only the exact single-argument form is recognized; a computed path such as `ivy.cms.co(in.docPath)` falls through to the general expression handling described below.

How the CMS object is turned into model content depends on its type and file extension:

| CMS object | Result |
| --- | --- |
| Type `STRING` | Inlined as text |
| Type `FILE`, extension `png` / `jpg` / `jpeg` / `pdf` | Sent as image or PDF content |
| Type `FILE`, extension `txt` / `md` | File is read and inlined as UTF-8 text |
| Type `FILE`, any other extension | Fails — see below |
| Path does not exist | Silently dropped from the message |

PDFs are **not** parsed locally. The raw bytes are base64-encoded and shipped to the provider, which does the reading. No text or layout extraction happens in Smart Workflow.

> **Important:** Two of the rows above are easy to mistake for a model problem.
>
> - A **missing CMS path** contributes nothing to the message. The agent still runs, just without the document — and without a log entry. Note the contrast with a normal expression, which falls back to inserting its own raw `<%=...%>` text when it cannot be resolved.
> - An **unsupported extension** raises an error internally, but it is caught while the message is being assembled. The whole User message ends up empty, and the agent call is then **skipped** with only `Agent call was skipped, since there was no user query` in the log.

## Passing a file from process data

Any expression that resolves to one of these types is turned into file content:

| Type | Filename used for type detection |
| --- | --- |
| `java.io.InputStream` | none — content is sniffed |
| `ch.ivyteam.ivy.scripting.objects.Binary` | none — content is sniffed |
| `java.nio.file.Path` | file name |
| `java.io.File` | file name |
| `ch.ivyteam.ivy.scripting.objects.File` | file name |
| `ch.ivyteam.ivy.workflow.document.IDocument` | document name |

```text
Extract the invoice data from this document:
<%=in.uploadedInvoice%>
```

This is the path to use for **documents your users upload**, which is the common production case — a file picker writes to a process data field and you reference that field directly.

Type detection differs between the two halves of the table. Where a file name is available, the extension decides. Where it is not (`InputStream` and `Binary`), the first bytes are sniffed, recognizing PNG, JPEG, and the `%PDF-` prefix.

> **Note:** A named file with no extension is detected as neither. `Path`, `File` and `IDocument` values whose name lacks a `.png` / `.jpg` / `.jpeg` / `.pdf` suffix produce no content and are dropped from the message — the byte sniffing does not run as a fallback for them.

An expression resolving to anything else — a `String`, a number, a data object — is converted with `String.valueOf(...)` and inlined as text, which is the ordinary non-file behaviour.

## Example

Extracting invoice fields from a scanned image into a typed object. The **System message** and **Output** configuration are exactly what they would be for text input; only the User message changes.

**System message:**

```text
You are an invoice extraction agent.
You receive an invoice document (image or PDF) and extract the following fields:
- invoiceNumber (String)
- supplierName (String)
- totalAmount (BigDecimal)
- currency (String, ISO 4217)
- invoiceDate (LocalDate, format yyyy-MM-dd)

Return only the structured data. Do not add commentary.
If a field is missing in the document, return null for that field.
```

**User message:**

```text
Extract the invoice data from this document:
<%=ivy.cms.co("/Files/Documents/InvoiceSample")%>
```

**Expect result of type:** `com.axonivy.utils.ai.Invoice.class`

**Map result to:** `in.invoiceResult`

The mapped field is a ready-to-use object — no casting, no parsing:

```java
in.invoiceResult.invoiceNumber
in.invoiceResult.totalAmount
```

For a working implementation, see the `FileExtractionDemo` process in the [`FileExtractionDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/FileExtractionDemo.p.json) project. It covers all three input styles — a CMS file, an `InputStream`, and a `Binary`.

## Common mistakes

- **Provider cannot do vision or PDF.** Check the [support matrix](reference/capabilities.md#file-extraction) first. Nothing warns you locally; the request reaches the provider and fails there. xAI and Ollama have no PDF support — convert to images first.
- **Expecting local PDF parsing.** The provider reads the PDF, so PDF quality and page limits are the provider's, not ours.
- **A wrong CMS path.** Nothing fails loudly. If the model answers as though it never saw the document, verify the path before suspecting the prompt.
- **An unexpected file extension in the CMS.** A `.tiff` or `.docx` object does not merely get skipped — it takes the whole agent call down with it, silently.
- **Assuming the CMS is the only route.** For uploaded documents, reference the process data field directly.

## See also

- [Agent Setup](agent-setup.md) — the element's fields, and structured output
- [Model Providers](providers.md) — choosing a provider that supports your document types
- [Provider Capabilities](reference/capabilities.md) — the per-provider support matrix
