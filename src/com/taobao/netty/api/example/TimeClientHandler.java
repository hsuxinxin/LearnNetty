package com.taobao.netty.api.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class TimeClientHandler extends ChannelHandlerAdapter{
	private final ByteBuf firstMessage;
	private int counter = 0;
	public TimeClientHandler(){
		byte[] req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
		firstMessage = Unpooled.buffer(req.length);
		firstMessage.writeBytes(req);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx){
		System.out.println("TimeClientHandler channelActive");
		byte[] req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
		ByteBuf tempMessage = null;
		for(int i=0 ; i<100; i++){
			tempMessage = Unpooled.buffer(req.length);
			tempMessage.writeBytes(req);
			ctx.writeAndFlush(tempMessage);
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
//		ByteBuf buf = (ByteBuf) msg;
//		byte[] req = new byte[buf.readableBytes()];
//		buf.readBytes(req);
//		String body = new String(req,"UTF-8");
		String body = (String) msg;
		System.out.println("Now is : " + body+"; the counter is " + ++counter);
		System.out.println("TimeClientHandler channelRead end");
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		ctx.close();
	}
}
