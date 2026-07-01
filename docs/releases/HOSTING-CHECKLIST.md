# Hosted Limited-Alpha Checklist

The player downloads connect to a single hosted server. Do not publish a
client archive configured for `localhost` or for the development database.

## Live Configuration

Use `server/myworld-host.conf` for the local-PC hosted alpha. It keeps the live
database separate from the development reset workflow and listens on the game
port expected by the release packager:

```yaml
database:
	db_name: spoiled_milk_alpha

world:
	server_name: Spoiled Milk
	server_name_welcome: Spoiled Milk
	welcome_text: Spoiled Milk limited alpha playtest.
	server_port: 43605
```

Keep `client_version: 10046` and `enforce_custom_client_version: true` aligned
with the released client. If the public port differs from `43605`, provide the
same port to `scripts/package-player-release.sh`.

## Initial Deployment

1. Configure the host firewall and router port forwarding for TCP `43605` only.
   Do not forward the websocket port unless a selected client explicitly needs it.
2. Start the hosted server with `./scripts/run-hosted-server.sh` or
   `make run-hosted-server`. This creates `spoiled_milk_alpha.db` from the seed
   database only if the live database does not already exist.
3. Do not use `scripts/start-fresh.sh` for hosted play; that command recreates
   local development state.
4. Start a configured release client, register a test account, log out, restart
   the server, and confirm the account and character progress persist.

## Routine Operation

1. Back up the live SQLite database before each server build or gameplay
   deployment.
2. Run `./scripts/pre-field-test.sh` and compile the server build selected for
   deployment before replacing the hosted server binaries.
3. Keep the published client endpoint and hosted server port synchronized.
4. Record the git revision used for each hosted alpha build and each attached
   player download.
