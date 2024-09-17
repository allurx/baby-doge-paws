package red.zyc.babydogepaws.model.request;

import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.response.Channel;

/**
 * 采集任务奖励所需信息
 *
 * @author allurx
 */
public class ResolveChannel extends BabyDogePawsGameRequestParam {

    /**
     * {@link Channel}
     */
    public final Channel channel;

    public ResolveChannel(BabyDogePawsUser user, Channel channel) {
        super(user);
        this.channel = channel;
    }
}
