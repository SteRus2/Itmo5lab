package us.obviously.itmo.prog.client;


import us.obviously.itmo.prog.client.exceptions.FailedToCloseConnection;
import us.obviously.itmo.prog.client.exceptions.FailedToReadRemoteException;
import us.obviously.itmo.prog.client.exceptions.FailedToSentRequestsException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Client implements ClientConnectionManager {
    private static InetAddress host;
    private static Socket connection;
    private ByteBuffer buffer;
    private InputStream is;
    private OutputStream os;
    private InetSocketAddress address;
    private boolean isActive;

    @Override
    public void run(int port) throws IOException {
        host = InetAddress.getLocalHost();
        connection = new Socket();
        address = new InetSocketAddress(host, port);
        connection.connect(address, 1000);
        activeClient();
    }

    @Override
    public void write(ByteBuffer data) throws IOException {
        os = connection.getOutputStream();
        os.write(data.array());
    }

    @Override
    public ByteBuffer read() throws IOException {
        is = connection.getInputStream();
        is.read(buffer.array());
        return buffer;
    }
    void activeClient(){
        this.isActive = true;
        System.out.println("Клиент запущен!");
    }
    void deactivateClient(){
        this.isActive = false;
        System.out.println("Клиент закрыт!");
    }

    @Override
    public String waitResponse() throws FailedToReadRemoteException {
        ByteBuffer byteBuffer;
        try {
            byteBuffer = read();
        } catch (IOException e) {
            throw new FailedToReadRemoteException("Не удается получить данные с сервера");
        }
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    @Override
    public void request(String myRequest) throws FailedToSentRequestsException {
        buffer = ByteBuffer.wrap(myRequest.getBytes());
        try {
            write(buffer);
        } catch (IOException e) {
            throw new FailedToSentRequestsException("Не удалось отправить запрос");
        }
    }

    @Override
    public void stop() throws FailedToCloseConnection {
        deactivateClient();
        try {
            connection.close();
        } catch (IOException e) {
            throw new FailedToCloseConnection("Закрыть соединение с сервером по какой то причине не получилось, будем вверить что вашего самообладания хватило на то, чтобы закрыть подключение самостоятельно, иначе, будем считать, что подключения уже нет");
        }
    }
}
