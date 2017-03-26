package net.skin43d.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import net.minecraft.entity.Entity;
import net.skin43d.SkinProvider;
import net.skin43d.skin3d.SkinType;
import riskyken.armourersWorkshop.api.common.skin.data.ISkinDye;
import riskyken.armourersWorkshop.client.render.bake.SkinBakery;
import riskyken.armourersWorkshop.common.skin.data.Skin;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * @author ci010
 */
public abstract class SkinProviderBase implements SkinProvider {
    private Cache<String, Skin> skinCache;
    private SkinBakery bakery;
    private ListeningExecutorService service;

    public SkinProviderBase(SkinBakery bakery, ListeningExecutorService service) {
        this.bakery = bakery;
        this.service = service;
        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .concurrencyLevel(1)
                .build();
    }

    protected void requestSkin(final Entity entity, final SkinType skinType) {
        final ListenableFuture<Skin> future = service.submit(requestSkinTask(entity, skinType));
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Skin skin = future.get();
                    bakery.bake(skin);
                    skinCache.put(createKey(entity, skinType), skin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }, service);
    }

    protected abstract Callable<Skin> requestSkinTask(Entity entity, SkinType skinType);

    @Override
    public Skin getSkinInfoForEntity(Entity entity, SkinType skinType) {
        String key = createKey(entity, skinType);
        Skin skin = skinCache.getIfPresent(key);
        if (skin == null)
            requestSkin(entity, skinType);
        return skin;
    }

    @Override
    public ISkinDye getPlayerDyeData(Entity entity, SkinType skinType) {
        return null;
    }

    protected String createKey(Entity entity, SkinType skinType) {
        return entity.getUniqueID() + skinType.getRegistryName();
    }
}