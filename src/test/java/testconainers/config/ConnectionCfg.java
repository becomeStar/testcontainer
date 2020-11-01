package testconainers.config;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ConnectionCfg {

    private String username;

    private String password;

    private int port;

    private String seeds;

    private String keyspace;



}
