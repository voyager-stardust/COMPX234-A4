import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.*;
public class server
{
    private static final int MIN_DATA_PORT = 50000;
    private static final int MAX_DATA_PORT = 50010;
    private static final int BUFFER_SIZE = 2048;
    private static final int MAX_THREADS = 50;
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: java server <port>");
            return;
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        try (DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0])))
        {
            System.out.println("Server started " + args[0]);
            while (true)
            {
                try
                {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(packet);
                    String request = new String(packet.getData(),0, packet.getLength()).trim();
                    if (request.startsWith("DOWNLOAD"))
                    {
                        String filename = request.substring(9).trim();
                        threadPool.execute(() ->
                        {
                            try
                            {
                                handleDownloadRequest(filename,packet.getAddress(),packet.getPort());
                            }
                            catch (IOException e)
                            {
                                System.err.println("error download");
                            }
                        });
                    }
                }
                catch (IOException e)
                {
                    System.err.println("error packet");
                }
            }
        }
        catch (SocketException e)
        {
            System.err.println("Could not start server: " + e.getMessage());
        }
        finally
        {
            threadPool.shutdown();
        }
    }
    private static void handleDownloadRequest(String filename,InetAddress clientAddress,int clientPort) throws IOException
    {
        File file = new File(filename);
        int dataPort = ThreadLocalRandom.current().nextInt(MIN_DATA_PORT, MAX_DATA_PORT + 1);
        try (DatagramSocket dataSocket = new DatagramSocket(dataPort);RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            System.out.println("Starting transfer "+filename+" to "+ clientAddress + ":" + clientPort + " on port " + dataPort);
            sendResponse(clientAddress, clientPort,String.format("%s SIZE %d PORT %d",filename, file.length(), dataPort));
            handleDataTransfer(dataSocket, raf, filename, clientAddress, clientPort);
        }
        catch (IOException e)
        {
            System.err.println("File error");
            sendResponse(clientAddress, clientPort, "ERR " + filename + " TRANSFER_ERROR");
        }
    }
    private static void handleDataTransfer(DatagramSocket socket,RandomAccessFile file, String filename,InetAddress clientAddress, int clientPort) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true)
        {
            try
            {
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] tokens = request.split(" ");
                if (tokens.length >= 3 && tokens[0].equals("FILE")&& tokens[2].equals("CLOSE"))
                {
                    sendResponse(clientAddress, clientPort,"FILE " + filename + " CLOSE_OK");
                    System.out.println(filename + " for " + clientAddress);
                    break;
                }
                if (tokens.length >= 7 && tokens[0].equals("FILE") && tokens[2].equals("GET"))
                {
                    long start = Long.parseLong(tokens[4]);
                    long end = Long.parseLong(tokens[6]);
                    int size = (int)(end - start + 1);
                    byte[] fileData = new byte[size];
                    file.seek(start);
                    file.read(fileData);
                    String response = String.format("FILE %s START %d END %d DATA %s",filename,start,end,Base64.getEncoder().encodeToString(fileData));
                    sendResponse(clientAddress, clientPort, response);
                }
            }
            catch (IOException e)
            {
                System.err.println("Error during transfer");
                sendResponse(clientAddress, clientPort,"FILE "+filename+" ERROR");
                break;
            }
        }
    }
    private static void sendResponse(InetAddress address, int port, String message) throws IOException
    {
        try (DatagramSocket tempSocket = new DatagramSocket())
        {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            tempSocket.send(packet);
        }
    }
}