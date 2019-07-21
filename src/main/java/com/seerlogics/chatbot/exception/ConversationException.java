package com.seerlogics.chatbot.exception;

/**
 * Created by bkane on 10/6/18.
 */
public class ConversationException extends RuntimeException {

    private Type type;

    public Type getType() {
        return type;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public ConversationException(String message, Throwable cause, Type type) {
        super(message, cause);
        this.type = type;
    }

    public ConversationException(String message) {
        super(message);
    }

    public ConversationException(String message, Type type) {
        super(message);
        this.type = type;
    }

    public enum Type {
        LAUNCH_INFO_NOT_FOUND, ERROR_READING_BOT_CONFIG
    }
}
