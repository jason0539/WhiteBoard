package com.jzj.socket;

public class ClsMainServer {

	public static void main(String[] args) {
		int port = 1234;
		TcpServer server = new TcpServer(port) {

			@Override
			public void onConnect(SocketTransceiver client) {
				printInfo(client, "Connect");
			}

			@Override
			public void onConnectFailed() {
				System.out.println("Client Connect Failed");
			}

			@Override
			public void onReceive(String from, String s) {
				System.out.println("遍历客户端发送消息");
				for (SocketTransceiver client : clients) {
					String targetAddr = client.getInetAddress().getHostAddress();
					if (!from.equals(targetAddr)) {
						System.out.println("发送给"+targetAddr);
						printInfo(client, "Send Data: " + s);
						client.send(s);
					}else {
						System.out.println("不给"+targetAddr+"发送");
					}
				}
			}

			@Override
			public void onDisconnect(SocketTransceiver client) {
				printInfo(client, "Disconnect");
			}

			@Override
			public void onServerStop() {
				System.out.println("--------Server Stopped--------");
			}
		};
		System.out.println("--------Server Started--------");
		server.start();
	}

	static void printInfo(SocketTransceiver st, String msg) {
		System.out.println("Client " + st.getInetAddress().getHostAddress());
		System.out.println("  " + msg);
		System.out.println("  \n\n");
	}
}
