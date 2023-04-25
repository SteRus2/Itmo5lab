package us.obviously.itmo.prog.server.net;


import us.obviously.itmo.prog.client.console.ConsoleColor;
import us.obviously.itmo.prog.client.console.Messages;
import us.obviously.itmo.prog.client.exceptions.FailedToReadRemoteException;
import us.obviously.itmo.prog.client.exceptions.FailedToSentRequestsException;
import us.obviously.itmo.prog.client.exceptions.IncorrectValueException;
import us.obviously.itmo.prog.common.actions.Action;
import us.obviously.itmo.prog.common.actions.Request;
import us.obviously.itmo.prog.common.actions.Response;
import us.obviously.itmo.prog.common.actions.ResponseStatus;
import us.obviously.itmo.prog.common.data.DataCollection;
import us.obviously.itmo.prog.server.ActionManager;
import us.obviously.itmo.prog.server.exceptions.*;
import us.obviously.itmo.prog.server.serverCommands.ServerCommand;
import us.obviously.itmo.prog.server.serverCommands.ServerCommandManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server implements ServerConnectionManager {
    private final int DATA_SIZE = 1024;
    private ServerSocketChannel server;
    private SocketAddress address;
    private final Scanner serverCommandReader;
    private boolean isActive;
    private final DataCollection data;
    private final Selector selector;
    private Set<SelectionKey> selectionKeySet;
    private ServerCommandManager serverCommandManager;
    private ActionManager actionManager;
    private Map<SocketChannel, ArrayList<Request>> clientSocketMap;

    {
        ConsoleColor.initColors();
        serverCommandReader = new Scanner(System.in);
        clientSocketMap = new HashMap<>();
    }
    public Server(DataCollection dataCollection, int port) throws FailedToStartServerException {
        this.data = dataCollection;
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
            activeServer();
            serverCommandManager = new ServerCommandManager(this);
            actionManager = new ActionManager();
        } catch (ClosedChannelException e){
            throw new FailedToStartServerException("Возникла ошибка при старте сервера, сетевой канал закрыт, попробуйте позже");
        } catch (IOException e){
            throw new FailedToStartServerException("Возникла ошибка при старте сервера, пожалуйста, попробуйте позднее");
        }
        try {
            address = new InetSocketAddress(port);
            server.bind(address);
        } catch (IOException e) {
            throw new FailedToStartServerException("Данный порт занят");
        }
        new Thread(){
            @Override
            public void run() {
                while (serverCommandReader.hasNextLine()) {
                    String commandString = serverCommandReader.nextLine();
                    ServerCommand command = serverCommandManager.getCommand(commandString);
                    if (command == null) {
                        System.out.println("Команды не существует");
                    } else {
                        command.execute();
                    }
                }
            }
        }.start();
    }
    @Override
    public void run() {
        while (isActive){
            try {
                int nowChannels = selector.select(1000);
                selectionKeySet = selector.selectedKeys();
                for (var iter = selectionKeySet.iterator(); iter.hasNext(); ){
                     SelectionKey key = iter.next();
                     iter.remove();
                     if (key.isValid()){
                         if(key.isAcceptable()){
                            acceptClient(key);
                             System.out.println(clientSocketMap.toString());
                         }
                         if(key.isReadable()){
                             SocketChannel socketChannel = (SocketChannel) key.channel();
                             Request request = readRequest(socketChannel);
                            if (clientSocketMap.containsKey((SocketChannel) key.channel())){
                                clientSocketMap.get((SocketChannel) key.channel()).add(request);
                                socketChannel.register(key.selector(), SelectionKey.OP_WRITE);
                            }
                            System.out.println(clientSocketMap.toString());

                         }
                         if (key.isWritable()){
                             SocketChannel socketChannel = (SocketChannel) key.channel();
                             if (clientSocketMap.containsKey(socketChannel)){
                                 var requests = clientSocketMap.get(socketChannel);
                                 Request request = requests.get(requests.size() - 1);
                                 requests.remove(requests.size() - 1);
                                 Action action = actionManager.getAction(request.getCommand());
                                 System.out.println(action);
                                 Response response;
                                 if (action == null){
                                     response = new Response("Действие не найдено", ResponseStatus.NOT_FOUND);
                                 } else {
                                     response = action.run(data, request.getBody());
                                 }
                                 System.out.println(response.toString());
                                 giveResponse(response, socketChannel);
                                 if (requests.isEmpty()){
                                     socketChannel.register(key.selector(), OP_READ);
                                 }
                             }
                         }
                     } else {
                         key.cancel();
                     }
                }
            } catch (IOException e) {
                Messages.printStatement("~yeUnknown io exception "+ Arrays.toString(e.getStackTrace()) + "~=");
            } catch (FailedToAcceptClientException e) {
                Messages.printStatement("~reНе получилось присоединить клиента: " + e.getMessage() + "~=");
            } catch (ClassNotFoundException e) {
                Messages.printStatement("~reНе удалось получить Приказ клиента~=");
            } catch (UsedKeyException e) {
                throw new RuntimeException(e);
            } catch (IncorrectValuesTypeException e) {
                throw new RuntimeException(e);
            } catch (IncorrectValueException e) {
                throw new RuntimeException(e);
            } catch (FileNotWritableException e) {
                throw new RuntimeException(e);
            } catch (CantParseDataException e) {
                throw new RuntimeException(e);
            } catch (CantWriteDataException e) {
                throw new RuntimeException(e);
            } catch (NoSuchIdException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void acceptClient(SelectionKey selectionKey) throws FailedToAcceptClientException {
        var clientSocket = (ServerSocketChannel) selectionKey.channel();
        try {
            var client = clientSocket.accept();
            clientSocketMap.put( client, new ArrayList<>());
            client.configureBlocking(false);
            client.register(selectionKey.selector(), OP_READ);
            System.out.println("Новый клиент: " + client.getRemoteAddress());
        } catch (IOException e) {
            throw new FailedToAcceptClientException("Во время соединения с клиентом произошла ошибка");
        }
    }

    private Request readRequest(SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        ByteBuffer buf = ByteBuffer.allocate(DATA_SIZE);
        try {
            socketChannel.read(buf);
        } catch (SocketException e) {
            socketChannel.close();
            System.out.println("Клиент отключен");
        }
        return deserializeRequest(buf);
    }

    private Request deserializeRequest(ByteBuffer buf) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf.array());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return (Request) objectInputStream.readObject();
    }
    private ByteBuffer serializeResponse(Response response){
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(response);
            return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
            //TODO exception
        }
    }

    @Override
    public ByteBuffer read() throws IOException {

        return ByteBuffer.allocate(1024);
    }

    @Override
    public void write(ByteBuffer data) throws IOException {

    }

    @Override
    public void waitRequest() throws FailedToReadRemoteException, FailedToSentRequestsException {

        /*try {
            System.out.println(123);
            byteBuffer.clear();
            byteBuffer = read();
            System.out.println(234);
        } catch (IOException e) {
            throw new FailedToReadRemoteException("Сервер не может получить данные от Клиента. Он сам виновать.");
        }
        String result = UTF_8.decode(byteBuffer).toString();
        System.out.println(result + " - from server");
        //giveResponse(result + " - from server");
        byteBuffer.clear();*/
    }

    @Override
    public void giveResponse(Response response, SocketChannel socketChannel) throws IOException {
        socketChannel.write(serializeResponse(response));
    }

    public void activeServer(){
        this.isActive = true;
        System.out.println("Сервер открыт!");
    }
    public void deactivateServer() throws FailedToCloseServerException {
        this.isActive = false;
        try {
            server.close();
        } catch (IOException e) {
            throw new FailedToCloseServerException("Возникла непредвиденная ошибка при закрытии сервера");
        }
        System.out.println("Сервер закрыт!");
        System.exit(0);

    }
}
