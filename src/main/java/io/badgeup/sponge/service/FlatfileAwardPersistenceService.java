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

public class FlatfileAwardPersistenceService implements AwardPersistenceService {

    private File awardsFile;
    private JSONObject pendingAwards;

    public FlatfileAwardPersistenceService(Path configDir) {
        // Make sure the directory exists
        configDir.toFile().mkdirs();
        this.awardsFile = configDir.resolve("pending-awards.json").toFile();

        if (this.awardsFile.exists()) {
            this.pendingAwards = readFromFile();
        } else {
            this.pendingAwards = new JSONObject();
        }

    }

    @Override
    public CompletableFuture<List<JSONObject>> getPendingAwardsForPlayer(UUID playerID) {
        return CompletableFuture.supplyAsync(() -> {
            List<JSONObject> playerAchievements = new ArrayList<>();

            if (this.pendingAwards.has(playerID.toString())) {
                this.pendingAwards.getJSONArray(playerID.toString()).forEach(obj -> {
                    playerAchievements.add((JSONObject) obj);
                });
            }

            return playerAchievements;
        });

    }

    @Override
    public void addPendingAward(UUID playerID, JSONObject achievement) {
        // TODO validate award data before saving
        if (this.pendingAwards.has(playerID.toString())) {
            JSONArray playerAchievements = this.pendingAwards.getJSONArray(playerID.toString());
            playerAchievements.put(achievement);
        } else {
            JSONArray playerAchievements = new JSONArray();
            playerAchievements.put(achievement);
            this.pendingAwards.put(playerID.toString(), playerAchievements);
        }
        saveToFile();
    }

    @Override
    public void removePendingAwardByID(UUID playerID, String achievementID) {
        if (!this.pendingAwards.has(playerID.toString())) {
            throw new IllegalStateException("Player " + playerID + " has no pending awards to remove");
        }
        JSONArray playerAchievements = this.pendingAwards.getJSONArray(playerID.toString());
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
            Scanner scanner = new Scanner(this.awardsFile);
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
            this.awardsFile.createNewFile(); // Only creates if not already
                                             // exists
            FileWriter writer = new FileWriter(this.awardsFile);
            writer.write(this.pendingAwards.toString());
            writer.close();
        } catch (IOException e) {
            // Shouldn't ever happen
            e.printStackTrace();
        }

    }

}
