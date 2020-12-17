package rusbik.helpers;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import rusbik.Rusbik;
import rusbik.database.RusbikDatabase;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {
    private static final Pattern url_patt = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
    private static JDA jda = null;
    public static String channelId = "";
    public static String token = "";
    public static boolean chatBridge = false;

    MinecraftServer server;

    public DiscordListener (MinecraftServer s){
        this.server = s;
    }

    public static void connect(MinecraftServer server, String t, String c){
        token = t;
        channelId = c;
        try{
            chatBridge = false;
            Rusbik.config.setRunning(false);
            jda = JDABuilder.createDefault(token).addEventListeners(new DiscordListener(server)).build();
            jda.awaitReady();
            chatBridge = true;
            Rusbik.config.setRunning(true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (chatBridge){
            if (event.getAuthor().isBot()) return;
            if (event.getMessage().getContentDisplay().equals("")) return;
            if (event.getMessage().getContentRaw().equals("")) return;
            if (event.getMessage().getContentRaw().equals("!online")) {
                if (DiscordUtils.isAllowed(2, event.getChannel().getIdLong())) {
                    StringBuilder msg = new StringBuilder();
                    int n = server.getPlayerManager().getPlayerList().size();
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        msg.append(player.getName().getString().replace("_", "\\_")).append("\n");
                    }
                    event.getChannel().sendMessage(Objects.requireNonNull(DiscordUtils.generateEmbed(msg, n)).build()).queue();
                }
            }

            else if (event.getMessage().getContentRaw().startsWith("!add ")) {
                if (DiscordUtils.isAllowed(1, event.getChannel().getIdLong())) {
                    String[] req = event.getMessage().getContentRaw().split(" ");
                    if (req.length == 2){
                        Whitelist whitelist = server.getPlayerManager().getWhitelist();
                        GameProfile gameProfile = server.getUserCache().findByName(req[1]);
                        if (gameProfile != null){
                            if (!whitelist.isAllowed(gameProfile)){
                                WhitelistEntry whitelistEntry = new WhitelistEntry(gameProfile);
                                long id = Long.parseLong(event.getAuthor().getId());
                                try {
                                    if (RusbikDatabase.hasPlayer(id)) {
                                        event.getChannel().sendMessage("Solo puedes meter en la whitelist a 1 persona").queue();
                                    }
                                    else {
                                        RusbikDatabase.addPlayerInformation(req[1], id);
                                        whitelist.add(whitelistEntry);
                                        event.getChannel().sendMessage("Añadido :)").queue();
                                        Guild guild = event.getGuild();
                                        Role role = guild.getRoleById(788922147841507371L);
                                        assert role != null;
                                        guild.addRoleToMember(Objects.requireNonNull(event.getMember()), role).queue();
                                    }
                                } catch (SQLException throwables) {
                                    whitelist.remove(whitelistEntry);
                                    event.getChannel().sendMessage("RIP :(, algo falló.").queue();
                                    throwables.printStackTrace();
                                }
                            }
                            else event.getChannel().sendMessage("Ya estaba en whitelist").queue();
                        }
                        else event.getChannel().sendMessage("No es premium :P").queue();
                    }
                    else event.getChannel().sendMessage("!add <playerName>").queue();
                }
            }

            else if (event.getMessage().getContentRaw().startsWith("!remove ")) {
                if (DiscordUtils.isAllowed(1, event.getChannel().getIdLong())) {
                    String[] req = event.getMessage().getContentRaw().split(" ");
                    if (req.length == 2){
                        Whitelist whitelist = server.getPlayerManager().getWhitelist();
                        GameProfile gameProfile = server.getUserCache().findByName(req[1]);
                        if (gameProfile != null){
                            if (whitelist.isAllowed(gameProfile)) {
                                long id = Long.parseLong(event.getAuthor().getId());
                                try {
                                    if (RusbikDatabase.allowedToRemove(id, req[1])) {
                                        RusbikDatabase.removeData(req[1]);
                                        WhitelistEntry whitelistEntry = new WhitelistEntry(gameProfile);
                                        whitelist.remove(whitelistEntry);
                                        event.getChannel().sendMessage("Eliminado ;(").queue();
                                        event.getGuild().removeRoleFromMember(Objects.requireNonNull(event.getMember()), Objects.requireNonNull(event.getGuild().getRoleById(788922147841507371L))).queue();
                                    }
                                    else {
                                        event.getChannel().sendMessage("No tienes permiso para eliminar a este usuario").queue();
                                    }
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                            }
                            else event.getChannel().sendMessage("No está en la whitelist").queue();
                        }
                        else event.getChannel().sendMessage("No es premium :P").queue();
                    }
                    else event.getChannel().sendMessage("!remove <playerName>").queue();
                }
            }

            else if (event.getMessage().getContentRaw().equals("!reload")) {
                if (DiscordUtils.isAllowed(0, event.getChannel().getIdLong())) {
                    server.getPlayerManager().reloadWhitelist();
                    server.kickNonWhitelistedPlayers(server.getCommandSource());
                    DiscordFileManager.initializeYaml();
                }
            }

            else if (event.getMessage().getContentRaw().equals("!list")) {
                if (DiscordUtils.isAllowed(1, event.getChannel().getIdLong())) {
                    String[] names = server.getPlayerManager().getWhitelistedNames();
                    if (names.length == 0) {
                        event.getChannel().sendMessage("Whitelist is empty").queue();
                    } else {
                        StringBuilder msg = new StringBuilder("`");
                        for (int i = 0; i < names.length - 1; i++){
                            msg.append(names[i]);
                            if (msg.length() < 1500) msg.append(", ");
                            else {
                                event.getChannel().sendMessage(msg.append("`")).queue();
                                msg.setLength(0);
                                msg.append("`");
                            }
                        }
                        event.getChannel().sendMessage(msg.append(names[names.length - 1]).append("`")).queue();
                    }
                }
            }

            else if (event.getMessage().getContentRaw().startsWith("!give ")) {
                if (DiscordUtils.isAllowed(0, event.getChannel().getIdLong())) {
                    String[] req = event.getMessage().getContentRaw().split(" ");
                    if (req.length == 3){
                        String player = req[1];
                        try {
                            int permsInt = Integer.parseInt(req[2]);
                            if (permsInt > 0 && permsInt < 4) {
                                try {
                                    RusbikDatabase.updatePerms(player, permsInt);
                                    event.getChannel().sendMessage(String.format("Player %s => %d", player, permsInt)).queue();
                                }
                                catch (Exception e){
                                    event.getChannel().sendMessage("unable to set perms").queue();
                                }
                            }
                            else event.getChannel().sendMessage("Pls input an integer between 1 and 3").queue();
                        }
                        catch (Exception e){
                            event.getChannel().sendMessage("Pls input an integer between 1 and 3").queue();
                        }
                    }
                    else event.getChannel().sendMessage("How to: !give <playerName> <int 1 to 3>").queue();
                }
                else event.getChannel().sendMessage("You can't use this command here").queue();
            }

            else if (event.getChannel().getIdLong() == (Rusbik.config.chatChannelId)) {
                String msg = "[Discord] <" + event.getAuthor().getName() + "> " + event.getMessage().getContentDisplay();
                if (msg.length() >= 256) msg = msg.substring(0, 253) + "...";

                Matcher m = url_patt.matcher(msg);
                MutableText finalMsg = new LiteralText("");
                boolean hasUrl = false;
                int prev = 0;

                while (m.find()){
                    hasUrl = true;
                    Text text = new LiteralText(m.group(0)).styled((style -> style.withColor(Formatting.GRAY)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, m.group(0)))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Open URL")))));
                    finalMsg = finalMsg.append(new LiteralText(msg.substring(prev, m.start()))).append(text);
                    prev = m.end();
                }
                if (hasUrl) server.getPlayerManager().broadcastChatMessage(finalMsg.append(msg.substring(prev)), MessageType.CHAT, Util.NIL_UUID);
                else server.getPlayerManager().broadcastChatMessage(new LiteralText(msg), MessageType.CHAT, Util.NIL_UUID);
            }
        }
    }

    public static void sendMessage(String msg) {
        if (chatBridge){
            try {
                TextChannel ch = jda.getTextChannelById(channelId);
                if (ch != null) ch.sendMessage(msg).queue();
            }
            catch (Exception e){
                System.out.println("wrong channelId :(");
            }
        }
    }

    public static void stop() {
        jda.shutdownNow();
        chatBridge = false;
    }
}