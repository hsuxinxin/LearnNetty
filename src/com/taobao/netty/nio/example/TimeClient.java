package com.taobao.netty.nio.example;

public class TimeClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port= 8080;
		new Thread(new TimeClientHandle("127.0.0.1",port),"TimeClient-001").start();
	}

}
