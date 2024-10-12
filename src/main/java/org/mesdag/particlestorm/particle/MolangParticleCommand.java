package org.mesdag.particlestorm.particle;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.network.EmitterCreationPacketC2S;

import java.util.Collection;

public class MolangParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("particlestorm").requires(sourceStack -> sourceStack.hasPermission(2))
                .then(Commands.argument("id", ResourceLocationArgument.id()).executes(context -> sendParticles(
                                context.getSource(),
                                ResourceLocationArgument.getId(context, "id"),
                                context.getSource().getPosition(),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("pos", Vec3Argument.vec3()).executes(context -> sendParticles(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "id"),
                                        Vec3Argument.getVec3(context, "pos"),
                                        context.getSource().getServer().getPlayerList().getPlayers()
                                )).then(Commands.argument("viewers", EntityArgument.players()).executes(context -> sendParticles(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id"),
                                                Vec3Argument.getVec3(context, "pos"),
                                                EntityArgument.getPlayers(context, "viewers")
                                        )
                                ))
                        )
                )
        );
    }

    private static int sendParticles(CommandSourceStack source, ResourceLocation id, Vec3 pos, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            PacketDistributor.sendToPlayer(serverplayer, new EmitterCreationPacketC2S(id, new Vector3f((float) pos.x, (float) pos.y, (float) pos.z), ParticleEffect.Type.EMITTER, MolangExp.EMPTY));
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(() -> Component.translatable("commands.particle.success", id.toString()), true);
            return i;
        }
    }
}
