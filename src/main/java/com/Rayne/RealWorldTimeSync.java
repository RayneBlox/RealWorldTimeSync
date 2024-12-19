package com.Rayne;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RealWorldTimeSync extends JavaPlugin {
    private String API_KEY;
    private String CITY;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();  // Load initial config values
        Bukkit.getScheduler().runTaskTimer(this, this::syncTime, 0L, 1200L);
    }

    private void loadConfigValues() {
        this.API_KEY = getConfig().getString("main.api");
        this.CITY = getConfig().getString("main.location");
    }

    private void syncTime() {
        int minecraftTime = getRealWorldTimeAsMinecraftTime();
        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setTime(minecraftTime);

            String weatherCondition = getRealWorldWeatherCondition();
            if (weatherCondition != null) {
                syncWeather(world, weatherCondition);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("RTTS.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            reloadConfig();
            loadConfigValues();  // Reload the configuration values
            Bukkit.getScheduler().cancelTasks(this);
            Bukkit.getScheduler().runTaskTimer(this, this::syncTime, 0L, 1200L);
            sender.sendMessage("§aConfiguration reloaded successfully!");
            getLogger().info("Config reloaded successfully.");
            return true;
        }

        return false;
    }

    private int getRealWorldTimeAsMinecraftTime() {
        try {
            String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&appid=" + API_KEY;

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder jsonResponse = new StringBuilder();
            while (scanner.hasNext()) {
                jsonResponse.append(scanner.nextLine());
            }
            scanner.close();

            if (getConfig().getBoolean("main.debug")) {
                getLogger().info("API response: " + jsonResponse.toString());
            }

            int timezoneOffsetSeconds = new JSONObject(jsonResponse.toString()).getInt("timezone");

            long realTimeInMillis = System.currentTimeMillis() + (timezoneOffsetSeconds * 1000L);

            int hours = (int) ((realTimeInMillis / (1000 * 60 * 60)) % 24);
            int minutes = (int) ((realTimeInMillis / (1000 * 60)) % 60);

            int minecraftTimeInTicks = (hours - 6 + 24) % 24 * 1000 + (minutes * (1000 / 60));

            return minecraftTimeInTicks % 24000;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getRealWorldWeatherCondition() {
        try {
            String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&appid=" + API_KEY;
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder jsonResponse = new StringBuilder();
            while (scanner.hasNext()) {
                jsonResponse.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject jsonObject = new JSONObject(jsonResponse.toString());
            return jsonObject.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void syncWeather(World world, String weatherCondition) {
        switch (weatherCondition.toLowerCase()) {
            case "clear":
                world.setStorm(false);
                world.setThundering(false);
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                break;
            case "thunderstorm":
                world.setStorm(true);
                world.setThundering(true);
                break;
            default:
                world.setStorm(false);
                world.setThundering(false);
                break;
        }
    }
}