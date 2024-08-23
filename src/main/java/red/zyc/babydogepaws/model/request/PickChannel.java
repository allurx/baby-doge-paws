package red.zyc.babydogepaws.model.request;

import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.response.Channel;

/**
 * 采集任务奖励所需信息
 *
 * @author allurx
 */
public class PickChannel extends BabyDogePawsGameRequestParam {

    /**
     * {@link Channel}
     */
    public final Channel channel;

    public PickChannel(BabyDogePawsAccount account, Channel channel) {
        super(account);
        this.channel = channel;
    }
}
