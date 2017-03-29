/*
 * Author: Irene Hardjono
 * Date: 03/01/2017
 * ChatClient.java
 */

package io.grpc.chat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import com.google.protobuf.ByteString; 

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
/**
 * A simple client that requests a chat from the {@link ChatServer}.
 */
public class ChatClient {
  private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
	private final ManagedChannel channel;
	private final ChatterGrpc.ChatterBlockingStub blockingStub;
	public static String messString = new String();
	
  /** Construct client connecting to client server at {@code host:port}. */
	public ChatClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port)
		// Channels are secure by default (via SSL/TLS). 
		// For the example we disable TLS to avoid
			// needing certificates.
		  .usePlaintext(true)
			.build();
	  blockingStub = ChatterGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
	  channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	//Stub functions
	public String createUsername(String name) {
    mName req;
    mString resp;
		try {
      req = mName.newBuilder().setName(name).build();
      resp = blockingStub.createUsername(req);
		} catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return e.getStatus().toString();
		}

		return resp.getValue();
	}
  
  public boolean removeUsername(String name) {
		mName req = mName.newBuilder().setName(name).build();
	  mBoolean resp = blockingStub.removeUsername(req);
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

  public boolean sendFiles(String name, ByteString byteFile) {
    mNameFile req;
    mBoolean resp;
    try {
      req = mNameFile.newBuilder().setName(name).setFile(byteFile).build();
		  resp = blockingStub.sendFiles(req);
		} catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return false;
    }
		return resp.getValue();
	}

  public ByteString getFile(String name) {
    mName req = mName.newBuilder().setName(name).build();
    mByte resp = blockingStub.getFile(req);
    return resp.getValue();
  }

  public boolean sendBigFiles(String name, ByteString byteFile) {
    mNameFile req;
    mBoolean resp;
    try {
      req = mNameFile.newBuilder().setName(name).setFile(byteFile).build();
		  resp = blockingStub.sendBigFiles(req);
		} catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return false;
    }
		return resp.getValue();
	}

	/**
	* Chat server. If provided, the first element of {@code args} is the name to use in the
	* greeting.
	*/
  public static int count = 1;
  public static void main(String[] args) throws Exception {
    
    System.out.println("Welcome to Chat Room! Please press Enter to continue...");
    
    /* Access a service running on the local machine on port 50051 */
    final ChatClient client = new ChatClient("localhost", 50051);
    final User u = new User();
    Scanner sc = new Scanner(System.in);
    String command = sc.nextLine();
    boolean isUnknown = true;
    Timer timer = new Timer();
    TimerTask doAsynchronousTask = new TimerTask() {
      @Override
      public void run() {
        if (!u.isEmpty())
        {
          messString =  client.getMessage(u.getName());
          if (!messString.isEmpty()){
            System.out.print(messString);
            System.out.print(u.getName() + " > ");
          }
				}
			}
	};
	timer.schedule(doAsynchronousTask, 0, 1000); //execute in every 100 ms
	try {
      command = command.toLowerCase();
			while (!command.equals("/exit")) {
      	Runtime.getRuntime().addShutdownHook(new Thread() {
          @Override
          public void run(){
            client.removeUsername(u.getName());	
       		}
       	});
      	if(isUnknown) {
          System.out.print("Please enter your username: ");
       		u.setName(client.createUsername(sc.nextLine()));
       		if(!u.getName().isEmpty()) {
            System.out.println("Successfully created nickname " + u.getName());	  														
          }
       		isUnknown = false;
      	} else if(command.startsWith("/upload")) {
          	byte[] fileInBytes;// = new byte[];
          	try {
            	String filename = command.substring(command.indexOf(" ") + 1, command.length()).trim();
            	//System.out.println("filename is " + filename);
              if(filename.isEmpty() || !command.contains(" ")){
            	  System.err.println("Error Locating file -- check path or filename. Format is /upload [filename]");
              } else {
            	  fileInBytes = Files.readAllBytes(Paths.get(filename));
            	  ByteString byteString = ByteString.copyFrom(fileInBytes);
            	  System.out.println(u.getName() + " > uploading " + filename + " size of " 
								  + byteString.size());
            	  client.sendFiles(u.getName(), byteString);
            	  client.sendMessages(u.getName(), "broadcast", "Sending a file with a size of " 
								  + client.getFile(u.getName()).size());
              }
          	} catch (IOException e) {
            	System.err.println(e);
            	System.err.println("Error Locating file -- check path.");
          	}  
      		} else if (!u.isEmpty()) {
          	client.sendMessages(u.getName(), "broadcast", command);
      		}
      	System.out.print(u.getName() + " > ");
    		command = sc.nextLine();
			}
		}
		finally {
    	client.removeUsername(u.getName());
			timer.cancel();
			timer.purge();
			client.shutdown();
		}
  }
}
