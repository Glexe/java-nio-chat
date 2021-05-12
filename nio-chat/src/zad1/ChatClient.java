
package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ChatClient{
    public static final int BUFFER_SIZE = 128;
    private final String id;
    private final InetSocketAddress serverAddress;
    private SocketChannel clientChannel;

    private Thread listeningThread;
    private Selector selector;
    private final StringBuilder chatViewBuilder;


    public ChatClient(String host, int port, String id){
        serverAddress = new InetSocketAddress(host,port);
        chatViewBuilder = new StringBuilder();
        this.id = id;
    }

    public void login(){
        try{
            if(clientChannel==null){
                selector = Selector.open();
                clientChannel = SocketChannel.open(serverAddress);
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);
            }

            send("login "+id);
            startListening();

        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void startListening(){
        listeningThread = new Thread(()->{

            while (!listeningThread.isInterrupted()) {
                try {
                    if (selector.select() == 0) continue;
                }catch (IOException ioException){
                    ioException.printStackTrace();
                }

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        String[] answers = read(channel);
                        for(String answer:answers){
                            answer = answer.replaceAll("\\$","");
                            chatViewBuilder.append(answer).append("\n");
                            if(answer.equals(String.format("%s logged out",id))) listeningThread.interrupt();
                        }
                        
                    }
                }
            }
        });
        listeningThread.start();
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
        }catch (IOException e){
            e.printStackTrace();
        }

        return requestBuilder.toString().split("\\$");
    }

    public void logout(){
        try {
            send("logout");
        }catch (NullPointerException nullClientException){
            nullClientException.printStackTrace();
        }
    }
    
    public void send(String req){
        try{
            Thread.sleep(100);
            byte[] encodedBytes = (req+'$').getBytes(StandardCharsets.UTF_8);
            clientChannel.write(ByteBuffer.wrap(encodedBytes));
            Thread.sleep(100);
        }catch (IOException | InterruptedException ioException){
            ioException.printStackTrace();
        }
    }

    public String getChatView(){
        return String.format("=== %s chat view\n%s",id,chatViewBuilder);
    }
}
