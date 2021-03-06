package io.badgeup.sponge;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import io.badgeup.sponge.event.BadgeUpEvent;
import io.badgeup.sponge.service.AchievementPersistenceService;
import io.badgeup.sponge.service.AwardPersistenceService;
import org.json.JSONObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostEventRunnable implements Runnable {

    private BadgeUpSponge plugin;
    private BadgeUpEvent event;

    public PostEventRunnable(BadgeUpSponge plugin, BadgeUpEvent event) {
        this.plugin = plugin;
        this.event = event;
    }

    @Override
    public void run() {

        Config config = BadgeUpSponge.getConfig();

        // build the base API URL
        String baseURL = "";

        if (!config.getBadgeUpConfig().getBaseAPIURL().isEmpty()) {
            // override other config settings with this base URL
            baseURL = config.getBadgeUpConfig().getBaseAPIURL();
        } else {
            // region provided
            baseURL = "https://api." + config.getBadgeUpConfig().getRegion() + ".badgeup.io/v1/apps/";
        }

        String appId = Util.parseAppIdFromAPIKey(config.getBadgeUpConfig().getAPIKey()).get();

        this.plugin.getLogger().info("Posting BadgeUp event");

        this.event.setDiscardable(true);

        try {
            HttpResponse<JsonNode> response = Unirest.post(baseURL + appId + "/events").body(this.event.build())
                    .asJson();
            if (response.getStatus() == 413) {
                System.out.println("Event too large: " + this.event.build().getString("key"));
                return;
            }
            final JSONObject body = response.getBody().getObject();

            List<JSONObject> completedAchievements = new ArrayList<>();
            body.getJSONArray("progress").forEach(progressObj -> {
                JSONObject record = (JSONObject) progressObj;
                if (record.getBoolean("isComplete") && record.getBoolean("isNew")) {
                    completedAchievements.add(record);
                }
            });

            for (JSONObject record : completedAchievements) {
                final String earnedAchievementId = record.getString("earnedAchievementId");
                final JSONObject earnedAchievementRecord = Unirest
                        .get(baseURL + appId + "/earnedachievements/" + earnedAchievementId).asJson().getBody()
                        .getObject();

                final String achievementId = earnedAchievementRecord.getString("achievementId");
                final JSONObject achievement = Unirest.get(baseURL + appId + "/achievements/" + achievementId)
                        .asJson().getBody().getObject();

                final Optional<Player> subjectOpt = Sponge.getServer().getPlayer(this.event.getSubject());

                AwardPersistenceService awardPS = Sponge.getServiceManager()
                        .provide(AwardPersistenceService.class).get();
                List<String> awardIds = new ArrayList<>();
                achievement.getJSONArray("awards")
                        .forEach(awardId -> awardIds.add((String) awardId));

                for (String awardId : awardIds) {
                    final JSONObject award = Unirest.get(baseURL + appId + "/awards/" + awardId).asJson()
                            .getBody().getObject();
                    awardPS.addPendingAward(this.event.getSubject(), award);

                    boolean autoRedeem = Util.safeGetBoolean(award.getJSONObject("data"), "autoRedeem").orElse(false);
                    if (subjectOpt.isPresent() && autoRedeem) {
                        // Check if the award is auto-redeemable and
                        // send the redeem command if it is
                        Sponge.getCommandManager().process(subjectOpt.get(), "redeem " + awardId);
                    }
                }

                if (!subjectOpt.isPresent()) {
                    // Store the achievement to be presented later
                    AchievementPersistenceService achPS = Sponge.getServiceManager()
                            .provide(AchievementPersistenceService.class).get();
                    achPS.addUnpresentedAchievement(this.event.getSubject(), achievement);
                } else {
                    // Present the achievement to the player
                    BadgeUpSponge.presentAchievement(subjectOpt.get(), achievement);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            this.plugin.getLogger().error("Could not connect to BadgeUp API!");
        }

    }
}
