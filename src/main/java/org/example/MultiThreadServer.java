package org.example;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MultiThreadServer {
    @Value("${server.port}")
    private int serverPort;

    @Value("${thread.number}")
    private int threadNumber;

    private ExecutorService executorService;

    private final CarDAO dao;

    @Autowired
    public MultiThreadServer(CarDAO dao) {
        this.dao = dao;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(threadNumber);
        connectServer();
    }

    private void connectServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            log.info("Сервер готов к работе и слушает порт " + serverPort);
            while (!serverSocket.isClosed()) {
                final Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(20000);
                executorService.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            log.error("Ошибка при работе сервера: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            log.info("Запрос на подключение от клиента");
            handshakingResponse(writer);
            while (!clientSocket.isClosed()) {
                long number = readClientRequest(reader);
                if (number == 0) {
                    clientSocket.close();
                } else {
                    processClientRequest(number, writer);
                }
            }
            log.info("Поток освободился");
        } catch (SocketTimeoutException e) {
            log.error("Соединение закрыто из-за таймаута: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при работе с клиентом: " + e.getMessage());
        }
    }

    private void handshakingResponse(PrintWriter writer) {
        writer.println("Сервер готов к работе");
        writer.flush();
    }

    private long readClientRequest(BufferedReader reader) throws IOException {
        String inputLine = reader.readLine();
        long request = 0;
        try {
            if (inputLine == null || inputLine.isEmpty()) throw new NoSuchElementException();
            Pattern pattern = Pattern.compile("\\b[\\d]+\\b");
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.find()) {
                inputLine = matcher.group();
            }
            request = Integer.parseInt(inputLine);
            if (request < 0) throw new NumberFormatException();
            request *= 1000;
        } catch (NoSuchElementException e) {
            log.error("Клиент не передал данные: " + e.getMessage());
            return request;
        } catch (NumberFormatException e) {
            log.error("Клиент передал некорректные данные: " + e.getMessage());
            return request;
        }
        log.info("Получено корректное сообщение от клиента: " + inputLine);
        return request;
    }

    private void processClientRequest(long milliseconds, PrintWriter writer) throws InterruptedException {
        log.info("Поток засыпает на " + milliseconds + " миллисекунд");
        Thread.sleep(milliseconds);
        log.info("Поток просыпается");
        String car = dao.getCar(milliseconds);
        writer.println("Сервер возвращает: " + car);
        writer.println("Сервер готов принять новый запрос");
        writer.flush();
    }
}