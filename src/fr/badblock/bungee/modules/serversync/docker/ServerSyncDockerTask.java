package fr.badblock.bungee.modules.serversync.docker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.badblock.bungee.BadBungee;
import fr.badblock.bungee.utils.time.TimeUtils;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;
import redis.clients.jedis.Jedis;

/**
 * 
 * Task to send data in a recurrent time frame to synchronize nodes
 * 
 * @author xMalware
 *
 */
public class ServerSyncDockerTask extends Thread {

	public static Map<String, DockerServerData> serverData = new HashMap<>();
	public static Map<String, List<JsonObject>> clusterData = new HashMap<>();

	/**
	 * If the task is still running
	 * 
	 * @param New
	 *            state of the task
	 * @return If the task is still running or not
	 */
	public static boolean run = true;

	/**
	 * Task thread
	 */
	private static Thread syncThread = Thread.currentThread();

	/**
	 * Json parser ready to be used
	 */
	private static JsonParser jsonParser = new JsonParser();

	/**
	 * Get the current BungeeCord node IP
	 * 
	 * @return
	 */
	public static String getIP() {
		return BungeeCord.getInstance().config.getListeners().iterator().next().getHost().getAddress().getHostAddress();
	}

	/**
	 * Keep alive the current BungeeCord node
	 */
	public static void sync() {
		Jedis jedis = BadBungee.getInstance().getRedisService().getJedis();
		Map<String, DockerServerData> tempServerData = new HashMap<>();

		for (String cluster : BadBungee.getInstance().getConfig().getDockerClusters())
		{
			Set<String> keys = jedis.keys("clusters:" + cluster + ":*");
			if (keys == null || keys.isEmpty())
			{
				continue;
			}

			List<JsonObject> clusters = new ArrayList<>();
			List<String> values = jedis.mget(keys.toArray(new String[keys.size()]));
			for (String data : values)
			{
				JsonElement jsonElement = jsonParser.parse(data);
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				JsonObject idCardObject = jsonObject.getAsJsonObject("idCard");

				String fullId = idCardObject.get("fullId").getAsString();
				JsonObject dataObject = jsonObject.getAsJsonObject("data");
				JsonObject entityObject = dataObject.getAsJsonObject("entities");

				for (Entry<String, JsonElement> entr : entityObject.entrySet())
				{
					JsonArray jsonArray = entityObject.getAsJsonArray(entr.getKey());
					Iterator<JsonElement> iterator = jsonArray.iterator();
					while (iterator.hasNext())
					{
						JsonObject jsonServer = iterator.next().getAsJsonObject();
						long lastKeepAlive = jsonServer.get("lastKeepAlive").getAsLong();
						if (lastKeepAlive > System.currentTimeMillis())
						{
							String name = entr.getKey() + "_" + jsonServer.get("id").getAsInt();
							String ip = jsonServer.get("ip").getAsString();
							int port = jsonServer.get("port").getAsInt();
							DockerServerData dockerServerData = new DockerServerData(name, fullId, ip, port);
							tempServerData.put(name, dockerServerData);
						}
					}
				}

				clusters.add(jsonObject);
			}

			clusterData.put(cluster, clusters);
		}

		synchronized (syncThread)
		{
			Iterator<String> iterator = tempServerData.keySet().iterator();
			while (iterator.hasNext())
			{
				String key = iterator.next();
				if (!serverData.containsKey(key))
				{
					DockerServerData dockerServerData = tempServerData.get(key);
					ServerInfo server = BungeeCord.getInstance().constructServerInfo(dockerServerData.getServerName(),
							new InetSocketAddress(dockerServerData.getIp(), dockerServerData.getPort()), dockerServerData.getServerName(), false);
					// Add the server
					BungeeCord.getInstance().getServers().put(dockerServerData.getServerName(), server);
					BadBungee.log("§d[Docker] §aAdded server: §e" + key + " §ahosted by §e" + dockerServerData.getSource());
				}
				else
				{
					try
					{
						DockerServerData dockerServerData = tempServerData.get(key);
						DockerServerData current = serverData.get(key);
						if (!dockerServerData.equals(current))
						{
							iterator.remove();
							BadBungee.log("§d[Docker] §cRemoved server: §e" + key + " §c(because of difference, wait for being updated).");
						}
					}
					catch (Exception error)
					{
						error.printStackTrace();
					}
				}
			}

			for (String key : serverData.keySet())
			{
				if (!tempServerData.containsKey(key))
				{
					DockerServerData dockerServerData = serverData.get(key);
					BungeeCord.getInstance().getServers().remove(dockerServerData.getServerName());
					BadBungee.log("§d[Docker] §cRemoved server: §e" + key + " §chosted by §e" + dockerServerData.getSource());
				}
			}
		}

		serverData = tempServerData;
	}

	/**
	 * Constructor of a new Bungee task Automatic task start when instantiating
	 */
	public ServerSyncDockerTask() {
		// Start the thread
		this.start();
	}

	@Override
	/**
	 * Data sending loop method
	 */
	public void run() {
		// While the task is allowed to run
		while (run) {
			// Send a keep alive packet
			sync();
			// Sleep 1 second
			TimeUtils.sleepInSeconds(2);
		}
	}

}
