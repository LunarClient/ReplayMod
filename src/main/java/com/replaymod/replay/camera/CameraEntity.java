package com.replaymod.replay.camera;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.SettingsChangedCallback;
import com.replaymod.replay.ReplayHandler;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.utils.Utils;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replay.mixin.FirstPersonRendererAccessor;
import com.replaymod.replaystudio.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.AxisAlignedBB;

//#if FABRIC>=1
//$$ import com.replaymod.core.events.KeyBindingEventCallback;
//$$ import com.replaymod.core.events.PreRenderCallback;
//$$ import com.replaymod.core.events.PreRenderHandCallback;
//$$ import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
//$$ import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
//#else
import com.replaymod.core.versions.MCVer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

//#if MC>=11400
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.fluid.Fluid;
//$$ import net.minecraft.tags.Tag;
//$$ import net.minecraft.util.math.BlockRayTraceResult;
//$$ import net.minecraft.util.math.RayTraceResult;
//#else
import com.replaymod.replay.events.ReplayChatMessageEvent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.InputEvent;

//#if MC>=11400
//$$ import net.minecraft.util.math.RayTraceFluidMode;
//#else
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
//#endif
//#endif

//#if MC>=10904
//$$ import net.minecraft.inventory.EntityEquipmentSlot;
//#if MC>=11200
//#if MC>=11400
//$$ import net.minecraft.client.util.ClientRecipeBook;
//#else
//$$ import net.minecraft.stats.RecipeBook;
//#endif
//#endif
//$$ import net.minecraft.util.EnumHand;
//#endif

