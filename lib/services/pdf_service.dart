import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/services.dart' show rootBundle;
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:pdf/pdf.dart';
import 'package:barcode/barcode.dart';
import 'package:printing/printing.dart';

class PdfService {
  static const companyName = 'Inka Toys Luna';

  static Future<Uint8List> buildTicketPdf({
    required int consec,
    required String turno,
    required String metodo,
    required int monto,
    required String personas,
  }) async {
    final doc = pw.Document();
    final now = DateTime.now();
    final dateStr = DateFormat('yyyy-MM-dd HH:mm').format(now);

    // Logos
    final colorLogoBytes = await rootBundle.load('assets/logo_color.png');
    final grayLogoBytes = await rootBundle.load('assets/logo_grayscale.png');

    // Page: ancho configurable (58mm -> ~164pt, 80mm -> ~226pt); usamos 220pt por defecto
    final pageTheme = pw.PageTheme(
      pageFormat: PdfPageFormat(220, PdfPageFormat.a4.height,
          marginAll: 10), // alto dinámico (se corta por contenido)
      orientation: pw.PageOrientation.portrait,
      buildBackground: (ctx) => pw.FullPage(
        ignoreMargins: true,
        child: pw.Container(color: PdfColors.white),
      ),
    );

    // Barcodes
    final qrSvg = Barcode.qrCode();
    final code128 = Barcode.code128();

    doc.addPage(pw.Page(theme: pageTheme, build: (ctx) {
      return pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.stretch,
        children: [
          // Logo gris a máxima nitidez
          pw.Center(
            child: pw.Image(pw.MemoryImage(grayLogoBytes.buffer.asUint8List()),
              width: 180, // más ancho que 140
              fit: pw.BoxFit.contain,
              filterQuality: PdfImageFilter.qualityHigh,
            ),
          ),
          pw.SizedBox(height: 6),
          pw.Center(child: pw.Text(companyName, style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),

          pw.SizedBox(height: 6),
          pw.Divider(),
          pw.SizedBox(height: 2),

          pw.Center(child: pw.Text('TICKET ${consec.toString().padLeft(6, '0')}', style: pw.TextStyle(fontSize: 12, fontWeight: pw.FontWeight.bold))),
          pw.SizedBox(height: 2),
          pw.Center(child: pw.Text(dateStr, style: const pw.TextStyle(fontSize: 8))),

          pw.SizedBox(height: 8),
          _row('Turno', turno),
          _row('Método', metodo),
          _row('Monto', '\$${monto}'),
          _row('Personas', personas),

          pw.SizedBox(height: 8),
          pw.Divider(),
          pw.SizedBox(height: 2),

          // QR + Code128
          pw.Center(
            child: pw.BarcodeWidget(
              barcode: qrSvg,
              data: 'Inka Toys Luna · Ticket $consec · $dateStr',
              width: 80,
              height: 80,
            ),
          ),
          pw.SizedBox(height: 6),
          pw.Center(
            child: pw.BarcodeWidget(
              barcode: code128,
              data: consec.toString().padLeft(6, '0'),
              width: 120,
              height: 28,
            ),
          ),
          pw.SizedBox(height: 8),
          pw.Center(child: pw.Text('¡Gracias por su compra!', style: const pw.TextStyle(fontSize: 8, fontStyle: pw.FontStyle.italic))),
        ],
      );
    }));

    return await doc.save();
  }

  static pw.Widget _row(String a, String b) {
    return pw.Row(
      mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
      children: [
        pw.Text(a, style: pw.TextStyle(fontSize: 9)),
        pw.Text(b, style: pw.TextStyle(fontSize: 9, fontWeight: pw.FontWeight.bold)),
      ],
    );
  }

  static Future<File> saveTicketToAccounting(Uint8List bytes, int consec) async {
    final docs = await getApplicationDocumentsDirectory();
    final dir = Directory('${docs.path}${Platform.pathSeparator}InkaToysPOS${Platform.pathSeparator}CONTABILIDAD'
        '${Platform.pathSeparator}${DateFormat('yyyyMMdd').format(DateTime.now())}');
    if (!dir.existsSync()) dir.createSync(recursive: true);
    final f = File('${dir.path}${Platform.pathSeparator}Ticket_${consec.toString().padLeft(6, '0')}.pdf');
    await f.writeAsBytes(bytes, flush: True);
    return f;
  }

  static Future<void> printOrShare(Uint8List bytes) async {
    await Printing.layoutPdf(onLayout: (format) async => bytes);
  }
}
