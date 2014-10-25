package com.taobao.netty.http.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.spdy.SpdyHeaders.HttpNames;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{
	private final String url;
	
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
	
	public HttpFileServerHandler(String url) {
		this.url = url;
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx,
			FullHttpRequest request) throws Exception {
		if(!request.getDecoderResult().isSuccess()){
			sendError(ctx,HttpResponseStatus.BAD_REQUEST);
			return;
		}
		
		if(request.getMethod()!= HttpMethod.GET){
			sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}
		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		if(path == null){
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}
		File file = new File(path);
		if(file.isHidden() || !file.exists()){
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}
		if(file.isDirectory()){
			if(uri.endsWith("/")){
				sendListing(ctx,file,request);
			}else{
				sendRedirect(ctx,uri+"/");
			}
			return;
		}
		if(!file.isFile()){
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}
		if(uri.endsWith("html")){
			sendHtml(ctx, file, request);
			return;
		}
		sendFile(ctx, file, request);		
	}
	
	

	private void sendFile(ChannelHandlerContext ctx, File file, FullHttpRequest request) throws IOException {
		RandomAccessFile randomAccessFile = null;
		try{
			randomAccessFile = new RandomAccessFile(file, "r");
		}catch(FileNotFoundException e){
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}
		long fileLength = randomAccessFile.length();
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
		HttpHeaders.setContentLength(response, fileLength);
		SetContentTypeHeader(response, file);
		if(HttpHeaders.isKeepAlive(request)){
			response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		ctx.write(response);
		ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile,0,fileLength,8192), ctx.newProgressivePromise());
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
			
			@Override
			public void operationComplete(ChannelProgressiveFuture future)
					throws Exception {
				System.out.println("Transfer Complete.");
			}
			
			@Override
			public void operationProgressed(ChannelProgressiveFuture future,
					long progress, long total) throws Exception {
				if(total < 0){
					System.err.println("Transfer progress : " + progress);
				}else{
					System.err.println("Transfer progress : " + progress + "/"
							+ total);
				}
			}
		});
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!HttpHeaders.isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void sendListing(ChannelHandlerContext ctx, File dir,  FullHttpRequest request) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
		response.headers().set(Names.CONTENT_TYPE,"text/html;charset=UTF-8");
		StringBuilder buf = new StringBuilder();
		String dirPath = dir.getPath();
		buf.append("<!DOCTYPE html>\r\n");
		buf.append("<html><head><title>");
		buf.append(dirPath);
		buf.append("目录");
		buf.append("</title></head><body>\r\n");
		buf.append("<ul><li>返回上一级  <a href=\"../\">..</a></li>\r\n");
		for(File f : dir.listFiles()){
			if(f.isHidden() || !f.canRead()){
				continue;
			}
			String name = f.getName();
			if(!ALLOWED_FILE_NAME.matcher(name).matches()){
				continue;
			}
			buf.append("<li><a href=\"");
			buf.append(name);
			buf.append("\">");
			buf.append(name);
			buf.append("</a></li>\r\n");
		}
		buf.append("</ul></body></html>");
		HttpHeaders.setContentLength(response, buf.length());
		ByteBuf buffer = Unpooled.copiedBuffer(buf,CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		ChannelFuture lastContentFuture = ctx.writeAndFlush(response);
//		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!HttpHeaders.isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private void sendHtml(ChannelHandlerContext ctx, File file,  FullHttpRequest request) throws IOException {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
		response.headers().set(Names.CONTENT_TYPE,"text/html;charset=ISO_8859_1");
		StringBuilder buf = new StringBuilder();
		RandomAccessFile aFile = new RandomAccessFile(file, "rw");
		FileChannel inChannel = aFile.getChannel();
		ByteBuffer temBuffer = ByteBuffer.allocate(1024);
		while(inChannel.read(temBuffer) != -1){			
			temBuffer.flip();
			byte[] bytes = new byte[temBuffer.remaining()];
			temBuffer.get(bytes);
			String body = new String(bytes, "ISO_8859_1");
			buf.append(body);
			temBuffer.clear();
		}
		HttpHeaders.setContentLength(response, buf.length());
		ByteBuf buffer = Unpooled.copiedBuffer(buf,CharsetUtil.ISO_8859_1);
		response.content().writeBytes(buffer);
		buffer.release();
		ChannelFuture lastContentFuture = ctx.writeAndFlush(response);
//		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!HttpHeaders.isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}

	}
	
	private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.FOUND);
		response.headers().set(Names.LOCATION, newUri);
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		
	}
	
	private void sendError(ChannelHandlerContext ctx,
			HttpResponseStatus badRequest) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,badRequest,
				Unpooled.copiedBuffer("Failure: "+badRequest.toString()+"\r\n",CharsetUtil.UTF_8));
		response.headers().set(Names.CONTENT_TYPE,"text/plain; charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);			
	}
	
	private String sanitizeUri(String uri) {
		try{
			uri = URLDecoder.decode(uri, "UTF-8");
		}catch(UnsupportedEncodingException e){
			try{
				uri = URLDecoder.decode(uri,"ISO-8859-1");
			}catch(UnsupportedEncodingException el){
				return null;
			}
		}
		if(!uri.startsWith(url)){
			return null;
		}
		if(!uri.startsWith("/")){
			return null;
		}
		uri = uri.replace('/', File.separatorChar);
		if(uri.contains(File.separator+".") || uri.contains("."+File.separator)
		|| uri.startsWith(".") || uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()){
			return null;
		}
		return System.getProperty("user.dir") + File.separator + uri;
	}

	private static void SetContentTypeHeader(HttpResponse response, File file){
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.headers().set(Names.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		cause.printStackTrace();
		if(ctx.channel().isActive()){
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
