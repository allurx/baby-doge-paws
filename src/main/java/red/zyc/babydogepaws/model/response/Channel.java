package red.zyc.babydogepaws.model.response;

/**
 * 任务
 *
 * @author allurx
 */
public record Channel(long id,
                      long reward,
                      boolean isResolved,
                      boolean isRewardTaken,
                      boolean isPremium
) {
}
