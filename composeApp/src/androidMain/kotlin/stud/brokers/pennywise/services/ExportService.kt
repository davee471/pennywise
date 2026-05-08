package stud.brokers.pennywise.services

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stud.brokers.pennywise.util.Result

/**
 * Android implementation of [ExportService].
 *
 * Writes the generated invoice to the device's public Downloads directory.
 * Currently writes as an .html file so it compiles natively without extra libraries.
 */
actual class ExportService {

    /** The Android context required for system services. */
    private var context: Context? = null

    /**
     * Sets the Android [Context] for this service.
     *
     * @param ctx The context to use.
     */
    fun setContext(ctx: Context) {
        this.context = ctx
    }

    /**
     * Generates a PDF invoice using the Android PrintManager and a hidden [WebView].
     *
     * @param htmlContent The HTML content to export.
     * @return [Result.Success] on successful print job initiation, or [Result.Error] on failure.
     */
    actual suspend fun exportToPdf(htmlContent: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val ctx = context ?: throw Exception("Android Context not provided to ExportService")
                val webView = WebView(ctx)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val jobName = "PennyWise_Invoice_${System.currentTimeMillis()}"
                        val printAdapter = view.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Invoice Export Failed", Result.ErrorType.FILESYSTEM)
            }
        }
    }
}
