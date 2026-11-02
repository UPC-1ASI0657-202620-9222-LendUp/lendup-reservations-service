package com.lendup.reservas.domain.model;
import static org.junit.jupiter.api.Assertions.*; import java.math.BigDecimal; import java.time.LocalDateTime; import org.junit.jupiter.api.Test;
class PeriodoPrestamoTest { private final LocalDateTime base=LocalDateTime.of(2026,11,1,9,0);
  @Test void rechazaPeriodosInvalidos(){assertThrows(IllegalArgumentException.class,()->new PeriodoPrestamo(base,base));assertThrows(IllegalArgumentException.class,()->new PeriodoPrestamo(base,base.minusSeconds(1)));}
  @Test void intervalosQueSeTocanNoSeSuperponen(){var a=new PeriodoPrestamo(base,base.plusDays(1));var b=new PeriodoPrestamo(base.plusDays(1),base.plusDays(2));assertFalse(a.seSuperponeCon(b));}
  @Test void detectaSolapamientoSemiabierto(){var a=new PeriodoPrestamo(base,base.plusDays(2));var b=new PeriodoPrestamo(base.plusHours(23),base.plusDays(3));assertTrue(a.seSuperponeCon(b));}
  @Test void cobraBloquesIniciadosDe24HorasSinRedondeoBinario(){var p=new PeriodoPrestamo(base,base.plusHours(24).plusSeconds(1));assertEquals(2,p.bloquesIniciadosDe24Horas());assertEquals(new BigDecimal("25.00"),p.importe(new BigDecimal("12.50")));}
  @Test void fraccionMenorAUnSegundoIniciaUnBloque(){assertEquals(1,new PeriodoPrestamo(base,base.plusNanos(1)).bloquesIniciadosDe24Horas());}
  @Test void exactamenteVeinticuatroHorasEsUnBloque(){assertEquals(1,new PeriodoPrestamo(base,base.plusHours(24)).bloquesIniciadosDe24Horas());}
  @Test void veinticuatroHorasMasUnaFraccionSonDosBloques(){assertEquals(2,new PeriodoPrestamo(base,base.plusHours(24).plusNanos(1)).bloquesIniciadosDe24Horas());}
  @Test void variosDiasExactosNoAgreganBloque(){assertEquals(30,new PeriodoPrestamo(base,base.plusDays(30)).bloquesIniciadosDe24Horas());}
  @Test void extremosDeLocalDateTimeNoDesbordan(){long blocks=assertDoesNotThrow(()->new PeriodoPrestamo(LocalDateTime.MIN,LocalDateTime.MAX).bloquesIniciadosDe24Horas());assertTrue(blocks>0);}
  @Test void rechazaFechasNulas(){assertThrows(IllegalArgumentException.class,()->new PeriodoPrestamo(null,base));assertThrows(IllegalArgumentException.class,()->new PeriodoPrestamo(base,null));}
  @Test void rechazaTarifaNoPositiva(){var p=new PeriodoPrestamo(base,base.plusHours(1));assertThrows(IllegalArgumentException.class,()->p.importe(BigDecimal.ZERO));}
}
