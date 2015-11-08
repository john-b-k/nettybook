package com.nettybook.example.ch8;

import java.net.InetAddress;
import java.util.Date;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TelnetServer {
	private static final Logger logger = Logger.getLogger(TelnetServer.class.getName());
	private static final int PORT =8023;

	public static void main(String[] args){
		EventLoopGroup boss = new NioEventLoopGroup(1);
		EventLoopGroup worker = new NioEventLoopGroup();
		
		try{
			logger.info("초기화 실행");
			ServerBootstrap b = new ServerBootstrap();
			b.group(boss,worker).
			channel(NioServerSocketChannel.class).
			handler(new LoggingHandler(LogLevel.INFO)).
			childHandler(new TelnetServerInitializer());
			
			ChannelFuture future = b.bind(PORT).sync();
			logger.info("bind port 실행");
			future.channel().closeFuture().sync();
			logger.info("channel closeFuture 실행");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			logger.info("Finally 실행");
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}
}

class TelnetServerInitializer extends ChannelInitializer<SocketChannel>{
	private static final StringDecoder DECODER = new StringDecoder();
	private static final StringEncoder ENCODER = new StringEncoder();
	private static final TelnetServerHandler SERVER_HANDLER = new TelnetServerHandler();
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new DelimiterBasedFrameDecoder(8192,Delimiters.lineDelimiter()));
		pipeline.addLast(DECODER);
		pipeline.addLast(ENCODER);
		pipeline.addLast(SERVER_HANDLER);
	}
}

@Sharable
class TelnetServerHandler extends SimpleChannelInboundHandler<String>{
	private static final Logger logger = Logger.getLogger(TelnetServerHandler.class.getName());
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.write("Welcome. "+InetAddress.getLocalHost().getHostName()+" Connected!\r\n");
		ctx.write("Current Time is "+new Date()+"\r\n");
		ctx.flush();
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
		System.out.println("channelRead0 실행");
		String response;
		boolean close = false;
		
		if(request.isEmpty()){
			response = "명령을 입력해주세요 \r\n";
		}else if("bye".equals(request.toLowerCase())){
			response = "좋은 하루 되세요";
			close = true;
		}else{
			response = "입력하신 명령은 '"+request+"' 입니다.";
		}
		
		ChannelFuture future = ctx.write(response);
		
		if(close){
			logger.info("ChannelFutureListener 실행");
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx){
		logger.info("channelReadComplete 실행");
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		cause.printStackTrace();
		ctx.close();
	}
}