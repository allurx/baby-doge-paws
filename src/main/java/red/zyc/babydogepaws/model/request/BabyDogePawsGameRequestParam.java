package red.zyc.babydogepaws.model.request;

import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

/**
 * @author allurx
 */
public class BabyDogePawsGameRequestParam {

    public final BabyDogePawsUser user;

    public BabyDogePawsGameRequestParam(BabyDogePawsUser user) {
        this.user = user;
    }
}
