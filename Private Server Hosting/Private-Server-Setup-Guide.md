The standard release connects to a public server that anyone is welcome to join. If you'd prefer a private server this is how to go about it

## Which File Do I Use?

.bat for windows, .sh for linux


## First Time Setup

1. Install Java if you do not already have it.
   - Windows: search for `Adoptium Temurin Java 17`, download the Windows
     installer, and run it.
   - Linux: install Java from your distro app store or package manager. Java 17
     is a good choice.
2. Download this repo.
   - If you do not use Git, use GitHub's green `Code` button, choose
     `Download ZIP`, then extract the ZIP somewhere easy to find.
   - Do not run the files from inside the ZIP. Extract it first.
3. Open the `Private Server Hosting` folder.
4. Run the server file for your system.
5. Wait for the server window to finish building and say it is running.
6. Run the client file for your system.
7. Register or log in from the client.

The first server launch creates a local save database for you. After that, the
same database is reused so characters and progress remain.

## What The Server File Does

The server launcher does these steps for you:

- Checks that Java is available.
- Creates `server/inc/sqlite/myworld_dev.db` if it does not exist yet.
- Points the repo client to `localhost` and port `43605`.
- Builds the server.
- Starts the server.

You normally do not need to run anything else.

## If The Linux File Does Not Open

Some Linux desktops do not run `.sh` files when you double click them. The easy way to handle it is to right click the file,
choose properties, click permissions, check off "Allow executing file as a program." From now on double click works just fine

The slightly more cumbersome way is to open the Terminal app, drag the `.sh` into the terminal window, and press
Enter. If dragging does not work, type this from the repo folder:

```text
sh "Private Server Hosting/server.sh"
```

For the client:

```text
sh "Private Server Hosting/client.sh"
```

## Playing With Friends On The Same Network

If your friends are on the same home network:

1. Start your server with `server.bat` or `server.sh`.
2. Find your computer's local IP address.
   - Windows: open Command Prompt, type `ipconfig`, and look for `IPv4 Address`.
   - Linux: open Network settings and check connection details, or open Terminal
     and type `hostname -I`.
3. Give your friends that local IP address and the port `43605`.
4. Their client needs to use your local IP address instead of `localhost`.

Example:

```text
192.168.1.50
43605
```

`localhost` only means "this same computer", so your friends cannot use
`localhost` unless the server is running on their own machine.

## Playing Over The Internet

Hosting over the internet usually requires port forwarding.

1. Find your public IP address by searching the web for `what is my IP`.
2. Log in to your router.
3. Forward TCP port `43605` to the computer running the server.
4. Allow TCP port `43605` through your computer firewall if it asks.
5. Give your friends your public IP address and port `43605`.

Every router is different. The easiest search is usually:

```text
your router model port forward
```

If your public IP address changes later, your friends will need the new address.

## Changing The Client Address

The repo client reads its server address from these two files:

```text
Client_Base/Cache/ip.txt
Client_Base/Cache/port.txt
```

For playing on the same computer as the server:

```text
ip.txt: localhost
port.txt: 43605
```

For friends joining over LAN or internet, `ip.txt` should contain the host
computer's LAN IP or public IP instead.

## Save Data And Resets

Your local private-server save data is here:

```text
server/inc/sqlite/myworld_dev.db
```

Back up that file if you care about the characters on your private server.

To intentionally start over, close the server, delete `myworld_dev.db`, and run
the server launcher again. It will create a fresh database from the included
seed.

## Common Problems

- The server window closes right away: Java may not be installed, or the files
  may not have been extracted correctly.
- The client says it cannot connect: make sure the server window is still open.
- Friends cannot connect: check that they are using your IP address, not
  `localhost`.
- Internet friends cannot connect: port forwarding or firewall settings are the
  usual cause.
- Another app is already using the port: change `server_port` in
  `server/myworld.conf`, then use the same number in `Client_Base/Cache/port.txt`.