//#if MC>=10800
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EnumPlayerModelParts;
//#else
//$$ import net.minecraft.client.entity.EntityClientPlayerMP;
//$$ import net.minecraft.util.Session;
//#endif

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.replaymod.core.versions.MCVer.*;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay the player should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
@SuppressWarnings("EntityConstructor")
public class CameraEntity
        //#if MC>=10800
        extends EntityPlayerSP
        //#else
        //$$ extends EntityClientPlayerMP
        //#endif
{
    private static final UUID CAMERA_UUID = UUID.nameUUIDFromBytes("ReplayModCamera".getBytes(StandardCharsets.UTF_8));

    /**
     * Roll of this camera in degrees.
     */
    public float roll;

    private CameraController cameraController;

    private long lastControllerUpdate = System.currentTimeMillis();

    /**
     * The entity whose hand was the last one rendered.
     */
    private Entity lastHandRendered = null;

    /**
     * The hashCode and equals methods of Entity are not stable.
     * Therefore we cannot register any event handlers directly in the CameraEntity class and
     * instead have this inner class.
     */
    private EventHandler eventHandler = new EventHandler();

    public CameraEntity(
            Minecraft mcIn,
            //#if MC>=11400
            //$$ ClientWorld worldIn,
            //#else
            World worldIn,
            //#endif
            //#if MC<10800
            //$$ Session session,
            //#endif
            NetHandlerPlayClient netHandlerPlayClient,
            StatFileWriter statisticsManager
            //#if MC>=11200
            //#if MC>=11400
            //$$ , ClientRecipeBook recipeBook
            //#else
            //$$ , RecipeBook recipeBook
            //#endif
            //#endif
    ) {
        super(mcIn,
                worldIn,
                //#if MC<10800
                //$$ session,
                //#endif
                netHandlerPlayClient,
                statisticsManager
                //#if MC>=11200
                //$$ , recipeBook
                //#endif
                //#if MC>=11600
                //$$ , false
                //$$ , false
                //#endif
        );
        //#if MC>=10900
        //$$ setUniqueId(CAMERA_UUID);
        //#else
        entityUniqueID = CAMERA_UUID;
        //#endif
        eventHandler.register();
        if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
            cameraController = ReplayModReplay.instance.createCameraController(this);
        } else {
            cameraController = new SpectatorCameraController(this);
        }
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    /**
     * Moves the camera by the specified delta.
     * @param x Delta in X direction
     * @param y Delta in Y direction
     * @param z Delta in Z direction
     */
    public void moveCamera(double x, double y, double z) {
        setCameraPosition(Entity_getX(this) + x, Entity_getY(this) + y, Entity_getZ(this) + z);
    }

    /**
     * Set the camera position.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setCameraPosition(double x, double y, double z) {
        this.lastTickPosX = this.prevPosX = x;
        this.lastTickPosY = this.prevPosY = y;
        this.lastTickPosZ = this.prevPosZ = z;
        Entity_setPos(this, x, y, z);
        updateBoundingBox();
    }

    /**
     * Sets the camera rotation.
     * @param yaw Yaw in degrees
     * @param pitch Pitch in degrees
     * @param roll Roll in degrees
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.roll = roll;
    }

    /**
     * Sets the camera position and rotation to that of the specified AdvancedPosition
     * @param pos The position and rotation to set
     */
    public void setCameraPosRot(Location pos) {
        setCameraRotation(pos.getYaw(), pos.getPitch(), roll);
        setCameraPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets the camera position and rotation to that of the specified entity.
     * @param to The entity whose position to copy
     */
    public void setCameraPosRot(Entity to) {
        if (to == this) return;
        //#if MC>=10800
        float yOffset = 0;
        //#else
        //$$ float yOffset = 1.62f; // Magic value (eye height) from EntityRenderer#orientCamera
        //#endif
        this.prevPosX = to.prevPosX;
        this.prevPosY = to.prevPosY + yOffset;
        this.prevPosZ = to.prevPosZ;
        this.prevRotationYaw = to.prevRotationYaw;
        this.prevRotationPitch = to.prevRotationPitch;
        Entity_setPos(this, Entity_getX(to), Entity_getY(to), Entity_getZ(to));
        this.rotationYaw = to.rotationYaw;
        this.rotationPitch = to.rotationPitch;
        this.lastTickPosX = to.lastTickPosX;
        this.lastTickPosY = to.lastTickPosY + yOffset;
        this.lastTickPosZ = to.lastTickPosZ;
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        //#if MC>=11400
        //$$ float width = getWidth();
        //$$ float height = getHeight();
        //#endif
        //#if MC>=10800
        setEntityBoundingBox(new AxisAlignedBB(
        //#else
        //$$ this.boundingBox.setBB(AxisAlignedBB.getBoundingBox(
        //#endif
                Entity_getX(this) - width / 2, Entity_getY(this), Entity_getZ(this) - width / 2,
                Entity_getX(this) + width / 2, Entity_getY(this) + height, Entity_getZ(this) + width / 2));
    }

    @Override
    public void onUpdate() {
        //#if MC>=10800
        Entity view = getRenderViewEntity(this.mc);
        //#else
        //$$ EntityLivingBase view = getRenderViewEntity(this.mc);
        //#endif
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            if (spectating != null && (view.getUniqueID() != spectating
                    || view.worldObj != this.worldObj)
                    || this.worldObj.getEntityByID(view.getEntityId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = this.worldObj.getPlayerEntityByUUID(spectating);
                if (view != null) {
                    setRenderViewEntity(this.mc, view);
                } else {
                    setRenderViewEntity(this.mc, this);
                    return;
                }
            }
            // Move cmera to their position so when we exit the first person view
            // we don't jump back to where we entered it
            if (view != this) {
                setCameraPosRot(view);
            }
        }
    }

    @Override
    public void preparePlayerToSpawn() {
        // Make sure our world is up-to-date in case of world changes
        if (this.mc.theWorld != null) {
            this.worldObj = this.mc.theWorld;
        }
        super.preparePlayerToSpawn();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        if (getRenderViewEntity(this.mc) == this) {
            // Only update camera rotation when the camera is the view
            super.setRotation(yaw, pitch);
        }
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return falseUnlessSpectating(Entity::isEntityInsideOpaqueBlock); // Make sure no suffocation overlay is rendered
    }

    //#if MC<11400
    @Override
    public boolean isInsideOfMaterial(Material materialIn) {
        return falseUnlessSpectating(e -> e.isInsideOfMaterial(materialIn)); // Make sure no overlays are rendered
    }
    //#endif

    //#if MC>=11400
    //$$ @Override
    //$$ public boolean areEyesInFluid(Tag<Fluid> fluid) {
    //$$     return falseUnlessSpectating(entity -> entity.areEyesInFluid(fluid));
    //$$ }
    //#else
    //#if MC>=10800
    @Override
    public boolean isInLava() {
        return falseUnlessSpectating(Entity::isInLava); // Make sure no lava overlay is rendered
    }
    //#else
    //$$ @Override
    //$$ public boolean handleLavaMovement() {
    //$$     return falseUnlessSpectating(Entity::handleLavaMovement); // Make sure no lava overlay is rendered
    //$$ }
    //#endif

    @Override
    public boolean isInWater() {
        return falseUnlessSpectating(Entity::isInWater); // Make sure no water overlay is rendered
    }
    //#endif

    @Override
    public boolean isBurning() {
        return falseUnlessSpectating(Entity::isBurning); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = getRenderViewEntity(this.mc);
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false; // We are in full control of ourselves
    }

    //#if MC>=10800
    @Override
    protected void createRunningParticles() {
        // We do not produce any particles, we are a camera
    }
    //#endif

    @Override
    public boolean canBeCollidedWith() {
        return false; // We are a camera, we cannot collide
    }

    //#if MC>=10800
    @Override
    public boolean isSpectator() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        return replayHandler == null || replayHandler.isCameraView(); // Make sure we're treated as spectator
    }
    //#endif

    //#if MC>=11400
    //$$ @Override
    //$$ public boolean isInRangeToRender3d(double double_1, double double_2, double double_3) {
    //$$     return false; // never render the camera otherwise it'd be visible e.g. in 3rd-person or with shaders
    //$$ }
    //#else
    @Override
    public boolean shouldRenderInPass(int pass) {
        // Never render the camera
        // This is necessary to hide the player head in third person mode and to not
        // cause any unwanted shadows when rendering with shaders.
        return false;
    }
    //#endif

    //#if MC>=10800
    @Override
    public float getFovModifier() {
        Entity view = getRenderViewEntity(this.mc);
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getFovModifier();
        }
        return 1;
    }
    //#else
    //$$ @Override
    //$$ public float getFOVMultiplier() {
    //$$     return 1;
    //$$ }
    //#endif

    @Override
    public boolean isInvisible() {
        Entity view = getRenderViewEntity(this.mc);
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public ResourceLocation getLocationSkin() {
        Entity view = getRenderViewEntity(this.mc);
        if (view != this && view instanceof EntityPlayer) {
            return Utils.getResourceLocationForPlayerUUID(view.getUniqueID());
        }
        return super.getLocationSkin();
    }

    //#if MC>=10800
    @Override
    public String getSkinType() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getSkinType();
        }
        return super.getSkinType();
    }

    @Override
    public boolean isWearing(EnumPlayerModelParts modelPart) {
        Entity view = getRenderViewEntity(this.mc);
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).isWearing(modelPart);
        }
        return super.isWearing(modelPart);
    }
    //#endif

    @Override
    public float getSwingProgress(float renderPartialTicks) {
        Entity view = getRenderViewEntity(this.mc);
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getSwingProgress(renderPartialTicks);
        }
        return 0;
    }

    //#if MC>=10904
    //$$ @Override
    //$$ public float getCooldownPeriod() {
    //$$     Entity view = this.mc.getRenderViewEntity();
    //$$     if (view != this && view instanceof EntityPlayer) {
    //$$         return ((EntityPlayer) view).getCooldownPeriod();
    //$$     }
    //$$     return 1;
    //$$ }
    //$$
    //$$ @Override
    //$$ public float getCooledAttackStrength(float adjustTicks) {
    //$$     Entity view = this.mc.getRenderViewEntity();
    //$$     if (view != this && view instanceof EntityPlayer) {
    //$$         return ((EntityPlayer) view).getCooledAttackStrength(adjustTicks);
    //$$     }
    //$$     // Default to 1 as to not render the cooldown indicator (renders for < 1)
    //$$     return 1;
    //$$ }
    //$$
    //$$ @Override
    //$$ public EnumHand getActiveHand() {
    //$$     Entity view = this.mc.getRenderViewEntity();
    //$$     if (view != this && view instanceof EntityPlayer) {
    //$$         return ((EntityPlayer) view).getActiveHand();
    //$$     }
    //$$     return super.getActiveHand();
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean isHandActive() {
    //$$     Entity view = this.mc.getRenderViewEntity();
    //$$     if (view != this && view instanceof EntityPlayer) {
    //$$         return ((EntityPlayer) view).isHandActive();
    //$$     }
    //$$     return super.isHandActive();
    //$$ }
    //$$
    //#if MC>=11400
    //$$ @Override
    //$$ protected void playEquipSound(ItemStack itemStack_1) {
    //$$     // Suppress equip sounds
    //$$ }
    //#endif
    //$$
    //#if MC>=11400
    //$$ @Override
    //$$ public RayTraceResult func_213324_a(double maxDistance, float tickDelta, boolean fluids) {
    //$$     RayTraceResult result = super.func_213324_a(maxDistance, tickDelta, fluids);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if (result instanceof BlockRayTraceResult) {
    //$$         BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
    //$$         result = BlockRayTraceResult.createMiss(result.getHitVec(), blockResult.getFace(), blockResult.getPos());
    //$$     }
    //$$
    //$$     return result;
    //$$ }
    //#else
    //#if MC>=11400
    //$$ @Override
    //$$ public RayTraceResult rayTrace(double blockReachDistance, float partialTicks, RayTraceFluidMode p_174822_4_) {
    //$$     RayTraceResult pos = super.rayTrace(blockReachDistance, partialTicks, p_174822_4_);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.type == RayTraceResult.Type.BLOCK) {
    //$$         pos.type = RayTraceResult.Type.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#else
    //$$ @Override
    //$$ public RayTraceResult rayTrace(double p_174822_1_, float p_174822_3_) {
    //$$     RayTraceResult pos = super.rayTrace(p_174822_1_, 1f);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
    //$$         pos.typeOfHit = RayTraceResult.Type.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#endif
    //#endif
    //#else
    @Override
    public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
        MovingObjectPosition pos = super.rayTrace(p_174822_1_, 1f);

        // Make sure we can never look at blocks (-> no outline)
        if(pos != null && pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            pos.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
        }

        return pos;
    }
    //#endif

    //#if MC<11400
    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
        // Do not open any block GUIs for the camera entities
        // Note: Vanilla GUIs are filtered out on a packet level, this only applies to mod GUIs
    }
    //#endif

    @Override
    public void setDead() {
        super.setDead();
        if (eventHandler != null) {
            eventHandler.unregister();
            eventHandler = null;
        }
    }

    private void update() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != this.worldObj) {
            if (eventHandler != null) {
                eventHandler.unregister();
                eventHandler = null;
            }
            return;
        }

        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        handleInputEvents();

        Map<String, KeyBindingRegistry.Binding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getBindings();
        if (keyBindings.get("replaymod.input.rollclockwise").keyBinding.isKeyDown()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("replaymod.input.rollcounterclockwise").keyBinding.isKeyDown()) {
            roll -= Utils.isCtrlDown() ? 0.2 : 1;
        }

        //#if MC>=10800
        this.noClip = this.isSpectator();
        //#endif
    }

    private void handleInputEvents() {
        if (this.mc.gameSettings.keyBindAttack.isPressed() || this.mc.gameSettings.keyBindUseItem.isPressed()) {
            if (this.mc.currentScreen == null && canSpectate(this.mc.pointedEntity)) {
                ReplayModReplay.instance.getReplayHandler().spectateEntity(
                        //#if MC<=10710
                        //$$ (EntityLivingBase)
                        //#endif
                        this.mc.pointedEntity);
                // Make sure we don't exit right away
                //noinspection StatementWithEmptyBody
                while (this.mc.gameSettings.keyBindSneak.isPressed());
            }
        }
    }

    private void updateArmYawAndPitch() {
        this.prevRenderArmYaw = this.renderArmYaw;
        this.prevRenderArmPitch = this.renderArmPitch;
        this.renderArmPitch = this.renderArmPitch +  (this.rotationPitch - this.renderArmPitch) * 0.5f;
        this.renderArmYaw = this.renderArmYaw +  (this.rotationYaw - this.renderArmYaw) * 0.5f;
    }

    public boolean canSpectate(Entity e) {
        return e != null
                //#if MC<10800
                //$$ && e instanceof EntityPlayer // cannot be more generic since 1.7.10 has no concept of eye height
                //#endif
                && !e.isInvisible();
    }

    //#if MC<11400
    //#if MC>=11102
    //$$ @Override
    //$$ public void sendMessage(ITextComponent message) {
    //$$     if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
    //$$     super.sendMessage(message);
    //$$ }
    //#else
    @Override
    public void addChatMessage(IChatComponent message) {
        if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
        super.addChatMessage(message);
    }
    //#endif
    //#endif

    //#if MC>=10800
    private
    //#else
    //$$ public // All event handlers need to be public in 1.7.10
    //#endif
    class EventHandler extends EventRegistrations {
        private final Minecraft mc = getMinecraft();

        private EventHandler() {}

        //#if FABRIC>=1
        //$$ { on(PreTickCallback.EVENT, this::onPreClientTick); }
        //$$ private void onPreClientTick() {
        //#else
        @SubscribeEvent
        public void onPreClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
        //#endif
            updateArmYawAndPitch();
        }

        //#if FABRIC>=1
        //$$ { on(PreRenderCallback.EVENT, this::onRenderUpdate); }
        //$$ private void onRenderUpdate() {
        //#else
        @SubscribeEvent
        public void onRenderUpdate(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
        //#endif
            update();
        }

        //#if FABRIC>=1
        //$$ { on(KeyBindingEventCallback.EVENT, CameraEntity.this::handleInputEvents); }
        //#elseif MC<11400
        @SubscribeEvent
        public void onKeyEvent(InputEvent.KeyInputEvent event) {
            handleInputEvents();
        }

        @SubscribeEvent
        public void onMouseInput(InputEvent.MouseInputEvent event) {
            handleInputEvents();
        }
        //#endif

        //#if FABRIC>=1
        //$$ { on(RenderSpectatorCrosshairCallback.EVENT, this::shouldRenderSpectatorCrosshair); }
        //$$ private Boolean shouldRenderSpectatorCrosshair() {
        //$$     return canSpectate(mc.targetedEntity);
        //$$ }
        //#else
        @SubscribeEvent
        public void preCrosshairRender(RenderGameOverlayEvent.Pre event) {
            // The crosshair should only render if targeted entity can actually be spectated
            if (MCVer.getType(event) == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                event.setCanceled(!canSpectate(mc.pointedEntity));
            }
            // Hotbar should never be rendered
            if (MCVer.getType(event) == RenderGameOverlayEvent.ElementType.HOTBAR) {
                event.setCanceled(true);
            }
        }
        //#endif

        { on(SettingsChangedCallback.EVENT, this::onSettingsChanged); }
        private void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key) {
            if (key == Setting.CAMERA) {
                if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
                    cameraController = ReplayModReplay.instance.createCameraController(CameraEntity.this);
                } else {
                    cameraController = new SpectatorCameraController(CameraEntity.this);
                }
            }
        }

        //#if FABRIC>=1
        //$$ { on(PreRenderHandCallback.EVENT, this::onRenderHand); }
        //$$ private boolean onRenderHand() {
        //$$     // Unless we are spectating another player, don't render our hand
        //$$     if (getRenderViewEntity(mc) == CameraEntity.this || !(getRenderViewEntity(mc) instanceof PlayerEntity)) {
        //$$         return true; // cancel hand rendering
        //$$     } else {
        //$$         onRenderHandMonitor();
        //$$         return false;
        //$$     }
        //$$ }
        //#else
        @SubscribeEvent
        public void onRenderHand(RenderHandEvent event) {
            // Unless we are spectating another player, don't render our hand
            if (getRenderViewEntity(mc) == CameraEntity.this || !(getRenderViewEntity(mc) instanceof EntityPlayer)) {
                event.setCanceled(true);
            }
        }
        //#endif

        //#if FABRIC>=1
        //$$ private void onRenderHandMonitor() {
        //#else
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderHandMonitor(RenderHandEvent event) {
        //#endif
            Entity view = getRenderViewEntity(mc);
            if (view instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    FirstPersonRendererAccessor acc = (FirstPersonRendererAccessor) mc.entityRenderer.itemRenderer;
                    //#if MC>=10904
                    //$$ acc.setPrevEquippedProgressMainHand(1);
                    //$$ acc.setPrevEquippedProgressOffHand(1);
                    //$$ acc.setEquippedProgressMainHand(1);
                    //$$ acc.setEquippedProgressOffHand(1);
                    //$$ acc.setItemStackMainHand(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND));
                    //$$ acc.setItemStackOffHand(player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND));
                    //#else
                    acc.setPrevEquippedProgress(1);
                    acc.setEquippedProgress(1);
                    acc.setItemToRender(player.inventory.getCurrentItem());
                    acc.setEquippedItemSlot(player.inventory.currentItem);
                    //#endif


                    mc.thePlayer.renderArmYaw = mc.thePlayer.prevRenderArmYaw = player.rotationYaw;
                    mc.thePlayer.renderArmPitch = mc.thePlayer.prevRenderArmPitch = player.rotationPitch;
                }
            }
        }

        //#if MC>=11400
        //$$ // Moved to MixinCamera
        //#else
        //#if MC>=10800
        @SubscribeEvent
        public void onEntityViewRenderEvent(EntityViewRenderEvent.CameraSetup event) {
            if (mc.getRenderViewEntity() == CameraEntity.this) {
                //#if MC>=10904
                //$$ event.setRoll(roll);
                //#else
                event.roll = roll;
                //#endif
            }
        }
        //#endif
        //#endif

        private boolean heldItemTooltipsWasTrue;

        //#if FABRIC>=1
        //$$ // FIXME fabric
        //#else
        @SubscribeEvent
        public void preRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
            switch (MCVer.getType(event)) {
                case ALL:
                    heldItemTooltipsWasTrue = mc.gameSettings.heldItemTooltips;
                    mc.gameSettings.heldItemTooltips = false;
                    break;
                case ARMOR:
                case HEALTH:
                case FOOD:
                case AIR:
                case HOTBAR:
                case EXPERIENCE:
                case HEALTHMOUNT:
                case JUMPBAR:
                //#if MC>=10904
                //$$ case POTION_ICONS:
                //#endif
                    event.setCanceled(true);
                    break;
                case HELMET:
                case PORTAL:
                case CROSSHAIRS:
                case BOSSHEALTH:
                //#if MC>=10904
                //$$ case BOSSINFO:
                //$$ case SUBTITLES:
                //#endif
                case TEXT:
                case CHAT:
                case PLAYER_LIST:
                case DEBUG:
                    break;
            }
        }

        @SubscribeEvent
        public void postRenderGameOverlay(RenderGameOverlayEvent.Post event) {
            if (MCVer.getType(event) != RenderGameOverlayEvent.ElementType.ALL) return;
            mc.gameSettings.heldItemTooltips = heldItemTooltipsWasTrue;
        }
        //#endif
    }
}
