package com.taobao.nio.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable{
	private Selector selector;
	private ServerSocketChannel servChannel;
	private volatile boolean stop;
	
	public MultiplexerTimeServer(int port){
		try{
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress(port),1024);
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The time server is start in port : "+port);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void stop(){
		this.stop = false;
	}

	@Override
	public void run() {
		while(!stop){
			try{
				selector.select(1000);
				Set<SelectionKey> seletedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = seletedKeys.iterator();
				SelectionKey key = null;
				while(it.hasNext()){
					key = it.next();
					it.remove();
					try{
						handleInput(key);
					}catch(Exception e){
						if(key != null){
							key.cancel();
							if(key.channel() != null){
								key.channel().close();
							}
						}
					}
				}
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
		
		if(selector != null){
			try{
				selector.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private void handleInput(SelectionKey key) throws IOException{
		if(key.isValid()){
			if(key.isAcceptable()){
				ServerSocketChannel  ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				sc.register(selector, SelectionKey.OP_READ);
			}
			if(key.isReadable()){
				SocketChannel sc = (SocketChannel) key.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if(readBytes > 0){
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("The time Server receive order : " + body);
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
							new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
					doWrite(sc, currentTime);
				}else if (readBytes < 0){
					//对应链路关闭
					System.out.println("MultiplexerTimeServer 对应链路关闭");
					key.cancel();
					sc.close();
				}else{
					System.out.println("MultiplexerTimeServer Ignore 0 bytes");
				}
				
			}
		}
	}

	private void doWrite(SocketChannel sc, String responseStr) throws IOException {
		if(responseStr != null || responseStr.trim().length() > 0){
			byte[] bytes = responseStr.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			sc.write(writeBuffer);
		}
	}
	
}
