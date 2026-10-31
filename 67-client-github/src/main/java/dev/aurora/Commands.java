package dev.aurora;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.aurora.module.impl.BlockEsp;
import dev.aurora.module.impl.Friends;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Chat commands:
 *   /aurora blocks add|remove &lt;block&gt;, /aurora blocks list, /aurora blocks clear
 *   /aurora friend add|remove &lt;name&gt;, /aurora friend list
 */
public final class Commands {
    private Commands() {
    }

    private static final SuggestionProvider<FabricClientCommandSource> BLOCK_IDS =
            (ctx, builder) -> CommandSource.suggestIdentifiers(Registries.BLOCK.getIds(), builder);

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("aurora")
                .then(literal("blocks")
                        .then(literal("add")
                                .then(argument("block", IdentifierArgumentType.identifier())
                                        .suggests(BLOCK_IDS)
                                        .executes(c -> {
                                            Identifier id = c.getArgument("block", Identifier.class);
                                            boolean ok = AuroraClient.modules.get(BlockEsp.class).addBlock(id);
                                            say(c.getSource(), ok ? "Block ESP: added " + id
                                                    : "Block ESP: " + id + " is not a block");
                                            return 1;
                                        })))
                        .then(literal("remove")
                                .then(argument("block", IdentifierArgumentType.identifier())
                                        .suggests(BLOCK_IDS)
                                        .executes(c -> {
                                            Identifier id = c.getArgument("block", Identifier.class);
                                            boolean ok = AuroraClient.modules.get(BlockEsp.class).removeBlock(id);
                                            say(c.getSource(), ok ? "Block ESP: removed " + id
                                                    : "Block ESP: " + id + " was not in the list");
                                            return 1;
                                        })))
                        .then(literal("list")
                                .executes(c -> {
                                    var list = AuroraClient.modules.get(BlockEsp.class).customBlocks();
                                    say(c.getSource(), list.isEmpty()
                                            ? "Block ESP: custom list is empty. Add blocks with /aurora blocks add <block>"
                                            : "Block ESP custom blocks: " + String.join(", ", list));
                                    return 1;
                                }))
                        .then(literal("clear")
                                .executes(c -> {
                                    AuroraClient.modules.get(BlockEsp.class).clearBlocks();
                                    say(c.getSource(), "Block ESP: custom list cleared");
                                    return 1;
                                })))
                .then(literal("config")
                        .then(literal("save")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(c -> {
                                            String n = Config.cleanName(StringArgumentType.getString(c, "name"));
                                            if (n == null) {
                                                say(c.getSource(), "Config: use letters, numbers, - or _");
                                                return 1;
                                            }
                                            Config.saveNamed(AuroraClient.modules, n);
                                            say(c.getSource(), "Config saved: " + n);
                                            return 1;
                                        })))
                        .then(literal("load")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(c -> {
                                            String n = Config.cleanName(StringArgumentType.getString(c, "name"));
                                            boolean ok = n != null && Config.loadNamed(AuroraClient.modules, n);
                                            say(c.getSource(), ok ? "Config loaded: " + n : "Config not found");
                                            return 1;
                                        })))
                        .then(literal("delete")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(c -> {
                                            String n = Config.cleanName(StringArgumentType.getString(c, "name"));
                                            boolean ok = n != null && Config.delete(n);
                                            say(c.getSource(), ok ? "Config deleted: " + n : "Config not found");
                                            return 1;
                                        })))
                        .then(literal("list")
                                .executes(c -> {
                                    var names = Config.names();
                                    say(c.getSource(), names.isEmpty() ? "No configs saved yet"
                                            : "Configs: " + String.join(", ", names));
                                    return 1;
                                })))
                .then(literal("where")
                        .executes(c -> {
                            var p = net.minecraft.client.MinecraftClient.getInstance().player;
                            if (p == null) {
                                say(c.getSource(), "Not in a world");
                                return 1;
                            }
                            var cp = p.getChunkPos();
                            say(c.getSource(), "I'm in chunk " + cp.x + ", " + cp.z + " (" + Math.floorMod((int) Math.floor(p.getX()), 16) + ", " + Math.floorMod((int) Math.floor(p.getZ()), 16) + " inside it) (block "
                                    + (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " "
                                    + (int) Math.floor(p.getZ()) + ")");
                            return 1;
                        }))
                .then(literal("friend")
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(c -> {
                                            String n = StringArgumentType.getString(c, "name");
                                            boolean ok = AuroraClient.modules.get(Friends.class).add(n);
                                            say(c.getSource(), ok ? "Friends: added " + n : n + " is already a friend");
                                            return 1;
                                        })))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(c -> {
                                            String n = StringArgumentType.getString(c, "name");
                                            boolean ok = AuroraClient.modules.get(Friends.class).remove(n);
                                            say(c.getSource(), ok ? "Friends: removed " + n : n + " was not a friend");
                                            return 1;
                                        })))
                        .then(literal("list")
                                .executes(c -> {
                                    var list = AuroraClient.modules.get(Friends.class).list();
                                    say(c.getSource(), list.isEmpty() ? "Friends: none yet"
                                            : "Friends: " + String.join(", ", list));
                                    return 1;
                                }))));
    }

    private static void say(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("[67 client] " + message));
    }
}
