package com.lucendar.gnss.actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import info.gratour.common.error.ErrorWithCode;
import info.gratour.common.types.rest.Reply;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.jdk.javaapi.FutureConverters;
import scala.reflect.ClassTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class Actors {

    public static final String BLOCKING_DISPATCHER = "akka.actor.default-blocking-io-dispatcher";
    private static final Logger LOGGER = LoggerFactory.getLogger("gnss.actors");

    public interface ActorMsg {}

    /**
     * Actor 启动完毕通知
     *
     * @param t 异常。为 null 时表正常启动。不为 null 时为启动失败。
     */
    public record StartUpCompleted(Throwable t) implements ActorMsg {
        boolean success() {
            return t == null;
        }
        boolean failed() {
            return t != null;
        }
    }

    /**
     * Actor 终止
     *
     * @param actor 终止的Actor对象
     * @param id 对象的ID，应用自定义
     */
    public record ActorTerminated(ActorRef actor, String id) implements ActorMsg {}

    /**
     * Worker请求新的任务
     *
     * @param worker 执行任务的 Worker
     */
    public record GetJob(@NonNull ActorRef worker) {}

    public interface ActorRequest extends ActorMsg {
        ActorRef replyTo();

        default ActorRef replyToOrSender(ActorContext context) {
            var r = replyTo();
            if (r != null)
                return r;
            else
                return context.sender();
        }

        default void sendReply(ActorContext context, Reply<?> reply) {
            var replyTo = replyTo();
            if (replyTo != null)
                replyTo.tell(reply, context.self());
        }

        default void sendReplyAny(ActorContext context, Object reply) {
            var replyTo = replyTo();
            if (replyTo != null)
                replyTo.tell(reply, context.self());
        }


        static <T extends ActorRequest> boolean safetyExec(ActorContext context, T msg, Consumer<T> handler) {
            try {
                handler.accept(msg);

                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R> boolean safetyCall(
                ActorContext context,
                T msg,
                Function<T, Reply<R>> handler) {
            try {
                var r = handler.apply(msg);
                msg.sendReply(context, r);
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R> boolean safetyCallList(
                ActorContext context,
                T msg,
                Function<T, List<R>> handler) {
            try {
                var r = handler.apply(msg);
                msg.sendReply(context, Reply.multi(r));
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R, R2> boolean safetyCallList(
                ActorContext context,
                T msg,
                Function<T, List<R>> handler,
                Reply.Mapper<R, R2> mapper) {
            try {
                var r = handler.apply(msg);
                var r2 = new ArrayList<R2>();
                for (R r1 : r) {
                    var item2 = mapper.map(r1);
                    r2.add(item2);
                }
                msg.sendReply(context, Reply.multi(r2));
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R> boolean safetyCallSingle(
                ActorContext context,
                T msg,
                Function<T, R> handler) {
            try {
                var r = handler.apply(msg);
                msg.sendReply(context, Reply.single(r));
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R, R2> boolean safetyCallSingle(
                ActorContext context,
                T msg,
                Function<T, R> handler,
                Reply.Mapper<R, R2> mapper) {
            try {
                var r = handler.apply(msg);
                msg.sendReply(context, Reply.single(mapper.map(r)));
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest> boolean safetyCallVoid(
                ActorContext context,
                T msg,
                Consumer<T> handler) {
            try {
                handler.accept(msg);
                msg.sendReply(context, Reply.OK_VOID);
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

        static <T extends ActorRequest, R, R2> boolean safetyCall(
                ActorContext context,
                T msg,
                Function<T, Reply<R>> handler,
                Reply.Mapper<R, R2> mapper) {
            try {
                var r = handler.apply(msg).map(mapper);
                msg.sendReply(context, r);
                return true;
            } catch (ErrorWithCode err) {
                LOGGER.error(err.getMessage(), err);
                msg.sendReply(context, Reply.error(err));
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                msg.sendReply(context, Reply.internalError());
            }

            return false;
        }

    }

    public static void replySender(Object msg, ActorContext context) {
        context.sender().tell(msg, context.self());
    }

    public interface MessageWithReplyToCreator<T extends ActorRequest> {
        T create(ActorRef replyTo);
    }

    public  static  <T, M extends ActorRequest> CompletionStage<Reply<T>> askWithReplyTo(
            ActorRef receiver,
            MessageWithReplyToCreator<M> messageCreator,
            int timeoutSeconds) {
        if (receiver == null)
            throw new NullPointerException("receiver");

        Future<Reply<T>> f = Patterns
                .askWithReplyTo(
                        receiver,
                        messageCreator::create,
                        timeoutSeconds * 1000L
                ).mapTo(ClassTag.<Reply<T>>apply(Reply.class));

        return FutureConverters.asJava(f).toCompletableFuture();
    }


}
