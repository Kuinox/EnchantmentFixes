package com.kuinox.enchantmentfixes;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable(){
        getServer().getPluginManager().registerEvents(new EnchantmentListener(getLogger()), this);
        getLogger().info("EnchantmentFixes enabled.");
    }

    @Override
    public void onDisable(){
        getLogger().info("EnchantmentFixes disabled.");
    }


}
