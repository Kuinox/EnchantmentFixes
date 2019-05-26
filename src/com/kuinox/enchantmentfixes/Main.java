package com.kuinox.enchantmentfixes;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

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
