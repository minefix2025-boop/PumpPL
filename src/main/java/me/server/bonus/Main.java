package me.pumpworld.core;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    // ===== SYSTEMS =====
    private final HashMap<String, Location> pos1 = new HashMap<>();
    private final HashMap<String, Location> pos2 = new HashMap<>();
    private final HashMap<String, Region> regions = new HashMap<>();
    private final HashMap<String, Location> lastDeath = new HashMap<>();
    private final HashMap<String, String> tpa = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ================= REGION =================
    static class Region {
        String name;
        Location p1, p2;
        Set<String> members = new HashSet<>();
        boolean pvp = true;

        Region(String name, Location p1, Location p2) {
            this.name = name;
            this.p1 = p1;
            this.p2 = p2;
        }

        boolean inside(Location l) {
            if (l == null || p1 == null || p2 == null) return false;
            if (!l.getWorld().equals(p1.getWorld())) return false;

            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());

            int minY = Math.min(p1.getBlockY(), p2.getBlockY());
            int maxY = Math.max(p1.getBlockY(), p2.getBlockY());

            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            return l.getBlockX() >= minX && l.getBlockX() <= maxX &&
                    l.getBlockY() >= minY && l.getBlockY() <= maxY &&
                    l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ;
        }
    }

    // ================= PROTECTION =================
    @EventHandler
    public void breakBlock(BlockBreakEvent e) {
        if (!canBuild(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cПриват!");
        }
    }

    @EventHandler
    public void placeBlock(BlockPlaceEvent e) {
        if (!canBuild(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cПриват!");
        }
    }

    private boolean canBuild(Player p, Location l) {
        for (Region r : regions.values()) {
            if (r.inside(l)) {
                return r.members.contains(p.getName()) || p.hasPermission("rg.admin");
            }
        }
        return true;
    }

    @EventHandler
    public void death(PlayerDeathEvent e) {
        lastDeath.put(e.getEntity().getName(), e.getEntity().getLocation());
    }

    // ================= COMMANDS =================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) return true;

        switch (cmd.getName().toLowerCase()) {

            // ===== ADMIN =====
            case "admin" -> {
                if (args.length < 2) return true;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + args[0] + " parent set " + args[1]);
                p.sendMessage("§aГруппа выдана");
            }

            // ===== CORE =====
            case "fly" -> p.setAllowFlight(!p.getAllowFlight());

            case "heal" -> {
                p.setHealth(20);
                p.setFoodLevel(20);
            }

            case "feed" -> p.setFoodLevel(20);

            case "coords" -> p.sendMessage("X:" + p.getX() + " Y:" + p.getY() + " Z:" + p.getZ());

            case "ping" -> p.sendMessage("§aPing: ~50ms");

            // ===== RTP / BACK =====
            case "rtp" -> {
                World w = p.getWorld();
                Random r = new Random();
                int x = r.nextInt(2000) - 1000;
                int z = r.nextInt(2000) - 1000;
                int y = w.getHighestBlockYAt(x, z) + 1;
                p.teleport(new Location(w, x, y, z));
            }

            case "back" -> {
                Location l = lastDeath.get(p.getName());
                if (l != null) p.teleport(l);
            }

            // ===== TPA =====
            case "tpa" -> {
                Player t = Bukkit.getPlayer(args[0]);
                if (t == null) return true;
                tpa.put(t.getName(), p.getName());
                t.sendMessage("§e/tpaccept или /tpdeny");
            }

            case "tpaccept" -> {
                Player f = Bukkit.getPlayer(tpa.get(p.getName()));
                if (f != null) f.teleport(p.getLocation());
                tpa.remove(p.getName());
            }

            case "tpdeny" -> {
                tpa.remove(p.getName());
            }

            // ===== SHOP =====
            case "shop" -> {
                Inventory inv = Bukkit.createInventory(null, 27, "§6Shop");

                ItemStack item = new ItemStack(Material.DIAMOND);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§bDiamond");
                item.setItemMeta(meta);

                inv.setItem(13, item);
                p.openInventory(inv);
            }

            // ===== WORLD EDIT =====
            case "wand" -> p.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));

            case "set" -> {
                if (args.length < 1) return true;
                Bukkit.dispatchCommand(p, "//set " + args[0]);
            }

            // ===== REGIONS =====
            case "rg" -> {
                if (args.length == 0) return true;

                switch (args[0]) {

                    case "pos1" -> pos1.put(p.getName(), p.getLocation());
                    case "pos2" -> pos2.put(p.getName(), p.getLocation());

                    case "create" -> {
                        if (args.length < 2) return true;

                        Region r = new Region(
                                args[1],
                                pos1.get(p.getName()),
                                pos2.get(p.getName())
                        );

                        regions.put(args[1], r);
                    }

                    case "tp" -> {
                        Region r = regions.get(args[1]);
                        if (r != null) p.teleport(r.p1);
                    }

                    case "add" -> {
                        Region r = regions.get(args[2]);
                        if (r != null) r.members.add(args[1]);
                    }

                    case "flag" -> {
                        Region r = regions.get(args[1]);
                        if (r != null && args[2].equals("pvp")) {
                            r.pvp = args[3].equals("allow");
                        }
                    }
                }
            }
        }

        return true;
    }
}
