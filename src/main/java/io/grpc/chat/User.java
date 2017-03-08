package io.grpc.chat;
import com.google.protobuf.ByteString; 

import java.util.ArrayList;
import java.util.List;
/**
 * Created by irenenatalia on 2/21/17.
 */
public class User {

    private String name;
    private List<String> myChannels;
    private List<String> messQueue;
    //private List<String> fileQueue;
    private List<ByteString> fileQueue;

    //public List<String> getFileQueue() {
    public List<ByteString> getFileQueue() {
        return fileQueue;
    }
    public List<String> getMessQueue() {
        return messQueue;
    }

    //public void setMessQueue(List<String> messQueue, List<String> fileQueue) {
    public void setMessQueue(List<String> messQueue, List<ByteString> fileQueue) {
        this.messQueue = messQueue;
        this.fileQueue = fileQueue;
    }

    public User(String name) {
        this.name = name;
        this.myChannels = new ArrayList<String>();
        messQueue = new ArrayList<String>();
        //fileQueue = new ArrayList<String>();
        fileQueue = new ArrayList<ByteString>();
    }

    public User() {
        this.name = "";
        this.myChannels = new ArrayList<String>();
        messQueue = new ArrayList<String>();
        //fileQueue = new ArrayList<String>();
        fileQueue = new ArrayList<ByteString>();
    }

    public String getName() {
        return name;
    }

    public String toString() { 
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMyChannels() {
        return myChannels;
    }

    public void setMyChannels(List<String> myChannels) {
        this.myChannels = myChannels;
    }

    public void addChannel(String channel) {
        this.myChannels.add(channel);
    }

    public void removeChannel (String channel)
    {
        this.myChannels.remove(this.myChannels.indexOf(channel));
    }
    public boolean isEmpty()
    {
        return this.name.isEmpty();
    }
    //public void addFile(String file)
    public void addFile(ByteString file)
    {
        fileQueue.add(file);
    }
    //public String getFile(int index){
    public ByteString getFile(int index){
        return fileQueue.remove(index);
    }
    //public String getFirstFile()
    public ByteString getFirstFile()
    {
        return fileQueue.remove(0);
    }
    public void addMessage(String message)
    {
        messQueue.add(message);
    }
    public String getAllMessage()
    {
        StringBuilder result = new StringBuilder();
        while (!messQueue.isEmpty())
        {
            result.append(messQueue.remove(0)).append("\n");
        }
        return result.toString();
    }
}
