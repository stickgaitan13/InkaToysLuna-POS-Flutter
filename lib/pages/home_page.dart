import 'package:flutter/material.dart';
import '../pages/new_sale_page.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Inka Toys Luna POS')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Expanded(
              child: Card(
                elevation: 2,
                child: Center(
                  child: Text('Resumen del día (en vivo)',
                    style: Theme.of(context).textTheme.titleMedium),
                ),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              height: 64,
              child: FilledButton.tonal(
                style: ButtonStyle(
                  backgroundColor: WidgetStatePropertyAll(Colors.red.shade700),
                  foregroundColor: const WidgetStatePropertyAll(Colors.white),
                  textStyle: const WidgetStatePropertyAll(TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                ),
                onPressed: () {
                  Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NewSalePage()));
                },
                child: const Text('INICIAR VENTA'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
