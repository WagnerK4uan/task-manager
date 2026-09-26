package com.taskmanager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: um PostgreSQL só, compartilhado por todas as classes que a
 * estendem. O container é iniciado uma vez e vive enquanto a JVM de teste viver — o ciclo de vida do
 * {@code @Container} do JUnit o pararia ao fim de cada classe, e o contexto Spring reaproveitado
 * pela classe seguinte tentaria conectar numa porta que já não existe.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.username=irrelevante",
            "spring.datasource.password=irrelevante"
        })
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }
}
