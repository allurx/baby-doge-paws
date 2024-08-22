package red.zyc.babydogepaws.model.response;

/**
 * 任务
 *
 * @param id          任务id
 * @param isAvailable 任务是否可获取
 * @author allurx
 */
public record Channel(String id, String inviteLink, boolean isAvailable) {
}
