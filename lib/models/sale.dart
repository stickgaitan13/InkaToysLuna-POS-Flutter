class Sale {
  final int id; // consecutivo
  final String turno; // mañana/tarde/noche o libre
  final String metodo; // EFECTIVO / TRANSFERENCIA
  final int monto; // en COP
  final String personas; // texto libre
  final DateTime ts;

  Sale({
    required this.id,
    required this.turno,
    required this.metodo,
    required this.monto,
    required this.personas,
    required this.ts,
  });
}
