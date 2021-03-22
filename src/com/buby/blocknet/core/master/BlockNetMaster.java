package com.buby.blocknet.core.master;

import static com.buby.blocknet.util.CommonUtils.log;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.buby.blocknet.BlockNet;
import com.buby.blocknet.core.BlockNetCore;
import com.buby.blocknet.core.Servlet;
import com.buby.blocknet.core.master.model.Slave;
import com.buby.blocknet.core.slave.model.Master;
import com.buby.blocknet.util.CommonUtils.FileUtil;
import com.buby.blocknet.util.CommonUtils.MathUtil;
import com.buby.blocknet.util.model.HeaderModel;
import com.buby.blocknet.util.model.Pair;

import lombok.Getter;

public class BlockNetMaster extends BlockNetCore{
	
	@Getter private Set<Slave> activeSlaves = new HashSet<>();

	public BlockNetMaster() {
		this.commandProcessor = new MasterCommandProcessor();
		FileUtil.deleteFile(BlockNet.BLOCK_NET_CORE_DIR + "/_temp");
		long deployTime = System.currentTimeMillis();

		log("______ _            _    _   _      _   ");
		log("| ___ \\ |          | |  | \\ | |    | |  ");
		log("| |_/ / | ___   ___| | _|  \\| | ___| |_ ");
		log("| ___ \\ |/ _ \\ / __| |/ / . ` |/ _ \\ __|");
		log("| |_/ / | (_) | (__|   <| |\\  |  __/ |_ ");
		log("\\____/|_|\\___/ \\___|_|\\_\\_| \\_/\\___|\\__|");
		log("BlockNet v"+BlockNet.VERSION+" - By Lickymoo/Buby");
		log("https://github.com/Lickymoo");
		log("   ");
		                                        
		log("Starting BlockNet");
		log("Servlet Mode: Master");
		log("   ");

		log("Advertising IP: " + BlockNet.configProfile.getAdvertisementIp());
		log("Advertising API Port: " + BlockNet.configProfile.getApiPort());
		log("   ");
		
		log("Generating _temp folder");
		FileUtil.getOrMkdirs(BlockNet.BLOCK_NET_CORE_DIR + "/_temp/", true);
		log("   ");
		
		log("Registered " + registerServerTemplates() + " template(s)");
		log("   ");
		
		log("Initialising REST API");
		this.restApi = new MasterRestApi();
		log("   ");
		
		//Master is it's own slave
		this.master = new Master(BlockNet.configProfile.getAdvertisementIp(), BlockNet.configProfile.getApiPort());
		
		log("Done! (" + ((System.currentTimeMillis() - deployTime)) + "ms)");
		log("   ");
	}
	
	/* Provision a server on either master or slave
	 * Returns servlet & port of server
	 */
	public Pair<Servlet, Integer> provisionServer(String template) {
		/*Provision server on slave, or if out of bounds on master*/
		try {
			Slave[] slaveArray = activeSlaves.toArray(new Slave[activeSlaves.size()]);
			
			//No slaves, so have to resort to master
			if(slaveArray.length == 0) throw new Exception();
			Slave slave = slaveArray[MathUtil.random(0, activeSlaves.size())];
			
			if(slave.post("/ping") != HttpServletResponse.SC_OK) {
				//Slave is no longer pingable
				log("Could not connect to slave " + slave.getIp() + ":" +slave.getPort());
				log("Terminating connection, this slave will no longer be accessible");
				activeSlaves.remove(slave);
				return provisionServer(template);
			}
			if(slave.post("/from_master/can_host", new HeaderModel("template", template)) != HttpServletResponse.SC_OK) {
				return provisionServer(template);
			}
			
			int port = Integer.parseInt(slave.postComplex("/from_master/provision", new HeaderModel("template", template)).getFirstHeader("port").getValue() );
			return Pair.of(slave, port);
		}catch(StackOverflowError e) {
			return null;
		}catch(Exception e) {
			int port = Integer.parseInt(this.getMaster().postComplex("/from_master/provision", new HeaderModel("template", template)).getFirstHeader("port").getValue() );
			return Pair.of(this.getMaster(), port);
		}
	}
	
	public void registerSlave(Slave slave) {
		activeSlaves.add(slave);
		log("Slave " + slave.getIp() + ":" + slave.getPort() + " registered.");
	}
	
	public void instanceReady(String ip, String port) {
		log("Server instance ready: " + ip + ":" + port);
	}
	
}



















