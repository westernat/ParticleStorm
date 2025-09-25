package org.mesdag.particlestorm.particle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;

import java.util.Collection;

public class MolangParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("particlestorm").requires(sourceStack -> sourceStack.hasPermission(2))
                .then(Commands.literal("add").then(Commands.argument("particle", ResourceLocationArgument.id()).executes(context -> sendParticle(
                                context.getSource(),
                                ResourceLocationArgument.getId(context, "particle"),
                                context.getSource().getPosition(),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("pos", Vec3Argument.vec3()).executes(context -> sendParticle(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "particle"),
                                        Vec3Argument.getVec3(context, "pos"),
                                        context.getSource().getServer().getPlayerList().getPlayers()
                                )).then(Commands.argument("viewers", EntityArgument.players()).executes(context -> sendParticle(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "particle"),
                                                Vec3Argument.getVec3(context, "pos"),
                                                EntityArgument.getPlayers(context, "viewers")
                                        )
                                ))
                        )
                ))
                .then(Commands.literal("remove").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(context -> removeParticle(
                                IntegerArgumentType.getInteger(context, "id"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).executes(context -> removeParticle(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                ))
                .then(Commands.literal("attach").then(Commands.argument("id", IntegerArgumentType.integer(0)).then(Commands.argument("entity", EntityArgument.entity()).executes(context -> attachEmitter2Entity(
                                IntegerArgumentType.getInteger(context, "id"),
                                EntityArgument.getEntity(context, "entity"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).executes(context -> attachEmitter2Entity(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getEntity(context, "entity"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                )))
        );
    }

    private static int attachEmitter2Entity(int id, Entity entity, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            EmitterAttachPacketS2C.sendToClient(serverplayer, id, entity);
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            return i;
        }
    }

    private static int removeParticle(int id, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            EmitterRemovalPacket.sendToClient(serverplayer, id);
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            return i;
        }
    }

    private static int sendParticle(CommandSourceStack source, ResourceLocation particle, Vec3 pos, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            PacketDistributor.sendToPlayer(serverplayer, new EmitterCreationPacketS2C(particle, pos.toVector3f(), ParticleEffect.Type.EMITTER, MolangExp.EMPTY));
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(() -> Component.translatable("commands.particle.success", particle.toString()), true);
            return i;
        }
    }
}
