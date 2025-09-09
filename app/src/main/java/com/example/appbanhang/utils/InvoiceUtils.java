package com.example.appbanhang.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class InvoiceUtils {

    // Hàm tạo file PDF cho 1 hóa đơn đơn giản
    public static void exportInvoiceToPdf(Context context,
                                          String invoiceId,
                                          String tableName,
                                          String details,
                                          String total) {
        // Tạo document PDF
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("HÓA ĐƠN THANH TOÁN", 60, 40, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText("Mã hóa đơn: " + invoiceId, 20, 70, paint);
        canvas.drawText("Bàn: " + tableName, 20, 90, paint);
        canvas.drawText("Chi tiết: " + details, 20, 120, paint);
        paint.setFakeBoldText(true);
        canvas.drawText("Tổng cộng: " + total, 20, 160, paint);

        document.finishPage(page);

        // Lưu vào thư mục Documents/Invoices
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoices");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "invoice_" + invoiceId + ".pdf");

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(context, "Đã lưu: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        document.close();
    }
}
