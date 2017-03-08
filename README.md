grpc ChatApplication
==============================================

To build the chat, run in this directory:

```
$ ./gradlew installDist
```

This creates the scripts `char-server`, `chat-client`, 
in the `build/install/chat/bin/` directory that run the chat. Each
chat requires the server to be running before starting the client.

To run the server:

```
$ ./build/install/chat/bin/chat-server
```

And in a different terminal window run:

```
$ ./build/install/chat/bin/chat-client
```

That's it!

## Maven

If you prefer to use Maven:
```
$ mvn verify
$ # Run the server
$ mvn exec:java -Dexec.mainClass=io.grpc.chat.ClientServer
$ # In another terminal run the client
$ mvn exec:java -Dexec.mainClass=io.grpc.chat.ClientClient
```
