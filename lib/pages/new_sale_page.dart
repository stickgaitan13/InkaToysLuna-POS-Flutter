import 'dart:io';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/pdf_service.dart';
import '../services/accounting_service.dart';

class NewSalePage extends StatefulWidget {
  const NewSalePage({super.key});

  @override
  State<NewSalePage> createState() => _NewSalePageState();
}

class _NewSalePageState extends State<NewSalePage> {
  final turno = TextEditingController();
  final metodo = ValueNotifier<String>('EFECTIVO');
  final monto = TextEditingController();
  final personas = TextEditingController();

  int consec = DateTime.now().millisecondsSinceEpoch % 1000000;

  @override
  void dispose() {
    turno.dispose(); monto.dispose(); personas.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nueva Venta')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: turno,
            decoration: const InputDecoration(labelText: 'Turno (ej. Mañana/Tarde)'),
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            value: metodo.value,
            items: const [
              DropdownMenuItem(value: 'EFECTIVO', child: Text('EFECTIVO')),
              DropdownMenuItem(value: 'TRANSFERENCIA', child: Text('TRANSFERENCIA')),
            ],
            onChanged: (v) => metodo.value = v ?? 'EFECTIVO',
            decoration: const InputDecoration(labelText: 'Método de pago'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: monto,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(labelText: 'Monto (COP)'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: personas,
            decoration: const InputDecoration(labelText: 'Personas'),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            icon: const Icon(Icons.print),
            label: const Text('Registrar venta e imprimir ticket'),
            onPressed: () async {
              final now = DateTime.now();
              final bytes = await PdfService.buildTicketPdf(
                consec: consec,
                turno: turno.text,
                metodo: metodo.value,
                monto: int.tryParse(monto.text.trim()) ?? 0,
                personas: personas.text,
              );
              final f = await PdfService.saveTicketToAccounting(bytes, consec);
              await AccountingService.appendCsv({
                'ts': DateFormat('yyyy-MM-dd HH:mm:ss').format(now),
                'consec': consec,
                'turno': turno.text,
                'metodo': metodo.value,
                'monto': int.tryParse(monto.text.trim()) ?? 0,
                'personas': personas.text,
                'ticket_path': f.path,
              });
              await PdfService.printOrShare(bytes);
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Venta registrada e impresión enviada.'))
                );
              }
              setState(() { consec = (consec + 1) % 1000000; });
            },
          )
        ],
      ),
    );
  }
}
