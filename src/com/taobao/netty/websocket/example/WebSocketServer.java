package com.taobao.netty.websocket.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class WebSocketServer {
	public static void main(String[] args) throws Exception{
		int port = 8080;
		new WebSocketServer().run(port);
	}
	public void run(int port) throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch)
								throws Exception {
							ch.pipeline().addLast("http-codec",
									new HttpServerCodec());
							ch.pipeline().addLast("aggregator",
									new HttpObjectAggregator(65536));
							ch.pipeline().addLast("http-chunked",
									new ChunkedWriteHandler());
							ch.pipeline().addLast("handler",
									new WebSocketServerHandler());
							//…Ë÷√≥¨ ±
							ch.pipeline().addLast(new ReadTimeoutHandler(30));
						}
					});
			Channel ch = b.bind(port).sync().channel();
			System.out.println("Web sockect server started at port : " + port);
			ch.closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
