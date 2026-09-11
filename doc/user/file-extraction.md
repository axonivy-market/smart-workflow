# File Extraction

Agents are not limited to text. Reference a scanned invoice, a signed contract or a photographed form in an agent's **User message**, and the model reads the document itself — handing back the fields you asked for, typically as a typed Java object via [Structured Output](agent-setup.md#structured-output). No manual data entry, and no OCR service to wire up.

There is no upload step and no dedicated file field either. A document can come from the CMS, from a file a user has just uploaded, or from anywhere else in your process data — whatever the `<%=...%>` expressions in the User message resolve to, Smart Workflow turns into model content for you.

## Supported formats

| Format | Types |
| --- | --- |
| Image | PNG, JPG, JPEG |
| PDF | PDF |

PDFs are not parsed locally — the raw bytes go to the provider, which does the reading, so page limits and scan quality are the provider's concern rather than ours.

Because models are built differently, not every one of them can read every format. Most handle both images and PDFs, some accept images only, and on Ollama it depends which model you have pulled. Check the [file extraction support matrix](reference/capabilities.md#file-extraction) for the provider you are using.

> **Important:** Smart Workflow does not check provider capability before sending. It builds the image or PDF content unconditionally, so an unsupported combination fails at the provider, not locally, and the error text comes from the provider's API.

## Load a file from the CMS

Smart Workflow can load a file from the CMS. Reference the content object with `ivy.cms.co()` in the **User message**:

```
Extract the invoice data from this document:
<%=ivy.cms.co("/Files/Documents/InvoiceSample")%>
```

An image or PDF object becomes file content, while a `txt`, `md` or plain string object is read and inlined as text.

## Load a file from process data

Smart Workflow can load a file straight from your process data. Reference the field in the **User message** with a `<%=...%>` expression, and that is all it takes:

```
Extract the invoice data from this document:
<%=in.uploadedInvoice%>
```

It works with the file types you already have in a process — `InputStream`, `Binary`, `Path`, `File` and `IDocument`. An expression resolving to anything else is inlined as text instead.

## Example

Extracting invoice fields from a scanned image into a typed object. The **System message** and **Output** configuration are exactly what they would be for text input; only the User message changes.

Because the result class supplies the schema, the model already knows the field names and their types — there is no need to list them. Use the system message for what the schema cannot express: what a field means, how to choose between candidates on the page, and when to leave one empty.

**System message:**

```
You are an invoice extraction agent. You receive an invoice as an image or PDF.

- totalAmount is the gross total including tax, not the net subtotal.
- currency must be the ISO 4217 code, even when the document shows only a symbol.
- invoiceDate is the date the invoice was issued, not the due or delivery date.
- Return null for any field the document does not state. Do not infer or calculate it.
```

**User message:**

```
Extract the invoice data from this document:
<%=ivy.cms.co("/Files/Documents/InvoiceSample")%>
```

**Expect result of type:** `com.axonivy.utils.ai.Invoice.class`

**Map result to:** `in.invoiceResult`

The mapped field is a ready-to-use object — no casting, no parsing:

```
in.invoiceResult.invoiceNumber
in.invoiceResult.totalAmount
```

For a working implementation, see the `FileExtractionDemo` process in the [`FileExtractionDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/FileExtractionDemo.p.json) project. It covers all three input styles — a CMS file, an `InputStream`, and a `Binary`.

## Common mistakes

- Provider cannot do vision or PDF. Check the [support matrix](reference/capabilities.md#file-extraction) first. Nothing warns you locally; the request reaches the provider and fails there. xAI and Ollama have no PDF support — convert to images first.
- Expecting local PDF parsing. The provider reads the PDF, so PDF quality and page limits are the provider's, not ours.
- A wrong CMS path. Nothing fails loudly. If the model answers as though it never saw the document, verify the path before suspecting the prompt.
- An unexpected file extension in the CMS. A `.tiff` or `.docx` object does not merely get skipped — it takes the whole agent call down with it, silently.
- A filename without a recognised extension. A `Path`, `File` or `IDocument` whose name does not end in `.png`, `.jpg`, `.jpeg` or `.pdf` is dropped from the message with no log entry. `InputStream` and `Binary` are exempt — they have no filename, so their content is inspected instead.

If a document does not seem to reach the model, [Troubleshooting](troubleshooting.md#the-agent-answered-but-not-as-expected) works back from the symptom instead.

## See also

- [Agent Setup](agent-setup.md) — the element's fields, and structured output
- [Model Providers](providers.md) — choosing a provider that supports your document types
- [Provider Capabilities](reference/capabilities.md) — the per-provider support matrix
- [Troubleshooting](troubleshooting.md) — when a document does not reach the model
