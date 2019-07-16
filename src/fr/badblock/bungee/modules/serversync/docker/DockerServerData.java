package fr.badblock.bungee.modules.serversync.docker;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DockerServerData
{

	private String	serverName;

	private String	source;
	private String	ip;
	private int			port;

}
