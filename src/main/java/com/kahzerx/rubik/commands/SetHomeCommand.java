package com.kahzerx.rubik.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import com.kahzerx.rubik.database.RusbikDatabase;
import com.kahzerx.rubik.utils.KrusbibUtils;

import static net.minecraft.server.command.CommandManager.literal;

public final class SetHomeCommand {
    /**
     * Configurar una "home" para hacerte tp mediante el comando /home.
     * @param dispatcher register command.
     */
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("setHome").
                executes(context -> setHome(context.getSource())));
    }

    private SetHomeCommand() { }

    public static int setHome(final ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity playerEntity = source.getPlayer();
        if (playerEntity != null) {
            try {
                if (RusbikDatabase.userExists(playerEntity.getName().getString())) {
                    if (RusbikDatabase.getPlayerPerms(source.getPlayer().getName().getString()) > 0) {
                        // Actualizar la base de datos y mensaje.
                        RusbikDatabase.updateHomeInformation(
                                playerEntity.getName().getString(),
                                playerEntity.getX(),
                                playerEntity.getY(),
                                playerEntity.getZ(),
                                KrusbibUtils.getDim(playerEntity.world));
                        playerEntity.setSpawnPoint(
                                playerEntity.world.getRegistryKey(),
                                playerEntity.getBlockPos(),
                                0.0F,
                                true,
                                false);
                        source.sendFeedback(
                                new LiteralText(
                                        String.format(
                                                "Casa en: %s %s",
                                                KrusbibUtils.getDimensionWithColor(playerEntity.world),
                                                KrusbibUtils.formatCoords(playerEntity.getX(),
                                                        playerEntity.getY(),
                                                        playerEntity.getZ()))),
                                false);
                    } else {
                        source.sendFeedback(
                                new LiteralText(
                                        "No puedes usar este comando :P"),
                                false);
                    }
                } else {
                    source.sendFeedback(
                            new LiteralText(
                                    "Parece que no estás registrado correctamente "
                                            + "y no puedes ejecutar esta acción."),
                            false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 1;
    }
}
