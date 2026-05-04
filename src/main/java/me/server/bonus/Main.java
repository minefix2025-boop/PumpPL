package ru.yourname.dailyreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements CommandExecutor {

    // 24 часа в миллисекундах
    private final long COOLDOWN_MS = 24 * 60 * 60 * 1000; 

    @Override
    public void onEnable() {
        // Создаем конфиг, если его нет
        saveDefaultConfig();
        // Регистрация команд
        getCommand("reward").setExecutor(this);
        getCommand("lefttime").setExecutor(this);
        getCommand("skiptime").setExecutor(this);
        getLogger().info("DailyReward активирован!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команды доступны только игрокам!");
            return true;
        }

        Player player = (Player) sender;
        String path = "players." + player.getUniqueId();

        // КОМАНДА /REWARD
        if (cmd.getName().equalsIgnoreCase("reward")) {
            long lastUsed = getConfig().getLong(path, 0);
            long now = System.currentTimeMillis();

            if (now - lastUsed >= COOLDOWN_MS) {
                // Выдаем награду (алмаз)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " diamond 1");
                player.sendMessage(ChatColor.GREEN + "✔ Вы получили ежедневную награду!");
                
                // Сохраняем время получения
                getConfig().set(path, now);
                saveConfig();
            } else {
                long timeLeft = (lastUsed + COOLDOWN_MS) - now;
                player.sendMessage(ChatColor.RED + "✘ Награда еще не доступна! Подождите " + formatTime(timeLeft));
            }
            return true;
        }

        // КОМАНДА /LEFTTIME
        if (cmd.getName().equalsIgnoreCase("lefttime")) {
            long lastUsed = getConfig().getLong(path, 0);
            long now = System.currentTimeMillis();
            long timeLeft = (lastUsed + COOLDOWN_MS) - now;

            if (timeLeft <= 0) {
                player.sendMessage(ChatColor.GREEN + "★ Награда уже доступна! Используйте /reward");
            } else {
                player.sendMessage(ChatColor.YELLOW + "⏳ До следующей награды осталось: " + formatTime(timeLeft));
            }
            return true;
        }

        // КОМАНДА /SKIPTIME (для админов)
        if (cmd.getName().equalsIgnoreCase("skiptime")) {
            if (!player.hasPermission("dailyreward.admin")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав (dailyreward.admin)!");
                return true;
            }
            // Сбрасываем время в 0
            getConfig().set(path, 0);
            saveConfig();
            player.sendMessage(ChatColor.AQUA + "⚡ Время ожидания сброшено! Теперь вы можете взять /reward");
            return true;
        }

        return false;
    }

    // Вспомогательный метод для красивого вывода времени
    private String formatTime(long ms) {
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms / (1000 * 60)) % 60;
        long seconds = (ms / 1000) % 60;
        return String.format("%02dч. %02dмин. %02dсек.", hours, minutes, seconds);
    }
}
