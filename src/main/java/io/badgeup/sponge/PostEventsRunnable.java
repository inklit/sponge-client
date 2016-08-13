package io.badgeup.sponge;

import java.net.URI;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.Sponge;

import com.google.common.base.Preconditions;

import io.badgeup.sponge.event.BadgeUpEvent;

public class PostEventsRunnable implements Runnable {

	private final String BASE_URL = "http://localhost:3000/v1/apps/";

	private BadgeUpSponge plugin;

	public PostEventsRunnable(BadgeUpSponge plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		final String apiKey = BadgeUpSponge.getConfig().getAPIKey();
		Preconditions.checkArgument(!apiKey.isEmpty(), "API key must not be empty");
		// Base64 decode the API key
		final byte[] decodedKey = Base64.getDecoder().decode(apiKey);
		JSONObject keyObj = null;
		try {
			keyObj = new JSONObject(new String(decodedKey));
		} catch (Exception e) {
			plugin.getLogger().error("Please specify a valid API key.");
		}
		final String appId = keyObj.getString("applicationId");
		Preconditions.checkArgument(!appId.isEmpty(), "Application ID must not be empty");
		Preconditions.checkArgument(appId.matches("^[a-zA-Z0-9]*$"),
				"Application ID must contain only letters and numbers");
		final BlockingQueue<BadgeUpEvent> eventQueue = BadgeUpSponge.getEventQueue();

		final String authHeader = "Basic " + new String(Base64.getEncoder().encode((apiKey + ":").getBytes()));
		Client client = ClientBuilder.newBuilder().build();
		WebTarget target = client.target(URI.create(BASE_URL + appId + "/events"));
		Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
		invocationBuilder.header("Authorization", authHeader);

		plugin.getLogger().info("Started BadgeUp event consumer");

		try {
			while (true) {
				final BadgeUpEvent event = eventQueue.take();

				Response response = invocationBuilder
						.post(Entity.entity(event.build().toString(), MediaType.APPLICATION_JSON_TYPE));
				final String rawBody = response.readEntity(String.class);
				if (response.getStatus() == 413) {
					System.out.println("Event too large: " + event.build().getString("key"));
					continue;
				}
				final JSONObject body = new JSONObject(rawBody);
				final JSONArray achievementProgress = body.getJSONArray("progress");
				achievementProgress.forEach(progressObj -> {
					JSONObject progress = (JSONObject) progressObj;
					if (!(progress.getBoolean("complete"))) { //  && progress.getBoolean("isNew")
						return;
					}
					Sponge.getScheduler().createTaskBuilder().execute(new ProcessAchievementRunnable(plugin, event.getSubject(), progress))
							.submit(plugin);
				});
			}
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
}
