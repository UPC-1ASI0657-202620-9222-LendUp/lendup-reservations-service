# ADR 0001: serialización por agenda

Estado: aceptada.

Se crea una agenda única por publicación con `INSERT IGNORE`, se bloquea con `SELECT FOR UPDATE` y se reconsulta solapamiento dentro de la misma transacción. Esto serializa aceptaciones del mismo objeto, incluida la primera creación concurrente, sin bloquear objetos distintos. Una consulta previa o una proyección de Catálogo no evita la carrera.
