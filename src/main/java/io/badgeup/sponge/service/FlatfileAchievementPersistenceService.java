package io.badgeup.sponge.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FlatfileAchievementPersistenceService implements AchievementPersistenceService {

    private File achievementFile;
    private JSONObject unpresentedAchievements;

    public FlatfileAchievementPersistenceService(Path configDir) {
        // Make sure the directory exists
        configDir.toFile().mkdirs();
        this.achievementFile = configDir.resolve("unpresented-achievements.json").toFile();

        if (this.achievementFile.exists()) {
            this.unpresentedAchievements = readFromFile();
        } else {
            this.unpresentedAchievements = new JSONObject();
        }

    }

    @Override
    public CompletableFuture<List<JSONObject>> getUnpresentedAchievementsForPlayer(UUID playerID) {
        return CompletableFuture.supplyAsync(() -> {
            List<JSONObject> playerAchievements = new ArrayList<>();

            if (this.unpresentedAchievements.has(playerID.toString())) {
                this.unpresentedAchievements.getJSONArray(playerID.toString()).forEach(obj -> {
                    playerAchievements.add((JSONObject) obj);
                });
            }

            return playerAchievements;
        });

    }

    @Override
    public void addUnpresentedAchievement(UUID playerID, JSONObject achievement) {
        // TODO validate achievement data before saving
        if (this.unpresentedAchievements.has(playerID.toString())) {
            JSONArray playerAchievements = this.unpresentedAchievements.getJSONArray(playerID.toString());
            playerAchievements.put(achievement);
        } else {
            JSONArray playerAchievements = new JSONArray();
            playerAchievements.put(achievement);
            this.unpresentedAchievements.put(playerID.toString(), playerAchievements);
        }
        saveToFile();
    }

    @Override
    public void removeAchievementByID(UUID playerID, String achievementID) {
        if (!this.unpresentedAchievements.has(playerID.toString())) {
            throw new IllegalStateException("Player " + playerID + " has no unpresented achievements to remove");
        }
        JSONArray playerAchievements = this.unpresentedAchievements.getJSONArray(playerID.toString());
        for (int i = 0; i < playerAchievements.length(); i++) {
            if (playerAchievements.getJSONObject(i).getString("id").equals(achievementID)) {
                playerAchievements.remove(i);
                break;
            }
        }
        saveToFile();
    }

    private JSONObject readFromFile() {
        try {
            Scanner scanner = new Scanner(this.achievementFile);
            String jsonTxt = scanner.useDelimiter("\\Z").next();
            scanner.close();
            return new JSONObject(jsonTxt);
        } catch (Exception e) {
            // This shouldn't ever happen
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void saveToFile() {
        try {
            this.achievementFile.createNewFile(); // Only creates if not already
                                                  // exists
            FileWriter writer = new FileWriter(this.achievementFile);
            writer.write(this.unpresentedAchievements.toString());
            writer.close();
        } catch (IOException e) {
            // Shouldn't ever happen
            e.printStackTrace();
        }

    }

}
