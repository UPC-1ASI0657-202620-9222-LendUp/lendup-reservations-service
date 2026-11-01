package com.lendup.reservas.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="lendup.integrations")
public record IntegrationProperties(Catalog catalog, Identity identity){
  public IntegrationProperties {
    if(catalog==null)catalog=new Catalog("","","",2000,3000);
    if(identity==null)identity=new Identity("","","","",2000,3000);
  }
  public record Catalog(String baseUrl,String serviceToken,String snapshotUriTemplate,int connectTimeoutMs,int readTimeoutMs){}
  public record Identity(String baseUrl,String serviceToken,String firebaseUserUriTemplate,String authorizedContactUriTemplate,int connectTimeoutMs,int readTimeoutMs){}
}
