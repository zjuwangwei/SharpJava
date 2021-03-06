package netty2.factorial;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * Created by sunny-chen on 17/1/23.
 */
public class FactorialClientHandler extends SimpleChannelInboundHandler<BigInteger> {

    private int                       receivedMessage;
    private int                       next   = 1;
    private ChannelHandlerContext     ctx;
    final   BlockingQueue<BigInteger> answer = new LinkedBlockingDeque<>();

    public BigInteger getFactorial() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return answer.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        sendNumbers();
    }

    private void sendNumbers() {
        ChannelFuture future = null;
        for (int i = 0; i < 4096 && next <= FactorialClient.COUNT; i++) {
            future = ctx.write(Integer.valueOf(next));
            next++;
        }

        if (next <= FactorialClient.COUNT) {
            assert future != null;
            future.addListener(numberSender);
        }

        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BigInteger msg)
            throws Exception {
        receivedMessage++;
        if (receivedMessage == FactorialClient.COUNT) {
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    boolean offered = answer.offer(msg);
                    assert offered;
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private final ChannelFutureListener numberSender = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                sendNumbers();
            } else {
                future.cause().printStackTrace();
                future.channel().close();
            }
        }
    };
}
