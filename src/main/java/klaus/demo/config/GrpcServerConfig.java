package klaus.demo.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import klaus.demo.ticket.service.TicketServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GrpcServerConfig {
    private static final int PORT = 50051;
    private Server server;

    Logger logger = LoggerFactory.getLogger(GrpcServerConfig.class);

    private final TicketServiceImpl ticketService;

    public GrpcServerConfig(TicketServiceImpl ticketService) throws IOException, InterruptedException {
        this.ticketService = ticketService;
        start();
    }

    public void start() throws IOException, InterruptedException {
        logger.info("Starting server on port: " + PORT);
        server = ServerBuilder.forPort(PORT)
                .addService(ticketService)
                .build()
                .start();

        blockUntilShutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server == null) {
            return;
        }

        server.awaitTermination();
    }
}
