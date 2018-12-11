package ru.grigorev.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.grigorev.common.message.AuthMessage;
import ru.grigorev.common.message.Message;
import ru.grigorev.common.message.MessageType;
import ru.grigorev.server.db.dao.DAO;
import ru.grigorev.server.db.model.User;

/**
 * @author Dmitriy Grigorev
 */
public class AuthInHandler extends ChannelInboundHandlerAdapter {
    private boolean isAuthorized;
    private DAO dao;
    private String login;

    public AuthInHandler(DAO dao) {
        this.dao = dao;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client has connected (Auth Handler)");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof AuthMessage) {
                AuthMessage message = (AuthMessage) msg;
                login = message.getLogin();
                String password = message.getPassword();
                User foundUser = dao.getUserByLogin(login);
                if (message.getType().equals(MessageType.SIGN_IN_REQUEST)) {
                    if (foundUser == null)
                        ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_FAIL, "No such user or incorrect login!"));
                    else {
                        if (password.equals(foundUser.getPassword())) {
                            isAuthorized = true;
                            ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_OK, "You have successfully authorized!"));
                        } else
                            ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_FAIL, "Wrong password!"));
                    }
                }
                if (message.getType().equals(MessageType.SIGN_UP_REQUEST)) {
                    if (foundUser == null) {
                        dao.insertNewUser(new User(login, password));
                        isAuthorized = true;
                        ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_OK, "You have successfully signed up!"));
                    } else
                        ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_FAIL, "This user is already exists!"));
                }
                if (message.getType().equals(MessageType.SIGN_OUT_REQUEST)) {
                    isAuthorized = false;
                    ctx.writeAndFlush(new AuthMessage(MessageType.SIGN_OUT_RESPONSE));
                }
                if (message.getType().equals(MessageType.DISCONNECTING)) {
                    ctx.writeAndFlush(new AuthMessage(MessageType.DISCONNECTING));
                }
            }
            if (msg instanceof Message) {
                if (isAuthorized) {
                    Message msgWithLogin = (Message) msg;
                    msgWithLogin.setLogin(login);
                    ctx.fireChannelRead(msgWithLogin);
                } else ctx.writeAndFlush(new AuthMessage(MessageType.AUTH_FAIL, "Please sign in or sign up"));
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client disconnected");
    }
}
