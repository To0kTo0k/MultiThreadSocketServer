package org.example;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiThreadServer {
    @Value("${server.port}")
    private int serverPort;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    @PostConstruct
    public void init() {
        connectServer();
    }

    private void connectServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            log.info("Сервер готов к работе и слушает порт " + serverPort);
            while (!serverSocket.isClosed()) {
                final Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(20000);
                EXECUTOR_SERVICE.execute(()->handleClient(clientSocket));
            }
        } catch (IOException e) {
            log.error("Ошибка при работе сервера: " + e.getMessage());
        } finally {
            EXECUTOR_SERVICE.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            log.info("Запрос на подключение от клиента");
            while (!clientSocket.isClosed()) {
                handshakingResponse(writer);
                processClientRequest(readClientRequest(reader));
                clientSocket.close();
                log.info("Поток освобождается");
            }
        } catch (SocketTimeoutException e) {
            log.error("Соединение закрыто из-за таймаута: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при работе с клиентом: " + e.getMessage());
        }
    }

    private void handshakingResponse(PrintWriter writer) {
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/plain; charset=utf-8");
        writer.println();
        writer.println("Сервер готов к работе");
    }

    private long readClientRequest(BufferedReader reader) throws IOException {
        String inputLine = reader.readLine();
        long request = 0;
        try {
            if (inputLine == null || inputLine.isEmpty()) throw new NoSuchElementException();
            request = Integer.parseInt(inputLine);
            if (request < 0) throw new NumberFormatException();
            request *= 1000;
        } catch (NoSuchElementException e) {
            log.error("Клиент не передал данные: " + e.getMessage());
            return request;
        } catch(NumberFormatException e) {
            log.error("Клиент передал некорректные данные: " + e.getMessage());
            return request;
        }
        log.info("Получено корректное сообщение от клиента: " + inputLine);
        return request;
    }

    private void processClientRequest(long seconds) throws InterruptedException {
        log.info("Поток засыпает на " + seconds + " миллисекунд");
        Thread.sleep(seconds);
        log.info("Поток просыпается");
    }
}
