package red.zyc.babydogepaws.exception;

/**
 * 邮件发送异常
 *
 * @author allurx
 */
public class MailSendException extends RuntimeException {

    public MailSendException(Throwable cause) {
        super(cause);
    }
}