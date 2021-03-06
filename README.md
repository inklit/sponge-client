# BadgeUp Sponge Client
> Easily add Achievements and Awards to your Sponge server

[![BadgeUp & SpongePowered](./badgeup-sponge.png)](https://www.badgeup.io)

## Install & Run

To run the BadgeUp Sponge Client on your server, follow the [quickstart instructions](https://docs.badgeup.io/#/sponge-client/quickstart) on the BadgeUp documentation site.

## Development

### Building the Project

To build the client jar, run `gradlew build`. This will output the built jar to `build/libs/sponge-client-1.0.0-all.jar`.

### Configuration

See the [BadgeUp Docs](https://docs.badgeup.io/#/sponge-client/configuration) for standard configuration documentation.

* `badgeup`/`base-api-url`: to be used in development to target a BadgeUp API other than the standard production URL.

### Documentation

To generate documentation needed for [docs.badgeup.io](https://docs.badgeup.io/), run `node docs/doc.js`. The rendered HTML will reside in `docs/build/`.

## Docker

To build the Docker image: `docker build -t badgeup-sponge-server:1.0.2 .`

To run the server: `docker run -p 127.0.0.1:25565:25565 --interactive badgeup-sponge-server:1.0.2`

## Disclaimer

*The use of the SpongePowered or other third-party logos and trademarks is not intended to imply endorsement of BadgeUp LLC. BadgeUp does not claim ownership of any third-party logs and trademarks used.*
