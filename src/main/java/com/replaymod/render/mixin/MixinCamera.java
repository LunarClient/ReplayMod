package com.replaymod.render.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.world.IBlockReader;
//#else
import net.minecraft.client.renderer.EntityRenderer;
//#endif

//#if MC>=11400
//$$ @Mixin(value = ActiveRenderInfo.class)
//#else
@Mixin(value = EntityRenderer.class)
//#endif
public abstract class MixinCamera {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().entityRenderer).replayModRender_getHandler();
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    // Only relevant on 1.13+ (previously MC always used the non-head yaw) and only for LivingEntity view entities.
    private float orgHeadYaw;
    private float orgPrevHeadYaw;

    //#if MC>=11400
    //$$ @Inject(method = "update", at = @At("HEAD"))
    //#else
    @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    //#endif
    private void replayModRender_beforeSetupCameraTransform(
            //#if MC>=11400
            //$$ IBlockReader blockView,
            //$$ Entity entity,
            //$$ boolean thirdPerson,
            //$$ boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
            if (entity instanceof EntityLivingBase) {
                orgHeadYaw = ((EntityLivingBase) entity).rotationYawHead;
                orgPrevHeadYaw = ((EntityLivingBase) entity).prevRotationYawHead;
            }
        }
    //#if MC<11400
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
    //#endif
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            RenderSettings settings = getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                entity.prevRotationYaw = entity.rotationYaw = 0;
                if (entity instanceof EntityLivingBase) {
                    ((EntityLivingBase) entity).prevRotationYawHead = ((EntityLivingBase) entity).rotationYawHead = 0;
                }
            }
            if (settings.isStabilizePitch()) {
                entity.prevRotationPitch = entity.rotationPitch = 0;
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    //#if MC>=11400
    //$$ @Inject(method = "update", at = @At("RETURN"))
    //#else
    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    //#endif
    private void replayModRender_afterSetupCameraTransform(
            //#if MC>=11400
            //$$ IBlockReader blockView,
            //$$ Entity entity,
            //$$ boolean thirdPerson,
            //$$ boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            entity.rotationYaw = orgYaw;
            entity.rotationPitch = orgPitch;
            entity.prevRotationYaw = orgPrevYaw;
            entity.prevRotationPitch = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
            if (entity instanceof EntityLivingBase) {
                ((EntityLivingBase) entity).rotationYawHead = orgHeadYaw;
                ((EntityLivingBase) entity).prevRotationYawHead = orgPrevHeadYaw;
            }
        }
    }
}
