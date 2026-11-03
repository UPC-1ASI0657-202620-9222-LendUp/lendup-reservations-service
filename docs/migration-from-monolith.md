# Migración desde el monolito

No existe lectura cross-schema en runtime. El cutover debe ser un procedimiento separado:

1. Inventariar y respaldar `solicitudes`, `agendas_objeto` y `reservas` históricos.
2. Validar UUID, estados, periodos, moneda, snapshots y relaciones internas; decidir el mapeo de cancelaciones antiguas.
3. Crear `lendup_reservas`, ejecutar Flyway y exportar/importar en ventana controlada. No copiar tablas de otros contextos.
4. Conciliar conteos, unicidad solicitud-reserva, agenda-publicación y ausencia de solapamientos activos.
5. Detener escrituras del monolito para estas rutas, realizar delta final y cambiar Gateway.
6. Mantener rollback por routing durante la ventana, sin doble escritura no coordinada.

Flyway V3 es expandible y preserva V1/V2: añade referencias de consentimiento, clave de deduplicación pendiente, versión de agregado, periodo de ocupación y leasing Outbox. Inicializa `ocupacion_desde/hasta` con el periodo contractual. Solo copia evidencia de aceptación a una reserva cuando la solicitud histórica ya estaba marcada como verificada; no fabrica hash, ID ni timestamp. `HistoricalMigrationIT` ejecuta V1, inserta datos representativos y aplica V2/V3 antes de validar filas, fechas, restricciones e índices.

La contradicción histórica RF15 (“antes de entrega”) frente a la regla vigente (“antes de confirmación de recepción”) se resuelve a favor de la regla vigente. Una reserva solo pasa a cancelada cuando Préstamos emite `PrestamoCancelado`; entrega registrada sin recepción no libera inventario automáticamente.
