package de.tomalbrc.avatarrenderer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.tomalbrc.avatarrenderer.impl.AvatarRenderer;
import de.tomalbrc.avatarrenderer.impl.SkinLoader;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AvatarRendererMod implements ModInitializer {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    public static final String MODID = "avatar-renderer";
    public static Consumer<Component> NOOP = x->{};

    public record Key(String name, int offset, boolean flipped) {
    }

    public static Map<Key, Component> CACHED = new ConcurrentHashMap<>();

    public static void get(String nameOrUUID, int offset, boolean flipped, Consumer<Component> onFinish) {
        Key key = new Key(nameOrUUID, offset, flipped);
        if (CACHED.containsKey(key)) {
            if (onFinish != null) onFinish.accept(CACHED.get(key));
        } else {
            CompletableFuture.supplyAsync(() -> SkinLoader.load(nameOrUUID), EXECUTOR).thenAccept(optionalImage -> optionalImage.ifPresent(image -> {
                BufferedImage avatar = AvatarRenderer.render(image, key.flipped);

                Component asText = AvatarRenderer.asTextComponent(avatar, key.offset);

                CACHED.put(key, asText);

                if (onFinish != null) onFinish.accept(asText);
            })).exceptionally(ex -> {
                Key defaultKey = new Key("Steve", offset, flipped);
                if (CACHED.containsKey(defaultKey)) {
                    if (onFinish != null) onFinish.accept(CACHED.get(defaultKey));
                }

                return null;
            });
        }
    }

    private void addDefault() {
        BufferedImage image;
        try {
            image = ImageIO.read(Objects.requireNonNull(AvatarRendererMod.class.getResourceAsStream("/steve.png")));
            for (int i = 0; i < 256; i++) {
                Key defaultKey = new Key("Steve", i, false);
                Key defaultKey2 = new Key("Steve", i, true);

                BufferedImage avatar = AvatarRenderer.render(image, defaultKey.flipped);
                BufferedImage avatar2 = AvatarRenderer.render(image, defaultKey2.flipped);
                Component asText = AvatarRenderer.asTextComponent(avatar, defaultKey.offset);
                Component asText2 = AvatarRenderer.asTextComponent(avatar2, defaultKey2.offset);
                CACHED.put(defaultKey, asText);
                CACHED.put(defaultKey2, asText2);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets("avatar-renderer");

        addDefault();

        Placeholders.register(ResourceLocation.fromNamespaceAndPath("avatar-renderer", "avatar"), (ctx, arg) -> {
            if (arg == null)
                return PlaceholderResult.invalid();

            String[] split = arg.split(" ");

            String name = split[0];
            int offset = split.length < 2 ? 0 : Integer.parseInt(split[1]);
            boolean flipped = split.length > 2 && split[2].equals("flipped");
            get(name, offset, flipped, NOOP);

            Component res = CACHED.get(new Key(split[0], offset, flipped));
            if (res == null) {
                res = CACHED.get(new Key("Steve", offset, flipped));
            }

            return PlaceholderResult.value(res);
        });

        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {
            commandDispatcher.register(Commands.literal("avatar").requires(x -> x.hasPermission(4) || !x.getServer().isDedicatedServer()).then(Commands.argument("player-name", StringArgumentType.greedyString()).executes(context -> {
                String playerName = StringArgumentType.getString(context, "player-name");

                Component parsed = Placeholders.parseText(TextNode.of("%avatar-renderer:avatar " + playerName + "%"), PlaceholderContext.of(context.getSource().getPlayer()));
                context.getSource().sendSuccess(() -> parsed, false);
                return Command.SINGLE_SUCCESS;
            })));
        });
    }
}
