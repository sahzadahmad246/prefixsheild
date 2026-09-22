package dev.shahzad.prefixshield

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

@Composable
fun ShareQrDialog(title: String, payload: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val image = remember(payload) { qrBitmap(payload).asImageBitmap() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Image(image, contentDescription = "QR code", modifier = Modifier.size(220.dp))
            Spacer(Modifier.height(8.dp))
            Text(payload, color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, payload)
                context.startActivity(Intent.createChooser(send, "Share"))
            }) { Text("Share text", color = Accent) }
            TextButton(onClick = {
                val file = File(context.cacheDir, "share/contact-qr.png").apply { parentFile?.mkdirs() }
                qrBitmap(payload).compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val send = Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Share QR"))
            }) { Text("Share QR", color = Accent) }
            TextButton(onClick = onDismiss) { Text("Close", color = TextDim) }
        }
    }
}

private fun qrBitmap(text: String, size: Int = 640): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}
