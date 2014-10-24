package com.taobao.netty.http.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServer {
	private static final String DEFAULT_URL = "";
	
	public static void main(String[] args) {

	}
	
	public void run(final int port, final String url) throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast("http-decoder",new HttpRequestDecoder());
					ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
					ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
					ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
					ch.pipeline().addLast("fileServerhandler", new HttpFileServerHandler(url));
				}
				
			});
			ChannelFuture future = b.bind("127.0.0.1",port).sync();
			System.out.println("HTTP Server start");
			future.channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
