package com.taobao.netty.protocol.example.client;

import com.taobao.netty.protocol.example.Header;
import com.taobao.netty.protocol.example.MessageType;
import com.taobao.netty.protocol.example.NettyMessage;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

public class LoginAuthReqHandler extends ChannelHandlerAdapter {

	/**
	 * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward to the
	 * next {@link ChannelHandler} in the {@link ChannelPipeline}.
	 * 
	 * Sub-classes may override this method to change behavior.
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.writeAndFlush(buildLoginReq());
	}

	/**
	 * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward to
	 * the next {@link ChannelHandler} in the {@link ChannelPipeline}.
	 * 
	 * Sub-classes may override this method to change behavior.
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		NettyMessage message = (NettyMessage) msg;

		if (message.getHeader() != null
				&& message.getHeader().getType() == MessageType.LOGIN_RESP
						.value()) {
			byte loginResult = (Byte) message.getBody();
			if (loginResult != (byte) 0) {
				ctx.close();
			} else {
				System.out.println("Login is ok : " + message);
				ctx.fireChannelRead(msg);
			}
		} else
			ctx.fireChannelRead(msg);
	}

	private NettyMessage buildLoginReq() {
		NettyMessage message = new NettyMessage();
		Header header = new Header();
		header.setType(MessageType.LOGIN_REQ.value());
		message.setHeader(header);
		return message;
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.fireExceptionCaught(cause);
	}
}
