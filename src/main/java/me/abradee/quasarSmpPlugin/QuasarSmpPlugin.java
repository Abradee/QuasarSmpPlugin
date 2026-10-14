package me.abradee.quasarSmpPlugin;

import io.javalin.Javalin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class QuasarSmpPlugin extends JavaPlugin {

    private Javalin app;
    private Context context;

    @Override
    public void onEnable() {
        saveResource("server.js", false);
        File jsFile = new File(getDataFolder(), "server.js");
        File webFolder = new File(getDataFolder(), "web");
        if (!webFolder.exists()) {
            webFolder.mkdirs();
        }

        try {
            app = Javalin.create(config -> {
                config.staticFiles.add(webFolder.getAbsolutePath());
            }).start(7000);

            getLogger().info("Web server started on port 7000");

            context = Context.newBuilder("js")
                    .hostClassLoader(getClassLoader())
                    .allowAllAccess(true)
                    .build();

            context.getBindings("js").putMember("Bukkit", Bukkit.getServer());
            context.getBindings("js").putMember("Bridge", new PaperScriptBridge());

            String jsCode = Files.readString(jsFile.toPath());
            context.eval("js", jsCode);

            Value setupServerFunc = context.getBindings("js").getMember("setupServer");
            if (setupServerFunc != null && setupServerFunc.canExecute()) {
                setupServerFunc.execute(app);
                getLogger().info("JavaScript web routes loaded successfully!");
            } else {
                getLogger().severe("Could not find setupServer(app) function in server.js");
            }

        } catch (Exception e) {
            getLogger().severe("Failed to load JavaScript web server: " + e.getMessage());
            e.printStackTrace();

            disablePluginSafe();
        }
    }

    @Override
    public void onDisable() {
        disablePluginSafe();
    }

    private void disablePluginSafe() {
        if (app != null) {
            app.stop();
            app = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
    }

    public class PaperScriptBridge {

        public void runSync(Runnable task) {
            Bukkit.getScheduler().runTask(QuasarSmpPlugin.this, task);
        }

        public <T> CompletableFuture<T> callSync(Supplier<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(QuasarSmpPlugin.this, () -> {
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }
    }
}