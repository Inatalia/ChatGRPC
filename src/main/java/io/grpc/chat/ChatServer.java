/*
 * Author: Irene Hardjono 
 * Date: 03/01/2017
 * ChatServer.java
 */

package io.grpc.chat;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString; 

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
    public List<User> broadcastChannels;
    public User currentUser;

    public ChatterImpl() {
      defaultUsernames = new ArrayList<String>(
              Arrays.asList("A", "B", "C", "D", "E")
      );
      activeUsers = new ArrayList<String>();
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
    public void createUsername(mName request, StreamObserver<mString> responseObserver) {
      String name = request.getName();
      String finalname = "";
      System.out.println("created username from " + name);
      String finalName = "";
      
      if (name.equals("")) { 
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
    public void removeUsername(mName request, StreamObserver<mBoolean> responseObserver) {
      String name = request.getName();
      activeUsers.remove(name);
      broadcastChannels.remove(new User(name));

      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
    public void sendFiles(mNameFile request, StreamObserver<mBoolean> responseObserver) {
      String name = request.getName();
	  	ByteString file = request.getFile();
      System.out.println("sent size of file is " + file.size());
      int i = 0;
      for (int j = 0; j < broadcastChannels.size(); j++) {
        broadcastChannels.get(j).addFile(file);
      }
      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
	  public void getFile(mName request, StreamObserver<mByte> responseObserver) {
      String name = request.getName();
	  	ByteString fileByteString = null;
      for (int j = 0; j < broadcastChannels.size(); j++) {
        if (broadcastChannels.get(j).getName().equals(name)) {
          List<ByteString> listFileByteString = broadcastChannels.get(j).getFileQueue();
          if (!listFileByteString.isEmpty()) {
            fileByteString = broadcastChannels.get(j).getFirstFile();
            System.out.println("file size is " + fileByteString.size());
          }
        }
      } 
      mByte reply = mByte.newBuilder().setValue(fileByteString).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  
    /*@Override
    public void sendBigFiles(mNameFile request, StreamObserver<mBoolean> responseObserver) {
      //....? 
      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }*/

    @Override
    public void sendMessages(mNameGroupMsg request, StreamObserver<mBoolean> responseObserver) {
      String name = request.getName();
      String channel = request.getGroup();
      String message = request.getMessage();
      String mesString = new String("(Message from " + name + " > " + message + ")");
      int i = 0;
      System.out.println("Active users: " + broadcastChannels.size());
      for (int j = 0; j < broadcastChannels.size(); j++) {
          System.out.println("send msg " + mesString);
          broadcastChannels.get(j).addMessage(mesString);
      }
      mBoolean resp = mBoolean.newBuilder().setValue(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
    }

    @Override
    public void getMessage(mName request, StreamObserver<mString> responseObserver) {
      String name = request.getName();
      StringBuilder msgBuilder = new StringBuilder();
      for (int j = 0; j < broadcastChannels.size(); j++) {
        if (broadcastChannels.get(j).getName().equals(name)) {
          if (!broadcastChannels.get(j).getMessQueue().isEmpty()) {
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
