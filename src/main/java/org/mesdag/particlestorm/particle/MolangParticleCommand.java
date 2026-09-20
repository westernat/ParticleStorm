package org.mesdag.particlestorm.particle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MolangParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particlestorm.failed"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.particlestorm.unknown_particle", id)
    );
    private static final List<String> POSITION_SUGGESTIONS = List.of("~ ~ ~", "~ ~1 ~", "~ ~-1 ~", "^ ^ ^", "^ ^ ^1");
    private static final List<String> EXPRESSION_SUGGESTIONS = List.of("\"\"", "\"v.size=1;\"", "\"v.alpha=1;\"", "\"v.size=1;v.alpha=1;\"");
    private static final List<String> ENTITY_SUGGESTIONS = List.of("@s", "@p", "@e[limit=1,sort=nearest]");
    private static final List<String> VIEWER_SUGGESTIONS = List.of("@a", "@p", "@s");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("particlestorm").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add").then(Commands.argument("particle", IdentifierArgument.id()).suggests((context, builder) ->
                        SharedSuggestionProvider.suggestResource(PSGameClient.LOADER.suggestibleParticleIds(), builder)
                ).executes(context -> sendParticle(
                                context.getSource(),
                                IdentifierArgument.getId(context, "particle"),
                                context.getSource().getPosition(),
                                MolangExp.EMPTY,
                                null,
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("pos", Vec3Argument.vec3()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(POSITION_SUGGESTIONS, builder)
                        ).executes(context -> sendParticle(
                                        context.getSource(),
                                        IdentifierArgument.getId(context, "particle"),
                                        Vec3Argument.getVec3(context, "pos"),
                                        MolangExp.EMPTY,
                                        null,
                                        context.getSource().getServer().getPlayerList().getPlayers()
                                )).then(Commands.argument("expression", StringArgumentType.string()).suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EXPRESSION_SUGGESTIONS, builder)
                                ).executes(context -> sendParticle(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "particle"),
                                                Vec3Argument.getVec3(context, "pos"),
                                                new MolangExp(StringArgumentType.getString(context, "expression")),
                                                null,
                                                context.getSource().getServer().getPlayerList().getPlayers()
                                        )).then(Commands.argument("attach", EntityArgument.entity()).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(ENTITY_SUGGESTIONS, builder)
                                        ).executes(context -> sendParticle(
                                                        context.getSource(),
                                                        IdentifierArgument.getId(context, "particle"),
                                                        Vec3Argument.getVec3(context, "pos"),
                                                        new MolangExp(StringArgumentType.getString(context, "expression")),
                                                        EntityArgument.getEntity(context, "attach"),
                                                        context.getSource().getServer().getPlayerList().getPlayers()
                                                )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                                                ).executes(context -> sendParticle(
                                                                context.getSource(),
                                                                IdentifierArgument.getId(context, "particle"),
                                                                Vec3Argument.getVec3(context, "pos"),
                                                                new MolangExp(StringArgumentType.getString(context, "expression")),
                                                                EntityArgument.getEntity(context, "attach"),
                                                                EntityArgument.getPlayers(context, "viewers")
                                                        ))
                                                )
                                        )
                                )
                        )
                ))
                .then(Commands.literal("remove").then(Commands.argument("id", IntegerArgumentType.integer(0)).suggests(MolangParticleCommand::suggestEmitterIds).executes(context -> removeParticle(
                                IntegerArgumentType.getInteger(context, "id"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                        ).executes(context -> removeParticle(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                ))
                .then(Commands.literal("attach").then(Commands.argument("id", IntegerArgumentType.integer(0)).suggests(MolangParticleCommand::suggestEmitterIds).then(Commands.argument("entity", EntityArgument.entity()).suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(ENTITY_SUGGESTIONS, builder)
                ).executes(context -> attachEmitter2Entity(
                                IntegerArgumentType.getInteger(context, "id"),
                                EntityArgument.getEntity(context, "entity"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                        ).executes(context -> attachEmitter2Entity(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getEntity(context, "entity"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                )))
        );
    }

    private static CompletableFuture<Suggestions> suggestEmitterIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (ParticleEmitter emitter : PSGameClient.LOADER.getEmitters()) {
            builder.suggest(emitter.id);
        }
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(0);
        }
        return builder.buildFuture();
    }

    private static int attachEmitter2Entity(int id, Entity entity, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            EmitterAttachPacketS2C.sendToClient(serverplayer, id, entity);
            i++;
        }
        PSDiagnostics.info("command attach runtimeId={} entity={} viewers={}", id, entity.getScoreboardName(), viewers.stream().map(ServerPlayer::getScoreboardName).toList());
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
        PSDiagnostics.info("command remove runtimeId={} viewers={}", id, viewers.stream().map(ServerPlayer::getScoreboardName).toList());
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            return i;
        }
    }

    private static int sendParticle(CommandSourceStack source, Identifier particle, Vec3 pos, MolangExp expression, @Nullable Entity entity, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        Identifier resolved = PSGameClient.LOADER.resolveParticleId(particle);
        if (resolved == null) {
            throw ERROR_UNKNOWN_PARTICLE.create(particle);
        }
        int i = 0;
        PSDiagnostics.info("command add requested={} resolved={} pos={} expression={} attached={} viewers={}",
                particle,
                resolved,
                pos,
                expression == null ? "" : expression.getExpStr(),
                entity == null ? "none" : entity.getScoreboardName(),
                viewers.stream().map(ServerPlayer::getScoreboardName).toList()
        );
        for (ServerPlayer player : viewers) {
            EmitterCreationPacketS2C.sendToClient(player, resolved, pos.toVector3f(), expression, entity);
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(() -> Component.translatable("commands.particlestorm.add", resolved.toString()), true);
            return i;
        }
    }
}
