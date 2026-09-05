package com.github.squi2rel.mcft.services;

import com.github.squi2rel.mcft.MCFT;
import com.github.squi2rel.mcft.MCFTClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public class HTTP {
    private static final Object lifecycleLock = new Object();
    private static final Object lock = new Object();
    private static final String HTTP_LOOPBACK_HOST = "127.0.0.1";
    private static final int SOCKET_READ_TIMEOUT = 5000;
    private static final int MAX_HEADER_BYTES = 16384;
    // Minecraft dropped Jackson from its library set in 26.1, so the OSCQuery documents are built with Gson,
    // which ships with the game. The emitted JSON is unchanged.
    private static final Gson GSON = new Gson();
    public static final int port = MCFTClient.config.httpPort;
    private static ServerSocket serverSocket;
    private static Thread httpThread;

    public static void init() {
        synchronized (lifecycleLock) {
            initServices();
        }
    }

    private static void initServices() {
        try {
            OSC.init();
        } catch (Exception e) {
            MCFT.LOGGER.error("OSC start failed", e);
        }
        if (Minecraft.getInstance().getUser().getProfileId() == null) {
            MCFT.LOGGER.warn("OSCQuery start skipped without a session UUID");
            return;
        }
        try {
            createInfo();
        } catch (Exception e) {
            MCFT.LOGGER.error("OSC avatar info creation failed", e);
        }
        boolean httpStarted = false;
        try {
            httpStarted = startServer();
        } catch (Exception e) {
            MCFT.LOGGER.error("HTTP start failed", e);
        }
        if (httpStarted) {
            try {
                DNS.init();
            } catch (Exception e) {
                MCFT.LOGGER.error("DNS start failed", e);
            }
        }
    }

    public static void shutdown() {
        synchronized (lifecycleLock) {
            shutdownServices();
        }
    }

    private static void shutdownServices() {
        ServerSocket current;
        synchronized (lock) {
            current = serverSocket;
            serverSocket = null;
            httpThread = null;
        }
        if (current != null) {
            try {
                current.close();
            } catch (IOException e) {
                MCFT.LOGGER.error("Failed to close HTTP server", e);
            }
        }
        OSC.shutdown();
        DNS.shutdown();
    }

    private static boolean startServer() throws IOException {
        synchronized (lock) {
            if (serverSocket != null && !serverSocket.isClosed() && httpThread != null && httpThread.isAlive()) return true;
            requirePort(port);
            ServerSocket candidate = new ServerSocket();
            try {
                candidate.bind(new InetSocketAddress(InetAddress.getByName(HTTP_LOOPBACK_HOST), port));
                Thread thread = new Thread(() -> runServer(candidate));
                thread.setName("MCFT HTTP");
                thread.setDaemon(true);
                serverSocket = candidate;
                httpThread = thread;
                thread.start();
                MCFT.LOGGER.info("HTTP started on {}:{}", HTTP_LOOPBACK_HOST, port);
                return true;
            } catch (IOException | RuntimeException e) {
                if (serverSocket == candidate) {
                    serverSocket = null;
                    httpThread = null;
                }
                try {
                    candidate.close();
                } catch (IOException closeException) {
                    e.addSuppressed(closeException);
                }
                throw e;
            }
        }
    }

    private static void runServer(ServerSocket socket) {
        try {
            while (!socket.isClosed()) {
                Socket client;
                try {
                    client = socket.accept();
                } catch (IOException e) {
                    if (!socket.isClosed()) MCFT.LOGGER.error("HTTP accept failed", e);
                    break;
                }
                try (client) {
                    handleClient(client);
                } catch (SocketTimeoutException | SocketException ignored) {
                } catch (IOException e) {
                    MCFT.LOGGER.error("HTTP request failed", e);
                } catch (RuntimeException e) {
                    MCFT.LOGGER.error("HTTP request failed", e);
                }
            }
        } finally {
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            MCFT.LOGGER.error("Failed to close HTTP server", e);
        }
        boolean stoppedCurrent = false;
        synchronized (lock) {
            if (serverSocket == socket) {
                serverSocket = null;
                httpThread = null;
                stoppedCurrent = true;
            }
        }
        if (stoppedCurrent) DNS.shutdown();
    }

    private static void handleClient(Socket client) throws IOException {
        client.setSoTimeout(SOCKET_READ_TIMEOUT);
        readHeaders(client.getInputStream());
        String response = getString();
        OutputStream out = client.getOutputStream();
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void readHeaders(InputStream input) throws IOException {
        int third = -1;
        int second = -1;
        int previous = -1;
        for (int count = 0; count < MAX_HEADER_BYTES; count++) {
            int current = input.read();
            if (current == -1) return;
            if (previous == '\n' && current == '\n' || third == '\r' && second == '\n' && previous == '\r' && current == '\n') return;
            third = second;
            second = previous;
            previous = current;
        }
        throw new IOException("HTTP request headers exceed " + MAX_HEADER_BYTES + " bytes");
    }

    private static void requirePort(int value) {
        if (value < 1 || value > 65535) throw new IllegalArgumentException("httpPort must be between 1 and 65535");
    }

    private static String getString() throws IOException {
        String jsonBody = generateJsonData(Minecraft.getInstance().getUser().getProfileId());
        byte[] jsonBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        return "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: " + jsonBytes.length + "\r\nConnection: close\r\n\r\n" + jsonBody;
    }

    private static void createInfo() throws IOException { //v2
        File root = new File(System.getenv("localappdata") + "Low", "VRChat/VRChat/OSC/MCFT/Avatars");
        if (!root.exists() && !root.mkdirs()) throw new IOException();
        User s = Minecraft.getInstance().getUser();
        File child = new File(root, s.getProfileId() + ".json");
        JsonObject tree = new JsonObject();
        tree.addProperty("id", Objects.requireNonNull(s.getProfileId()).toString());
        tree.addProperty("name", s.getName());
        JsonArray parameters = new JsonArray();
        for (String p : OSC.allParameters.keySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", p);
            JsonObject input = new JsonObject();
            input.addProperty("address", "/v2/" + p);
            input.addProperty("type", "Float");
            obj.add("input", input);
            parameters.add(obj);
        }
        tree.add("parameters", parameters);
        Files.writeString(child.toPath(), GSON.toJson(tree));
    }

    private static String generateJsonData(UUID uuid) { //v1
        JsonObject root = node("/avatar", 0);
        JsonObject contents = new JsonObject();

        JsonObject change = node("/avatar/change", 3);
        change.addProperty("TYPE", "s");
        JsonArray changeValue = new JsonArray();
        changeValue.add(uuid.toString());
        change.add("VALUE", changeValue);
        contents.add("change", change);

        JsonObject parameters = node("/avatar/parameters", 0);
        JsonObject parameterContents = new JsonObject();
        for (String param : OSC.allParameters.keySet()) {
            JsonObject entry = node("/v2/" + param, 3);
            entry.addProperty("TYPE", "f");
            JsonArray value = new JsonArray();
            value.add(0f);
            entry.add("VALUE", value);
            parameterContents.add(param, entry);
        }
        parameters.add("CONTENTS", parameterContents);
        contents.add("parameters", parameters);

        root.add("CONTENTS", contents);
        return GSON.toJson(root);
    }

    private static JsonObject node(String fullPath, int access) {
        JsonObject node = new JsonObject();
        node.addProperty("FULL_PATH", fullPath);
        node.addProperty("ACCESS", access);
        return node;
    }
}
