package com.lendup.reservas.infrastructure.client;

import com.lendup.reservas.application.exception.DependencyUnavailableException;
import com.lendup.reservas.application.port.out.CatalogoPort;
import com.lendup.reservas.config.IntegrationProperties;
import com.lendup.reservas.domain.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogoRestAdapter implements CatalogoPort {
  private final IntegrationProperties.Catalog config; private final RestClient client;
  public CatalogoRestAdapter(IntegrationProperties props){config=props.catalog();client=config.baseUrl().isBlank()?null:RestClientFactory.create(config.baseUrl(),config.connectTimeoutMs(),config.readTimeoutMs());}
  @Override public PublicacionSnapshot obtenerSnapshot(UUID id,PeriodoPrestamo periodo){
    if(client==null||config.serviceToken().isBlank()||config.snapshotUriTemplate().isBlank())throw new DependencyUnavailableException("Integración con Catálogo no configurada");
    try{
      @SuppressWarnings("unchecked") Map<String,Object> row=client.get().uri(config.snapshotUriTemplate(),id,periodo.desde(),periodo.hasta()).header("Authorization","Bearer "+config.serviceToken()).retrieve().body(Map.class);
      if(row==null)throw new DependencyUnavailableException("Catálogo devolvió una respuesta vacía");
      CondicionesAcordadas c=new CondicionesAcordadas(decimal(row,"tarifa_diaria"),nullableDecimal(row,"garantia_monetaria"),text(row,"moneda"),text(row,"lugar_intercambio"),text(row,"condiciones_uso"),text(row,"condiciones_entrega"),text(row,"condiciones_devolucion"),text(row,"condiciones_cancelacion"));
      UUID returnedId=UUID.fromString(text(row,"id"));
      if(!id.equals(returnedId))throw new DependencyUnavailableException("Catálogo devolvió una publicación distinta");
      return new PublicacionSnapshot(returnedId,UUID.fromString(text(row,"propietario_usuario_id")),
          "ACTIVA".equals(text(row,"estado")),Boolean.TRUE.equals(row.get("periodo_ofrecido")),
          text(row,"snapshot_id"),text(row,"snapshot_hash"),c);
    }catch(DependencyUnavailableException e){throw e;}catch(Exception e){throw new DependencyUnavailableException("Catálogo no disponible",e);}
  }
  private static String text(Map<String,Object> m,String k){Object v=m.get(k);if(v==null)throw new DependencyUnavailableException("Catálogo omitió "+k);return v.toString();}
  private static BigDecimal decimal(Map<String,Object>m,String k){return new BigDecimal(text(m,k));} private static BigDecimal nullableDecimal(Map<String,Object>m,String k){return m.get(k)==null?null:new BigDecimal(m.get(k).toString());}
}
