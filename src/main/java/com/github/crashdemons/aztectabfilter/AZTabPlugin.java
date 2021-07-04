package com.github.crashdemons.aztectabfilter;

import com.github.crashdemons.aztectabfilter.filters.FilterArgs;
import com.github.crashdemons.aztectabfilter.filters.FilterSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * @author crashdemons
 */
public class AZTabPlugin extends JavaPlugin{
	// internal variables
	private final FilterSet filters;
	private final ListenerWhenLoaded listeners;

	// runtime behavior variables
	private volatile boolean ready = false;

	private boolean kickEarlyJoins = true;
	private String kickMessage = "The server is still loading - check back in a moment!";

	private boolean dumpFiltering = false;

	public AZTabPlugin(){
		filters = new FilterSet(this);
		listeners = new ListenerWhenLoaded();
	}

	private void loadConfig(){
		saveDefaultConfig();// fails silently if config exists
		reloadConfig();

		filters.load(getConfig());
		kickEarlyJoins = getConfig().getBoolean("kick-early-joins");
		kickMessage = getConfig().getString("kick-message");
	}

	@Override public void onEnable(){
		getServer().getPluginManager().registerEvents(listeners, this);
		ready = true;
	}
	
	@Override public void onDisable(){
		HandlerList.unregisterAll(listeners);
	}

	@Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		String command = cmd.getName();
		if(command.equalsIgnoreCase("aztabreload")){
			if(!sender.hasPermission("aztectabfilter.reload")){
				sender.sendMessage("You don't have permission to do this.");
				return true;
			}
			loadConfig();
			sender.sendMessage("[AZTab] Config reloaded.");
			return true;
		}
		else if(command.equalsIgnoreCase("aztabdump")){
			if(!sender.hasPermission("aztectabfilter.dump")){
				sender.sendMessage("You don't have permission to do this.");
				return true;
			}
			dumpFiltering = !dumpFiltering;
			String dumpResult = dumpFiltering ? "Enabled" : "Disabled";
			sender.sendMessage("[AZTab] Console Filter Logging: " + dumpResult);
			return true;
		}
		return false;
	}

	class ListenerWhenLoaded implements Listener {
		@EventHandler
		public void onCommandSuggestion(PlayerCommandSendEvent event){
			if(event.isAsynchronous()) return;

			Player player = event.getPlayer();
			if(player.hasPermission("aztectabfilter.bypass")){
				if(dumpFiltering) getLogger().info(player.getName() + " bypassed filtering by permission.");
				return;
			}
			if(!ready){
				if(dumpFiltering) getLogger().info(player.getName() + " denied suggestions - plugin not ready.");
				event.getCommands().clear();
				return;
			}
			if(!player.hasPermission("aztectabfilter.suggest")){
				if(dumpFiltering) getLogger().info(player.getName() + " denied suggestions by permission.");
				event.getCommands().clear();
			}
			else{
				if(dumpFiltering) getLogger().info(player.getName() + " commands,  pre-filter: " + event.getCommands());
				event.getCommands().removeIf(entry -> !filters.filter(new FilterArgs(player, entry)).isAllowed);
				if(dumpFiltering) getLogger().info(player.getName() + " commands, post-filter: " + event.getCommands());
			}
		}

		@EventHandler public void onPlayerLogin(PlayerLoginEvent event){
			if(!ready && kickEarlyJoins){
				event.setKickMessage(kickMessage);
				event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
			}
		}
	}
}
