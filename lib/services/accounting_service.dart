import 'dart:io';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';

class AccountingService {
  static Future<Directory> _docsDir() async {
    if (Platform.isWindows || Platform.isLinux || Platform.isMacOS) {
      final docs = await getApplicationDocumentsDirectory();
      return Directory('${docs.path}${Platform.pathSeparator}InkaToysPOS');
    }
    final docs = await getApplicationDocumentsDirectory();
    return Directory('${docs.path}${Platform.pathSeparator}InkaToysPOS');
  }

  static Future<Directory> ensureTodayDir() async {
    final base = await _docsDir();
    if (!base.existsSync()) base.createSync(recursive: true);
    final today = DateFormat('yyyyMMdd').format(DateTime.now());
    final day = Directory('${base.path}${Platform.pathSeparator}CONTABILIDAD${Platform.pathSeparator}$today');
    if (!day.existsSync()) day.createSync(recursive: true);
    return day;
  }

  static Future<File> appendCsv(Map<String, dynamic> row) async {
    final d = await ensureTodayDir();
    final date = DateFormat('yyyyMMdd').format(DateTime.now());
    final f = File('${d.path}${Platform.pathSeparator}ventas_${date}.csv');
    final header = 'ts,consec,turno,metodo,monto,personas,ticket_path\n';
    if (!f.existsSync()) f.writeAsStringSync(header, mode: FileMode.write, flush: true);
    final line = [
      row['ts'], row['consec'], row['turno'], row['metodo'],
      row['monto'], row['personas'], row['ticket_path']
    ].join(',') + '\n';
    f.writeAsStringSync(line, mode: FileMode.append, flush: true);
    return f;
  }

  static Future<File> writeResumen({required int efectivo, required int transfer, required int total}) async {
    final d = await ensureTodayDir();
    final date = DateFormat('yyyy-MM-dd').format(DateTime.now());
    final f = File('${d.path}${Platform.pathSeparator}resumen.txt');
    final text = 'Ventas del día $date\nTotal EFECTIVO: \$${efectivo} COP\n'
        'Total TRANSFERENCIAS: \$${transfer} COP\nTOTAL GLOBAL: \$${total} COP\n';
    f.writeAsStringSync(text, flush: true);
    return f;
  }
}
