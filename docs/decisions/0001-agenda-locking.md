# ADR 0001: serialización por agenda

Estado: aceptada.

Se crea una agenda única por publicación con `INSERT IGNORE` en un bootstrap corto `REQUIRES_NEW`. La transacción principal bloquea después esa fila con `SELECT FOR UPDATE`, carga en una consulta sus intervalos activos y aplica la política de `AgendaDelObjeto`. Esto serializa aceptaciones del mismo objeto sin bloquear objetos distintos. Mantener el insert inicial dentro de cada una de las 20 transacciones exteriores produjo deadlocks de gap/unique locks en MySQL 8.4; la fila vacía creada por el bootstrap es idempotente e inocua. Una consulta previa o una proyección de Catálogo no evita la carrera.
