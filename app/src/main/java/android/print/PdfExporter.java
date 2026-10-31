package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

/**
 * Writes a WebView's print output straight to a PDF file, without showing the print dialog.
 * Lives in package android.print so it can use the print callbacks directly.
 */
public final class PdfExporter {
    public interface Done { void onDone(boolean ok); }

    private PdfExporter() {}

    public static void export(final PrintDocumentAdapter adapter, final PrintAttributes attrs,
                              final ParcelFileDescriptor out, final Done done) {
        adapter.onStart();
        adapter.onLayout(null, attrs, new CancellationSignal(), new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, out, new CancellationSignal(),
                    new PrintDocumentAdapter.WriteResultCallback() {
                        @Override public void onWriteFinished(PageRange[] pages) { adapter.onFinish(); done.onDone(true); }
                        @Override public void onWriteFailed(CharSequence error) { adapter.onFinish(); done.onDone(false); }
                        @Override public void onWriteCancelled() { adapter.onFinish(); done.onDone(false); }
                    });
            }
            @Override public void onLayoutFailed(CharSequence error) { adapter.onFinish(); done.onDone(false); }
            @Override public void onLayoutCancelled() { adapter.onFinish(); done.onDone(false); }
        }, null);
    }
}
