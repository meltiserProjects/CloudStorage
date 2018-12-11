package ru.grigorev.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import ru.grigorev.common.Info;
import ru.grigorev.server.db.dao.DAO;
import ru.grigorev.server.db.service.DatabaseInteraction;
import ru.grigorev.server.db.service.JdbcDatabaseInteraction;

/**
 * @author Dmitriy Grigorev
 */
public class Server {
    private static final int MAX_OBJ_SIZE = 1024 * 1024 * 100; // 100 mb
    private static DatabaseInteraction db;
    private static DAO dao;
    private static Server server;

    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast( // web ----> server
                                    new ObjectDecoder(MAX_OBJ_SIZE, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthInHandler(dao),
                                    new MainInHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(Info.PORT).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        db = new JdbcDatabaseInteraction(Info.DB_URL, Info.DB_USER, Info.DB_PASSWORD);
        db.initialize();
        System.out.println("db has initialized");
        dao = db.getDAO();
        server = new Server();
        System.out.println("server is starting...");
        server.run();
    }
}
