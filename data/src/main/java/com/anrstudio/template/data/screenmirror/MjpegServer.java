package com.anrstudio.template.data.screenmirror;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MjpegServer implements Runnable {
    public static int port = 2609;

    private static final boolean allIpsAllowed = false;
    private Socket socket;


    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        ServerSocket server;
        try {
            server = new ServerSocket(port);
            server.setSoTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                socket = server.accept();
                if (allIpsAllowed || socket.getInetAddress().isSiteLocalAddress()) {
                    MjpegSocket mjpegSocket = new MjpegSocket(socket);
                    new Thread(mjpegSocket).start();
                } else {
                    socket.close();
                }
            } catch (SocketTimeoutException ste) {
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (port != server.getLocalPort()) {
                try {
                    server.close();
                    server = new ServerSocket(port);
                    server.setSoTimeout(5000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public int getPort() {
        return port;
    }
}
