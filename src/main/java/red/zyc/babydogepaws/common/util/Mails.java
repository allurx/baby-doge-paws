package red.zyc.babydogepaws.common.util;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.exception.MailSendException;

import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author allurx
 */
public final class Mails {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mails.class);
    private static final ExecutorService SEND_MAIL_EXECUTOR = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("Mailer-", 0).factory());
    private static final MailServerConfiguration GMAIL_SERVER = new MailServerConfiguration("smtp.gmail.com", 465, "allurx.zyc@gmail.com", "allurx.zyc@gmail.com", "allurx.zyc@gmail.com", "mjegfzjggsxacxrq");
    private static final MailServerConfiguration QQ_SERVER = new MailServerConfiguration("smtp.qq.com", 465, "allurx@qq.com", "allurx@qq.com", "allurx@qq.com", "niqdqxyghqiyhbcg");

    private Mails() {
    }

    /**
     * 发送文本邮件
     *
     * @param subject 邮件主题
     * @param message 邮件内容
     */
    public static void sendTextMail(String subject, String message) {
        SEND_MAIL_EXECUTOR.execute(() -> {
            try {
                Email email = buildEmail(new SimpleEmail(), QQ_SERVER);
                email.setSubject(subject);
                email.setMsg(checkMessage(message));
                email.send();
            } catch (Throwable t) {
                LOGGER.error("邮件发送异常", new MailSendException(t));
            }
        });
    }

    /**
     * 发送zip邮件
     *
     * @param subject 邮件主题
     * @param errors  邮件内容
     */
    public static void sendZipMail(String subject, Map<String, ? extends Throwable> errors) {
        SEND_MAIL_EXECUTOR.execute(() -> {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {

                // 组装zip并将其传输到ByteArrayOutputStream中
                for (Map.Entry<String, ? extends Throwable> entry : errors.entrySet()) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getKey() + ".txt"));
                    zipOutputStream.write(Commons.convertThrowableToString(entry.getValue()).getBytes());
                    zipOutputStream.closeEntry();
                }

                // 如果不调用此方法，写入邮件的zip字节是错的，得到的是一个损坏的压缩包
                zipOutputStream.finish();

                MultiPartEmail email = buildEmail(new MultiPartEmail(), QQ_SERVER);
                email.setSubject(subject);
                email.attach(new ByteArrayDataSource(out.toByteArray(), "application/zip"), String.format("errors-%s.zip", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"))), "异常日志汇总");
                email.send();
            } catch (Exception e) {
                throw new MailSendException(e);
            }
        });
    }

    private static <T extends Email> T buildEmail(T email, MailServerConfiguration configuration) throws Exception {
        email.setHostName(configuration.hostname);
        email.setSmtpPort(configuration.port);
        email.setAuthenticator(new DefaultAuthenticator(configuration.username, configuration.password));
        email.setSSLOnConnect(true);
        email.setFrom(configuration.from);
        email.addTo(configuration.to);
        return email;
    }

    private static String checkMessage(String message) {
        return message != null && !message.isEmpty() ? message : "no message";
    }

    record MailServerConfiguration(String hostname, int port, String from, String to, String username,
                                   String password) {
    }
}
