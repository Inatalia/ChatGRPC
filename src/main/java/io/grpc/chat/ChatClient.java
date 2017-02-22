/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc.chat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the {@link ChatServer}.
 */
public class ChatClient {
  private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

  private final ManagedChannel channel;
  private final ChatterGrpc.ChatterBlockingStub blockingStub;
  public static String messString = new String();

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public ChatClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build();
    blockingStub = ChatterGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

    /** Say hello to server. */
  public void send(String message) {
    //logger.info("Will try to greet " + name + " ...");
    ChatRequest request = ChatRequest.newBuilder().setMessage(message).build();
    ChatReply response;
    try {
      response = blockingStub.sendMessage(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Server > " + response.getMessage());
  }

    //Stub functions
    public String createUsername(String name) {
        mName req = mName.newBuilder().setName(name).build();
        mString resp = blockingStub.createUsername(req);
        return resp.getValue();
    }

    public boolean joinGroup(String name, String group) {
        mNameGroup req = mNameGroup.newBuilder().setName(name).setGroup(group).build();
        mBoolean resp = blockingStub.joinGroup(req);
        return resp.getValue();
    }

    public boolean leaveGroup(String name, String group) {
        mNameGroup req = mNameGroup.newBuilder().setName(name).setGroup(group).build();
        mBoolean resp = blockingStub.leaveGroup(req);
        return resp.getValue();
    }

    public boolean sendMessages(String name, String group, String message) {
        mNameGroupMsg req = mNameGroupMsg.newBuilder().setName(name).setGroup(group).setMessage(message).build();
        mBoolean resp = blockingStub.sendMessages(req);
        return resp.getValue();
    }

    public String getMessage(String name) {
        mName req = mName.newBuilder().setName(name).build();
        mString resp = blockingStub.getMessage(req);
        return resp.getValue();
    }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws Exception {
    final ChatClient client = new ChatClient("localhost", 50051);
    /*try {
      /* Access a service running on the local machine on port 50051 * /
      String user = "USER";
      if (args.length > 0) {
        user = args[0]; /* Use the arg as the name to greet if provided * /
      }
      while(true){
	    //logger.info(user + "> ");
        System.out.print(user + "> ");
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String message = br.readLine();
	    client.send(message);
      }
    } finally {
      client.shutdown();
    }*/

    final User u = new User();
    Scanner sc = new Scanner(System.in);
    String command = sc.nextLine();

    Timer timer = new Timer();
    TimerTask doAsynchronousTask = new TimerTask() {
        @Override
        public void run() {
            if (!u.isEmpty())
            {
                messString =  client.getMessage(u.getName());
                if (!messString.isEmpty()) {
                    System.out.print(messString);
                }
            }
        }
    };
    timer.schedule(doAsynchronousTask, 0, 1000); //execute in every 100 ms

    //...
      while (!command.equals("/EXIT")) {
          if (command.contains("/USERNAME")) {
              if (command.length() == 9) { //default username
                  u.setName(client.createUsername(""));
                  System.out.println("Successfully created nickname " + u.getName());
              } else if (command.length() >= 11 && command.charAt(8) == ' ') {
                  String name = client.createUsername(command.substring(10, command.length()));
                  if (!name.equals("")) {
                      u.setName(name);
                      System.out.println("Successfully created nickname " + u.getName());
                  } else {
                      System.out.println("Name was taken. Choose another name");
                  }

              }
          } else if (command.contains("/JOIN") && !u.isEmpty()) {
              if (command.length() == 5) { //default username
                  if (client.joinGroup(u.getName(), "channelname")) {
                      u.addChannel("channelname");
                      System.out.println("Successfully joined channelname");
                  }
              } else if (command.length() >= 7 && command.charAt(5) == ' ') {
                  if (client.joinGroup(u.getName(), command.substring(6, command.length()))) {
                      u.addChannel(command.substring(6, command.length()));
                      System.out.println("Successfully joined " + command.substring(6, command.length()));
                  } else {
                      System.out.println("Join channel failed");
                  }

              }
              else
              {
                  System.out.println("Wrong format");
              }
          } else if (command.contains("/LEAVE") && !u.isEmpty()) {
              if (command.length() >= 8 && command.charAt(6) == ' ') {
                  if (client.leaveGroup(u.getName(), command.substring(7, command.length()))) {
                      u.removeChannel(command.substring(7, command.length()));
                      System.out.println("Successfully left " + command.substring(7, command.length()));
                  } else {
                      System.out.println("Leave channel failed");
                  }

              }
          }
          else if (command.length() >= 4 && command.charAt(0) == ('@') && !u.isEmpty()) {
              if (command.contains(" "))
              {
                  String channelname = command.substring(1,command.indexOf(' '));
                  String message = command.substring(command.indexOf(' ')+1,command.length());
                  if (!client.sendMessages(u.getName(),channelname,message))
                  {
                      System.out.println("No channel found");
                  }
              }
              else
              {
                  System.out.println("You didn't type the message");
              }
          }
          else if (!u.isEmpty()) {
              client.sendMessages(u.getName(),"broadcast",command);
          }
          command = sc.nextLine();
      }

    timer.cancel();
    timer.purge();
    client.shutdown();
  }
}
