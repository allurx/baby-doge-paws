package red.zyc.babydogepaws.model.response;

/**
 * 任务
 *
 * @param id          任务id
 * @param isResolved 任务是否已解决
 * @author allurx
 */
public record Channel(long id,
                      boolean isResolved,
                      boolean isRewardTaken,
                      boolean isPremium
                      ) {
}
