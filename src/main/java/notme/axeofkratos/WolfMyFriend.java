package notme.axeofkratos;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class WolfMyFriend implements CommandExecutor, Listener {

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, Brain.getInstance());
        new WolfGlowTask().runTaskTimer(Brain.getInstance(), 0L, 100L);
        Brain.getInstance().getCommand("pet").setExecutor(this);
        Brain.getInstance().getLogger().info(String.valueOf(getClass().getSimpleName()) + " module has been initialized! (Perms: perks.wolfmyfriend.teleport, perks.wolfmyfriend.fetching-items)");
    }
    
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
    	Entity attacked = event.getEntity();
        Entity damager = event.getDamager();
        if (attacked instanceof Wolf && damager instanceof Player) {
        	Wolf wolf = (Wolf) attacked;
        	Player player = (Player) damager;
        	if (wolf.isTamed() && wolf.getOwner().equals(player)) {
        		event.setCancelled(true);
        	}
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getEntity();
            if (wolf.isTamed()) {
                if (event.getCause() == DamageCause.PROJECTILE) {
                    Projectile projectile = (Projectile) ((EntityDamageByEntityEvent) event).getDamager();
                    if (projectile.getShooter() instanceof Player) {
                        Player shooter = (Player) projectile.getShooter();
                        if (shooter.equals(wolf.getOwner())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                if (event.getCause() == DamageCause.SUFFOCATION) {
                    event.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getRightClicked() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getRightClicked();
            if (wolf.isTamed() && wolf.getOwner().equals(player) && player.isSneaking()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.of("#ffabf5") + "Ви лагідно погладили свого вовка!"));
                wolf.getWorld().playSound(wolf.getLocation(), "entity.wolf.whine", 1.0f, 1.0f);
                wolf.getWorld().spawnParticle(Particle.HEART, wolf.getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
               	event.setCancelled(true);
                Vector direction = player.getLocation().toVector().subtract(wolf.getLocation().toVector()).normalize();
                wolf.setVelocity(direction.multiply(0.2));
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (!player.hasPermission("perks.wolfmyfriend.teleport")) {
        	player.sendMessage(ChatColor.RED + "У вас немає дозволу на використання цієї команди.");
        	return true;
        }
        boolean teleported = Bukkit.getWorlds().stream()
        	    .flatMap(world -> world.getEntitiesByClass(Wolf.class).stream())
        	    .filter(wolf -> wolf.isTamed() && wolf.getOwner() != null && wolf.getOwner().equals(player) && !wolf.isSitting())
        	    .peek(wolf -> wolf.teleport(player))
        	    .count() > 0;

        player.sendMessage(teleported ? ChatColor.GREEN + "Усіх ваших улюбленців телепортовано до вас!" : ChatColor.RED + "У вас немає улюбленців, або вони сидять.");
        return true;
    }
    
    public class WolfGlowTask extends BukkitRunnable {
        @Override
        public void run() {
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("perks.wolfmyfriend.fetching-items")).forEach(player -> {
                player.getWorld().getEntitiesByClass(Wolf.class).stream()
                    .filter(wolf -> wolf.isTamed() && wolf.getOwner() != null && wolf.getOwner().equals(player) && !wolf.isSitting() && !wolf.isAngry())
                    .forEach(wolf -> {
                        // Знаходимо предмети поблизу вовка
                  /*      wolf.getWorld().getNearbyEntities(wolf.getLocation(), 50, 50, 50).stream()
                            .filter(entity -> entity instanceof Item || entity instanceof Arrow)
                            .forEach(entity -> {
                                // Увімкнення свічіння для предметів тільки для власника вовка
                                player.addScoreboardTag("show_glow");
                                entity.setGlowing(true);
                            });
                        */
                        
                        wolf.getWorld().getNearbyEntities(wolf.getLocation(), 50, 50, 50).stream()
                        .filter(entity -> (entity instanceof Item || entity instanceof Arrow || entity instanceof ExperienceOrb) && !entity.getLocation().add(0, -1, 0).getBlock().isPassable() && entity.getLocation().distance(player.getLocation()) >= 5)
                        .min(Comparator.comparingDouble(entity -> entity.getLocation().distance(wolf.getLocation())))
                        .ifPresent(entity -> {
                            wolf.teleport(entity.getLocation());
                            wolf.setAI(false);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    wolf.teleport(player.getLocation().add(0, 0, 1));
                                    if (entity instanceof Arrow) {
                                    	player.getInventory().addItem((((Arrow) entity).getItem()));
                                    	entity.remove();
                                    } else {
                                    	entity.teleport(player.getLocation());
                                    }
                                    wolf.setAI(true);
                                }
                            }.runTaskLater(Brain.getInstance(), 20L);
	                    });
                    });
            });
        }
    }
}
