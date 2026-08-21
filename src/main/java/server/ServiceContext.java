package server;

public record ServiceContext(
        GameManager gameManager,
        UserManager userManager,
        GameQueryService gameQuery,
        GameViewFormatter formatter
) {}