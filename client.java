import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;
public class client
{
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_TIMEOUT = 1000;
    private static final int BLOCK_SIZE = 1000;
    private static final int BUFFER_SIZE = 2048;
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.out.println("Usage: java UDPclient <hostname> <port> <filelist>");
            return;
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String fileList = args[2];
        try (DatagramSocket socket = new DatagramSocket();BufferedReader br = new BufferedReader(new FileReader(fileList)))
        {
            socket.setSoTimeout(INITIAL_TIMEOUT);
            String filename;
            while ((filename = br.readLine()) != null)
            {
                filename = filename.trim();
                if (!filename.isEmpty())
                {
                    downloadFile(socket, hostname, port, filename);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Client error");
        }
    }
    private static void downloadFile(DatagramSocket socket, String hostname,int serverPort, String filename) throws IOException
    {
        InetAddress serverAddress = InetAddress.getByName(hostname);
        String downloadRequest = "DOWNLOAD " + filename;
        String response = sendWithRetry(socket, serverAddress, serverPort, downloadRequest);
        if (response == null)
        {
            System.out.println("timeout");
            return;
        }
        String[] tokens = response.split(" ");
        if (tokens[0].equals("ERR"))
        {
            System.out.println("Server error");
            return;
        }
        long fileSize = Long.parseLong(tokens[3]);
        int dataPort = Integer.parseInt(tokens[5]);
        try (RandomAccessFile file = new RandomAccessFile(filename, "rw"))
        {
            System.out.printf("Downloading %s (Size: %d bytes)", filename, fileSize);
            long bytesReceived = 0;
            while (bytesReceived < fileSize)
            {
                long start = bytesReceived;
                long end = Math.min(start + BLOCK_SIZE - 1, fileSize - 1);
                String blockRequest = String.format("FILE %s GET START %d END %d", filename, start, end);
                String blockResponse = sendWithRetry(socket, serverAddress, dataPort, blockRequest);
                if (blockResponse == null)
                {
                    System.out.println("request timeout");
                    return;
                }
                if (!processDataBlock(blockResponse, file))
                {
                    System.out.println("Error");
                    return;
                }
                bytesReceived = end+1;
                showProgress(bytesReceived, fileSize);
            }
            String closeResponse = sendWithRetry(socket, serverAddress, dataPort,"FILE " + filename + " CLOSE");
            if (closeResponse != null && closeResponse.equals("FILE " + filename + " CLOSE_OK"))
            {
                System.out.printf("Download completed:",filename,bytesReceived);
            }
            else
            {
                System.out.printf("Without confirmation:", filename, bytesReceived);
            }
        }
    }
    private static String sendWithRetry(DatagramSocket socket, InetAddress address,int port, String message) throws IOException
    {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        byte[] receiveData = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        int currentTimeout = INITIAL_TIMEOUT;
        int attempt = 0;
        while (attempt < MAX_RETRIES)
        {
            try
            {
                socket.send(sendPacket);
                socket.setSoTimeout(currentTimeout);
                socket.receive(receivePacket);
                return new String(receivePacket.getData(),0, receivePacket.getLength()).trim();
            }
            catch (SocketTimeoutException e)
            {
                attempt++;
                System.out.printf("Timeout");
                currentTimeout *= 2;
            }
        }
        return null;
    }
    private static boolean processDataBlock(String response, RandomAccessFile file) throws IOException
    {
        String[] parts = response.split(" DATA ");
        if (parts.length < 2)
        {
            return false;
        }
        try
        {
            String[] headerParts = parts[0].split(" ");
            long start = Long.parseLong(headerParts[4]);
            byte[] fileData = Base64.getDecoder().decode(parts[1]);
            file.seek(start);
            file.write(fileData);
            return true;
        }
        catch (Exception e)
        {
            System.err.println("error");
            return false;
        }
    }
    private static void showProgress(long bytesReceived, long fileSize)
    {
        int progress = (int)((double)bytesReceived/fileSize*50);
        System.out.print("[");
        for (int i = 0; i < 50; i++)
        {
            System.out.print(i < progress ? "=" : " ");
        }
        System.out.printf("] %d%% (%d/%d bytes)",(int)((double)bytesReceived/fileSize*100),bytesReceived,fileSize);
    }
}