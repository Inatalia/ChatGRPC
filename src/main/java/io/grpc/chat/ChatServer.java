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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class ChatServer {
  private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

  /* The port on which the server should run */
  private int port = 50051;
  private Server server;

  private void start() throws IOException {
    server = ServerBuilder.forPort(port)
        .addService(new ChatterImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        ChatServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final ChatServer server = new ChatServer();
    server.start();
    server.blockUntilShutdown();
  }

  private class ChatterImpl extends ChatterGrpc.ChatterImplBase {
    public List<String> defaultUsernames;
    public List<String> activeUsers;
    public List<Channel> activeChannels;
    public List<User> broadcastChannels;
    public User currentUser;

    public ChatterImpl() {
      defaultUsernames = new ArrayList<String>(
              Arrays.asList("A", "B", "C", "D", "E")
      );
      activeUsers = new ArrayList<String>();
      activeChannels = new ArrayList<Channel>();
      currentUser = new User();
      broadcastChannels = new ArrayList<User>();
    }

    //Utility functions
    private boolean isUsernameExists(String name) {
      for (int i = 0; i < activeUsers.size(); i++) {
        if (name.equals(activeUsers.get(i))) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void sendMessage(ChatRequest req, StreamObserver<ChatReply> responseObserver) {
      ChatReply reply = ChatReply.newBuilder().setMessage(req.getMessage()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void createUsername(mName request, StreamObserver<mString> responseObserver) {
      String name = request.getName();
      String finalname = "";
      System.out.println("calling username from " + name);
      String finalName = "";
      if (name.equals("")) { //kasus random username, diasumsikan masih ada nama yang tersedia
        int rndIdx = new Random().nextInt((defaultUsernames.size() - 0));
        String potentialName = defaultUsernames.get(rndIdx);
        while (isUsernameExists(potentialName)) {
          rndIdx = new Random().nextInt((defaultUsernames.size() - 0));
          potentialName = defaultUsernames.get(rndIdx);
        }
        defaultUsernames.remove(rndIdx);
        finalName = potentialName;
      } else {
        if (isUsernameExists(name)) {
          mString fn = mString.newBuilder().setValue(finalName).build();
          responseObserver.onNext(fn);
          responseObserver.onCompleted();
          return;
        } else {
          finalName = name;
        }
      }
      activeUsers.add(finalName);
      currentUser.setName(finalName);
      broadcastChannels.add(new User(finalName));
      mString fn = mString.newBuilder().setValue(finalName).build();
      responseObserver.onNext(fn);
      responseObserver.onCompleted();
    }

    @Override
    public void joinGroup(mNameGroup request, StreamObserver<mBoolean> responseObserver) {
      if (request.getName().isEmpty() || request.getGroup().isEmpty()) {
        mBoolean resp = mBoolean.newBuilder().setValue(false).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        return;
      }
      String name = request.getName();
      String channel = request.getGroup();
      System.out.println("calling join from " + name);
      int i = 0;
      while (i < activeChannels.size()) {
        if (activeChannels.get(i).getName().compareToIgnoreCase(channel) == 0) {
          System.out.println(name + " successfully joined " + channel);
          activeChannels.get(i).addActiveUser(name);
          currentUser.addChannel(channel);
          mBoolean resp = mBoolean.newBuilder().setValue(true).build();
          responseObserver.onNext(resp);
          responseObserver.onCompleted();
          return;
        } else {
          i++;
        }
      }
      Channel c = new Channel(channel);
      c.addActiveUser(name);
      activeChannels.add(c);
      currentUser.addChannel(channel);
      System.out.println(name + " successfully joined " + channel);

      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
    public void leaveGroup(mNameGroup request, StreamObserver<mBoolean> responseObserver) {
      String name = request.getName();
      String channel = request.getGroup();
      if (name.isEmpty() || channel.isEmpty()) {
        mBoolean resp = mBoolean.newBuilder().setValue(false).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        return;
      }
      System.out.println("calling leave from " + name);
      int i = 0;
      while (i < activeChannels.size()) {
        if (activeChannels.get(i).getName().compareToIgnoreCase(channel) == 0) {
          System.out.println("> "+ activeChannels.get(i).getName());
          for(int j=0; j<currentUser.getMyChannels().size(); j++) {
            System.out.println(currentUser.getMyChannels().get(j));
          }
          activeChannels.get(i).removeActiveUser(name);
          currentUser.removeChannel(channel);
          System.out.println(name + " successfully left " + channel);
          mBoolean resp = mBoolean.newBuilder().setValue(true).build();
          responseObserver.onNext(resp);
          responseObserver.onCompleted();
          return;
        } else {
          i++;
        }
      }
      System.out.println("Channel not found");

      mBoolean resp = mBoolean.newBuilder().setValue(false).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
    public void sendMessages(mNameGroupMsg request, StreamObserver<mBoolean> responseObserver) {
      String name = request.getName();
      String channel = request.getGroup();
      String message = request.getMessage();
      String mesString = new String("[" + channel + "] " + "(" + name + ") " + message);
      System.out.println(name + " sending " + message + " to " + channel);
      int i = 0;
      boolean foundChannel = false;
      System.out.println("channel msg " + channel);
      if (!channel.equals("broadcast")) {
        while (i < activeChannels.size() && !foundChannel) {
          if (activeChannels.get(i).getName().equals(channel)) {
            foundChannel = true;
            for (int j = 0; j < activeChannels.get(i).activeUser.size(); j++) {
              activeChannels.get(i).activeUser.get(j).addMessage(mesString);
            }
          }
          i++;
        }
        mBoolean resp = mBoolean.newBuilder().setValue(foundChannel).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
      } else {
        System.out.println("broadcast channels: " + broadcastChannels.size());
        for (int j = 0; j < broadcastChannels.size(); j++) {
          System.out.println("broadcast msg " + mesString);
          broadcastChannels.get(j).addMessage(mesString);
        }
        if(broadcastChannels.isEmpty())
          System.out.println("broadcast msg " + mesString);
      }
      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
    public void getMessage(mName request, StreamObserver<mString> responseObserver) {
      String name = request.getName();
      StringBuilder msgBuilder = new StringBuilder();
      for (int i = 0; i < activeChannels.size(); i++) {
        for (int j = 0; j < activeChannels.get(i).activeUser.size(); j++) {
          if (activeChannels.get(i).activeUser.get(j).getName().equals(name)) {
            if (!activeChannels.get(i).activeUser.get(j).getMessQueue().isEmpty()) {
              msgBuilder.append(activeChannels.get(i).activeUser.get(j).getAllMessage());
            }
          }
        }
      }
      for (int j = 0; j < broadcastChannels.size(); j++) {
        if (broadcastChannels.get(j).getName().equals(name)) {
          //                    System.out.println("broad = "+broadcastChannels.get(j).getAllMessage());
          if (!broadcastChannels.get(j).getMessQueue().isEmpty()) {
            //                    System.out.println("broad = "+broadcastChannels.get(j).getAllMessage());
            msgBuilder.append(broadcastChannels.get(j).getAllMessage());
          }
        }
      }
      mString reply = mString.newBuilder().setValue(msgBuilder.toString()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

  }
}
