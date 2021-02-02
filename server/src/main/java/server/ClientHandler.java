package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    private final int TIMEOUT = 120_000;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    authLoop();
                    workLoop();
                } catch (SocketException e) {
                    sendMsg(Command.END);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void workLoop() throws IOException {
        socket.setSoTimeout(0);
        //цикл работы
        while (true) {
            String str = in.readUTF();

            if (str.startsWith("/")) {
                if (str.equals(Command.END)) {
                    sendMsg(Command.END);
                    System.out.println("client disconnected");
                    break;
                } else if (str.startsWith(Command.PRV_MSG)) {
                    int limit = 3;
                    String[] token = str.split("\\s", limit);
                    if (token.length > limit) {
                        server.privateMsg(this, token[1], token[2]);
                    }
                } else if (str.startsWith(Command.NICK_CHANGE)) {
                    // template /cnick [password] [newNickname]
                    int limit = 3;
                    String[] token = str.split("\\s", limit);
                    if (token.length >= limit) {
                        String pass = token[1];
                        String newNick = token[2];
                        if (server.getAuthService().changeNickname(login, pass, newNick)) {
                            nickname = newNick;
                            server.broadcastClientList();
                        }
                    }
                }

            } else {
                server.broadcastMsg(this, str);
            }
        }
    }

    private void authLoop() throws IOException {
        socket.setSoTimeout(TIMEOUT);
        //цикл аутентификации
        while (true) {
            String str = in.readUTF();

            if (str.startsWith(Command.AUTH)) {
                String[] token = str.split("\\s");
                String newNick = server.getAuthService()
                        .getNicknameByLoginAndPassword(token[1], token[2]);
                login = token[1];
                if (newNick != null) {
                    if (!server.isLoginAuthenticated(login)) {
                        nickname = newNick;
                        sendMsg(Command.AUTH_OK + " " + nickname);
                        server.subscribe(this);
                        System.out.println("client " + nickname + " connected " + socket.getRemoteSocketAddress());
                        break;
                    } else {
                        sendMsg("С этим логином уже авторизовались");
                    }
                } else {
                    sendMsg("Неверный логин / пароль");
                }
            }

            if (str.equals(Command.END)) {
                sendMsg(Command.END);
                throw new RuntimeException("client disconnected");
            }

            if (str.startsWith(Command.REG)) {
                String[] token = str.split("\\s");
                if (token.length < 4) {
                    continue;
                }
                boolean isRegistered = server.getAuthService().registration(token[1], token[2], token[3]);
                if (isRegistered) {
                    sendMsg(Command.REG_OK);
                } else {
                    sendMsg(Command.REG_NO);
                }
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
