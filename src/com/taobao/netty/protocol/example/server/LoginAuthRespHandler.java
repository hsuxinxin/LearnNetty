package com.taobao.netty.protocol.example.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.taobao.netty.protocol.example.Header;
import com.taobao.netty.protocol.example.MessageType;
import com.taobao.netty.protocol.example.NettyMessage;

public class LoginAuthRespHandler extends ChannelHandlerAdapter {

	private Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();
	private String[] whitekList = { "127.0.0.1" };

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
				&& message.getHeader().getType() == MessageType.LOGIN_REQ
						.value()) {
			String nodeIndex = ctx.channel().remoteAddress().toString();
			NettyMessage loginResp = null;
			// 重复登陆，拒�?
			if (nodeCheck.containsKey(nodeIndex)) {
				loginResp = buildResponse((byte) -1);
			} else {
				InetSocketAddress address = (InetSocketAddress) ctx.channel()
						.remoteAddress();
				String ip = address.getAddress().getHostAddress();
				boolean isOK = false;
				for (String WIP : whitekList) {
					if (WIP.equals(ip)) {
						isOK = true;
						break;
					}
				}
				loginResp = isOK ? buildResponse((byte) 0)
						: buildResponse((byte) -1);
				if (isOK)
					nodeCheck.put(nodeIndex, true);
			}
			System.out.println("The login response is : " + loginResp
					+ " body [" + loginResp.getBody() + "]");
			ctx.writeAndFlush(loginResp);
		} else {
			ctx.fireChannelRead(msg);
		}
	}

	private NettyMessage buildResponse(byte result) {
		NettyMessage message = new NettyMessage();
		Header header = new Header();
		header.setType(MessageType.LOGIN_RESP.value());
		message.setHeader(header);
		message.setBody(result);
		return message;
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		nodeCheck.remove(ctx.channel().remoteAddress().toString());// 删除缓存
		ctx.close();
		ctx.fireExceptionCaught(cause);
	}
}
