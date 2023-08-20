package de.dasbabypixel.testplugin;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.InvocationOrder;
import eu.cloudnetservice.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.defaults.DefaultFileChunkedPacketHandler;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestPlugin extends JavaPlugin {

    private final ConcurrentMap<UUID, CompletableFuture<ZipInputStream>> zipFutures = new ConcurrentHashMap<>();
    private final BiConsumer<? super ZipInputStream, ? super Throwable> completeAction = (in, t) -> {
        System.out.println("received");
        if (t != null) t.printStackTrace();
        try {
            zipReceived(in);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    @Override
    public void onEnable() {
        InjectionLayer<Injector> i = InjectionLayer.boot();
//        i.instance(EventManager.class).registerListener(this);
        ComponentInfo componentInfo = i.instance(ComponentInfo.class);
        TemplateStorageProvider t = i.instance(TemplateStorageProvider.class);
        getCommand("test").setExecutor((sender, command, label, args) -> {
            t.localTemplateStorage().openZipInputStreamAsync(ServiceTemplate.parse("proxy/default")).whenComplete(completeAction);
//            zipTemplateAsync(componentInfo, ServiceTemplate.parse("proxy/default")).whenComplete(completeAction);
            return true;
        });
    }

    private Task<ZipInputStream> zipTemplateAsync(ComponentInfo componentInfo, ServiceTemplate template) {
        return Task.wrapFuture(Task.supply(() -> {
            var responseId = UUID.randomUUID();
            var future = new CompletableFuture<ZipInputStream>();
            zipFutures.put(responseId, future);
            var response = ChannelMessage.builder().message("remote_templates_zip_template").channel("cloudnet:internal").targetNode(componentInfo.nodeUniqueId()).buffer(DataBuf.empty().writeString(template.storageName()).writeObject(template).writeUniqueId(responseId)).build().sendSingleQuery();
            boolean suc = response.content().readBoolean();
            if (!suc) return null;

            return future;
            // 5 Second timeout after the node finished sending the last packet. Should be enough for the wrapper to receive all the packets, write the file and notify us
        }).thenCompose(fut -> fut.orTimeout(5, TimeUnit.SECONDS)));
    }

    private void zipReceived(ZipInputStream in) throws IOException {
        if (in == null) {
            System.out.println("null in");
            return;
        }
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            System.out.println(entry.getName());
            in.closeEntry();
        }
        System.out.println("end of entries");
    }

    private boolean shouldNotHandle(ChunkedPacketSessionOpenEvent event) {
        if (!event.session().transferChannel().equals("request_template_file_result")) return true;
        // In production the next check is rather expensive (without loadfactor for the map),
        // which is why doing the first check for the channel makes sense. Could also leave out the first check
        return !zipFutures.containsKey(event.session().sessionUniqueId());
    }

//    @EventListener(order = InvocationOrder.EARLY)
//    public void handleEarly(ChunkedPacketSessionOpenEvent event) {
//        if (shouldNotHandle(event)) return;
//        // Got to do this so the TemplateStorageCallbackListener doesn't fail, which in turn stops #handleLate
//        FileUtil.createDirectory(FileUtil.TEMP_DIR);
//    }
//
//    @EventListener(order = InvocationOrder.LATE)
//    public void handleLate(ChunkedPacketSessionOpenEvent event) {
//        if (shouldNotHandle(event)) return;
//
//        event.handler(new DefaultFileChunkedPacketHandler(
//                event.session(),
//                (chunkSessionInformation, inputStream) -> zipFutures.remove(chunkSessionInformation.sessionUniqueId()).complete(new ZipInputStream(inputStream)),
//                FileUtil.TEMP_DIR.resolve(event.session().sessionUniqueId().toString())));
//    }
}
