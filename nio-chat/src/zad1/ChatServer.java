
package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer{
    private static final int BUFFER_SIZE = 512;
    private static final int MIN_ID_SIZE = 2;
    public static final int MAX_ID_SIZE = 128;

    private final StringBuilder serverLogBuilder;

    private Selector selector;
    private final InetSocketAddress serverAddress;
    private ServerSocketChannel serverChannel;
    private Thread serverThread;
    private final Map<SocketChannel, String> clients;

    public ChatServer(String host, int port){
        serverLogBuilder = new StringBuilder();
        clients = new HashMap<>();
        serverAddress = new InetSocketAddress(host,port);
    }


    private void startListening(){
        while(!serverThread.isInterrupted()){

            try {
                selector.select();
            }catch (IOException ioException){
                ioException.printStackTrace();
            }

            if(serverThread.isInterrupted()) break;

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while(keys.hasNext()){
                SelectionKey key = keys.next();
                keys.remove();

                if(key.isAcceptable()){
                    acceptClient();
                }

                if(key.isReadable()){
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    synchronized (this) {
                        String[] requests = read(clientChannel);
                        for (String s : requests) {
                            String request = s.replaceAll("\\$", "");
                            processRequest(request, clientChannel);
                        }
                    }
                }
            }
        }
    }

    private void acceptClient(){
        try{
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }
    
    private String[] read(SocketChannel readableChannel){
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder requestBuilder = new StringBuilder(); //in case request size is greater than buffer size
        
        try {
            while(readableChannel.read(buffer) > 0){
                buffer.flip();
                buffer.clear();
                requestBuilder.append(new String(buffer.array(), StandardCharsets.UTF_8).trim());
                
            }
        }catch (IOException ioException){
            sendToAllClients("*** "+ioException.getMessage());
        }
        return requestBuilder.toString().split("\\$");
    }

    private void processRequest(String req, SocketChannel clientChannel){
        Matcher loginMatcher = Pattern.compile(String.format("^login \\w{%d,%d}$", MIN_ID_SIZE,MAX_ID_SIZE),Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(req);
        Matcher logoutMatcher = Pattern.compile("logout",Pattern.CASE_INSENSITIVE).matcher(req);
        Matcher msgMatcher = Pattern.compile(".+$",Pattern.UNICODE_CHARACTER_CLASS).matcher(req);

        if(loginMatcher.matches()){
            String clientId = req.substring(6);
            clients.put(clientChannel,clientId);
            serverLogBuilder.append(String.format("%s %s logged in", LocalTime.now(),clientId)).append("\n");
            sendToAllClients(String.format("%s logged in", clientId));
        }
        else if(logoutMatcher.matches()){
            String clientId = clients.get(clientChannel);
            serverLogBuilder.append(String.format("%s %s logged out", LocalTime.now(),clientId)).append("\n");
            sendToAllClients(String.format("%s logged out", clientId));
            clients.remove(clientChannel);
        }
        else if(msgMatcher.matches()){
            String clientId = clients.get(clientChannel);
            serverLogBuilder.append(String.format("%s %s: %s", LocalTime.now(),clientId,req)).append("\n");
            sendToAllClients(String.format("%s: %s",clientId,req));
        }

    }

    private void sendToAllClients(String message){
        for(Map.Entry<SocketChannel, String> entry : clients.entrySet()){
            try{
                entry.getKey().write(ByteBuffer.wrap((message+'$').getBytes(StandardCharsets.UTF_8)));
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void initiateServer(){
        try{
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel.socket().bind(serverAddress);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started\n");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startServer(){
        serverThread = new Thread(() ->{
            initiateServer();
            startListening();
        });
        serverThread.start();
    }

    public void stopServer(){
        try{
            serverThread.interrupt();
            serverChannel.close();
            selector.close();
            System.out.println("Server stopped");
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    public String getServerLog(){
        return serverLogBuilder.toString();
    }
}
